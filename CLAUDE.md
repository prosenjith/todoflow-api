# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build      # compile and package
./gradlew run        # start the server (listens on :8080)
./gradlew test       # run all tests
./gradlew test --tests "com.example.ServerTest.test todos endpoint returns 200"  # run a single test
```

## Architecture

This is a minimal Ktor 3.x server using the **Netty** engine, configured via `application.yaml` (not code).

**Startup flow:** `main()` delegates to `EngineMain`, which reads `src/main/resources/application.yaml` and calls the listed module functions to wire up the application.

**Module pattern:** Each concern is an extension function on `Application` registered in `application.yaml` under `ktor.application.modules`. Currently:
- `configureSerialization()` — installs `ContentNegotiation` with `kotlinx.serialization` JSON
- `configureDatabase()` — connects to H2, starts the TCP + web console servers, creates the schema
- `configureRouting()` — instantiates `TodoRepository` and mounts all routes

New features should follow this pattern: add an `Application.configureX()` function in its own file and register it in `application.yaml`.

**Database:** H2 in-memory database accessed via Jetbrains Exposed ORM.
- JDBC URL: `jdbc:h2:mem:todos;DB_CLOSE_DELAY=-1` — data resets on server restart
- H2 TCP server on port `9092`, web console on port `8082`
- To inspect data: open `http://localhost:8082`, set JDBC URL to `jdbc:h2:tcp://localhost:9092/mem:todos`, user `sa`, password blank
- Schema is defined in `TodoRepository.kt` as the `Todos` `Table` object; `SchemaUtils.create(Todos)` runs on startup in `configureDatabase()`
- Exposed version: `0.61.0`; H2 version: `2.3.232`

**Todo feature:** The main domain feature is a CRUD Todo API backed by H2 via Exposed. The layers are:
- `models/Todo.kt` — `@Serializable` data classes: `Todo`, `CreateTodoRequest`, `UpdateTodoRequest`
- `repositories/TodoRepository.kt` — `Todos` table definition + all SQL operations wrapped in `transaction { }` blocks; IDs are random UUIDs
- `routes/TodoRoutes.kt` — `Route` extension function `todoRoutes(repository)` defining the REST endpoints below

| Method | Path | Description |
|--------|------|-------------|
| GET | `/todos` | List all todos |
| GET | `/todos/{id}` | Get by id (404 if missing) |
| POST | `/todos` | Create (400 if title blank) |
| PUT | `/todos/{id}` | Partial update title/completed |
| DELETE | `/todos/{id}` | Delete (204 on success, 404 if missing) |

`TodoRepository` is instantiated once in `configureRouting()` and passed into `todoRoutes()` — there is no DI framework.

**Testing:** Uses `ktor-server-test-host` via the `testApplication { }` DSL. The `configure()` call inside a test block loads the default `application.yaml` modules, so tests run against the real module stack.

**Dependency management:** Ktor dependencies come from the `ktorLibs` version catalog (`io.ktor:ktor-version-catalog:3.5.0`), referenced as `ktorLibs.*` in `build.gradle.kts`. Non-Ktor deps use the default `libs` catalog.
