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
- `configureDatabase()` — connects to PostgreSQL (env-var driven), creates the schema via Exposed
- `configureStatusPages()` — installs Ktor's `StatusPages` plugin; maps app exceptions to HTTP responses (see below)
- `configureRouting()` — instantiates `TodoRepository`, wraps it in `TodoService`, and mounts all routes

New features should follow this pattern: add an `Application.configureX()` function in its own file and register it in `application.yaml`.

**Database:** PostgreSQL accessed via Jetbrains Exposed ORM.
- Connection is configured via environment variables with local-dev fallbacks:
  - `DATABASE_URL` — default `jdbc:postgresql://localhost:5432/todos`
  - `DATABASE_USER` — default `postgres`
  - `DATABASE_PASSWORD` — default `devpassword`
- Local dev: PostgreSQL on `localhost:5432`, database `todos`, user `postgres`, password `devpassword`
- Schema is defined as `Table` objects in their respective repository files; `SchemaUtils.create(Todos, Users)` runs on startup in `configureDatabase()`
- `Todos` table: `id` (PK), `title`, `completed`
- `Users` table: `id` (PK), `username` (unique), `password_hash`
- Passwords are hashed with bcrypt via jBCrypt (`org.mindrot:jbcrypt:0.4`); `AuthService.hashPassword` / `verifyPassword` are the only call sites
- Exposed version: `0.61.0`; PostgreSQL JDBC driver: `42.7.4`; H2 `2.3.232` is kept as a dependency but not connected

**Todo feature:** The main domain feature is a CRUD Todo API backed by PostgreSQL via Exposed. The layers are:
- `models/Todo.kt` — `@Serializable` data classes: `Todo`, `CreateTodoRequest`, `UpdateTodoRequest`
- `repositories/TodoRepository.kt` — `Todos` table definition + all SQL operations wrapped in `transaction { }` blocks; IDs are random UUIDs
- `services/TodoService.kt` — business logic and validation; calls `TodoRepository`; has zero Ktor/HTTP imports. Throws `ValidationException` (→ 400) for invalid input and `NotFoundException` (→ 404) for missing records. Validation rules: title must not be blank after trimming, max 200 chars; whitespace is trimmed before saving.
- `routes/TodoRoutes.kt` — `Route` extension function `todoRoutes(service)` defining the REST endpoints below; only handles request parsing and success responses — exceptions propagate to `StatusPages`

**Error handling:** Centralized in `plugins/StatusPages.kt` via Ktor's `StatusPages` plugin. Exception-to-HTTP mapping:
- `exceptions/AppExceptions.kt` defines `ValidationException` and `NotFoundException` (both extend `Exception`)
- `ValidationException` → 400 with `{ "message": "..." }`
- `NotFoundException` → 404 with `{ "message": "..." }`
- Any other `Throwable` → 500 with `{ "message": "Internal server error" }` (detail not leaked)
- All error responses are JSON `ErrorResponse(message)` objects

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
