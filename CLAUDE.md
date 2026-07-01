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
- `configureSecurity()` — installs Ktor's `Authentication` plugin with a `jwt("auth-jwt")` block; verifies token signature and expiry, validates `userId` claim presence
- `configureRouting()` — instantiates repositories, services, and mounts all routes

New features should follow this pattern: add an `Application.configureX()` function in its own file and register it in `application.yaml`.

**Database:** PostgreSQL accessed via Jetbrains Exposed ORM.
- Connection is configured via environment variables with local-dev fallbacks:
  - `DATABASE_URL` — default `jdbc:postgresql://localhost:5432/todos`
  - `DATABASE_USER` — default `postgres`
  - `DATABASE_PASSWORD` — default `devpassword`
- Local dev: PostgreSQL on `localhost:5432`, database `todos`, user `postgres`, password `devpassword`
- Schema is defined as `Table` objects in their respective repository files; `SchemaUtils.createMissingTablesAndColumns(Todos, Users)` runs on startup in `configureDatabase()` — creates new tables and adds missing columns on existing tables. Note: Exposed 0.61 marks this deprecated; for production use Flyway or Liquibase instead.
- `Todos` table: `id` (PK), `title`, `completed`, `user_id` (FK to users)
- `Users` table: `id` (PK), `username` (unique), `password_hash`
- Passwords are hashed with bcrypt via jBCrypt (`org.mindrot:jbcrypt:0.4`); `AuthService.hashPassword` / `verifyPassword` are the only call sites
- Exposed version: `0.61.0`; PostgreSQL JDBC driver: `42.7.4`; H2 `2.3.232` is kept as a dependency but not connected

**Todo feature:** The main domain feature is a CRUD Todo API backed by PostgreSQL via Exposed. The layers are:
- `models/Todo.kt` — `@Serializable` data classes: `Todo`, `CreateTodoRequest`, `UpdateTodoRequest`
- `repositories/TodoRepository.kt` — `Todos` table definition + all SQL operations wrapped in `transaction { }` blocks; IDs are random UUIDs; every query filters by `userId` so cross-user access is impossible at the SQL level
- `services/TodoService.kt` — business logic and validation; threads `userId` through all repository calls; throws `NotFoundException("Todo not found")` for both missing and wrong-owner todos (ownership not revealed to caller); no Ktor/HTTP imports
- `routes/TodoRoutes.kt` — all routes wrapped in `authenticate("auth-jwt") { }`; `userId` extracted from `call.principal<JWTPrincipal>()` — never from request body or params

**Error handling:** Centralized in `plugins/StatusPages.kt` via Ktor's `StatusPages` plugin. Exception-to-HTTP mapping:
- `exceptions/AppExceptions.kt` defines `ValidationException` and `NotFoundException` (both extend `Exception`)
- `ValidationException` → 400 with `{ "message": "..." }`
- `NotFoundException` → 404 with `{ "message": "..." }`
- Any other `Throwable` → 500 with `{ "message": "Internal server error" }` (detail not leaked)
- All error responses are JSON `ErrorResponse(message)` objects

All `/todos` routes require `Authorization: Bearer <token>`. Todos are scoped to the authenticated user — a user can only see and modify their own todos.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/todos` | List caller's todos |
| GET | `/todos/{id}` | Get by id (404 if missing or owned by another user) |
| POST | `/todos` | Create todo for caller (400 if title blank/too long) |
| PUT | `/todos/{id}` | Partial update title/completed (404 if not owned) |
| DELETE | `/todos/{id}` | Delete (204 on success, 404 if not owned) |

**Auth feature:** Registration and login backed by the `Users` table. Login issues a JWT; no routes are protected yet.
- `models/User.kt` — `User`, `RegisterRequest`, `LoginRequest`, `AuthResponse(token, user)`
- `repositories/UserRepository.kt` — `Users` table + `create`, `findByUsername` (returns `Pair<User, String>` to expose hash only within the service layer), `findById`
- `services/AuthService.kt` — `register` (uniqueness check, bcrypt hash, persist), `login` (credential lookup + hash verify; same error for not-found and wrong-password to prevent enumeration; returns `AuthResponse`), `generateToken` (HMAC256-signed JWT, 24h expiry, `userId` claim), `hashPassword`, `verifyPassword`
- `routes/AuthRoutes.kt` — `POST /register` (201 + `User`), `POST /login` (200 + `AuthResponse { token, user }`)
- `plugins/Security.kt` — installs `Authentication` with `jwt("auth-jwt")`; validates signature, expiry, and `userId` claim; no routes protected yet — wrap route blocks with `authenticate("auth-jwt") { }` when ready

**JWT:** Tokens are HMAC256-signed with a `userId` claim and 24-hour expiry. Secret read from `JWT_SECRET` env var; fallback `"dev-only-secret-do-not-use-in-production"` is clearly marked — **must be overridden in production**. Dependency: `ktor-server-auth-jwt` (BOM-managed).

All instances are wired in `configureRouting()` — there is no DI framework.

**Testing:** Uses `ktor-server-test-host` via the `testApplication { }` DSL. The `configure()` call inside a test block loads the default `application.yaml` modules, so tests run against the real module stack.

**Dependency management:** Ktor dependencies come from the `ktorLibs` version catalog (`io.ktor:ktor-version-catalog:3.5.0`), referenced as `ktorLibs.*` in `build.gradle.kts`. Non-Ktor deps use the default `libs` catalog.
