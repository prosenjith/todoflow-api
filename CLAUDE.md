# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build      # compile and package
./gradlew run        # start the server (listens on :8080)
./gradlew test       # run all tests
./gradlew test --tests "com.example.ServerTest.test root endpoint"  # run a single test
```

## Architecture

This is a minimal Ktor 3.x server using the **Netty** engine, configured via `application.yaml` (not code).

**Startup flow:** `main()` delegates to `EngineMain`, which reads `src/main/resources/application.yaml` and calls the listed module functions to wire up the application.

**Module pattern:** Each concern is an extension function on `Application` registered in `application.yaml` under `ktor.application.modules`. Currently:
- `configureSerialization()` — installs `ContentNegotiation` with `kotlinx.serialization` JSON
- `configureRouting()` — instantiates `TodoRepository` and mounts all routes

New features should follow this pattern: add an `Application.configureX()` function in its own file and register it in `application.yaml`.

**Todo feature:** The main domain feature is a CRUD Todo API backed by an in-memory `ConcurrentHashMap` (no database). The layers are:
- `models/Todo.kt` — `@Serializable` data classes: `Todo`, `CreateTodoRequest`, `UpdateTodoRequest`
- `repositories/TodoRepository.kt` — in-memory store; all operations are thread-safe via `ConcurrentHashMap`; IDs are random UUIDs
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
