# todoflow-api

A production-ready REST API for task management, built with **Kotlin + Ktor 3** on the JVM. Demonstrates clean layered architecture, JWT-based authentication, per-user data isolation, and centralized error handling — with integration tests that hit a real database.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.x (JVM 21) |
| Framework | Ktor 3.x (Netty engine) |
| Database | PostgreSQL via Jetbrains Exposed ORM |
| Auth | JWT (HMAC256, 24h expiry) via `ktor-server-auth-jwt` |
| Password hashing | bcrypt via jBCrypt |
| Serialization | `kotlinx.serialization` |
| Testing | `ktor-server-test-host` (integration, no mocks) |
| Build | Gradle with Kotlin DSL + Ktor version catalog |

## Architecture

The app follows a strict layered design. Each concern is a Ktor `Application` extension function registered in `application.yaml` — no DI framework, no magic.

```
routes/          ← HTTP boundary: auth guard, request parsing, response shaping
services/        ← Business logic and validation; no Ktor/HTTP imports
repositories/    ← SQL via Exposed; all queries scoped to userId
models/          ← @Serializable data classes shared across layers
plugins/         ← Cross-cutting: JWT security, status pages
exceptions/      ← AppExceptions mapped to HTTP codes centrally
```

**Module startup order** (wired in `application.yaml`):
1. `configureSerialization` — JSON content negotiation
2. `configureDatabase` — PostgreSQL connection + schema bootstrap via `SchemaUtils`
3. `configureStatusPages` — maps `ValidationException` → 400, `NotFoundException` → 404, anything else → 500 (detail not leaked)
4. `configureSecurity` — JWT `auth-jwt` named provider
5. `configureRouting` — instantiates and wires repositories, services, and routes

## API

### Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/register` | — | Create account → `201 User` |
| `POST` | `/login` | — | Verify credentials → `200 AuthResponse { token, user }` |

### Todos

All todo routes require `Authorization: Bearer <token>`. Todos are fully scoped to the authenticated user — cross-user access is prevented at the SQL level (every query filters by `userId`), not just at the application level.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/todos` | List caller's todos |
| `GET` | `/todos/{id}` | Get by id (404 if missing or owned by another user) |
| `POST` | `/todos` | Create todo (400 if title blank or too long) |
| `PUT` | `/todos/{id}` | Partial update: title, completed |
| `DELETE` | `/todos/{id}` | Delete (204 on success, 404 if not owned) |

All error responses are JSON: `{ "message": "..." }`. Ownership is not revealed in error responses — a 404 is returned whether the todo doesn't exist or belongs to another user.

## Security Design

- **JWT**: HMAC256-signed with a `userId` claim and 24-hour expiry. Secret read from `JWT_SECRET` env var (`dev-only-secret-do-not-use-in-production` fallback — must be overridden in production).
- **Passwords**: bcrypt via jBCrypt. Wrong-password and not-found errors return the same response to prevent username enumeration.
- **Data isolation**: All repository queries filter by `userId`. A user cannot read, modify, or delete another user's todos even with a valid token.
- **Error leakage**: Unhandled exceptions return a generic 500 — stack traces and internal detail are never exposed to the client.

## Getting Started

### Prerequisites

- JDK 21+
- PostgreSQL running on `localhost:5432`

### Database setup

```sql
CREATE DATABASE todos;
-- User: postgres / Password: devpassword (default dev config)
```

Schema is created automatically on first startup via `SchemaUtils.createMissingTablesAndColumns`.

### Run

```bash
./gradlew run
# Server starts on http://0.0.0.0:8080
```

### Configuration

All config is in `src/main/resources/application.yaml`. Connection details are env-var driven with local-dev fallbacks:

| Env var | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/todos` | JDBC connection string |
| `DATABASE_USER` | `postgres` | DB username |
| `DATABASE_PASSWORD` | `devpassword` | DB password |
| `JWT_SECRET` | `dev-only-secret-do-not-use-in-production` | JWT signing secret |

### Build & Test

```bash
./gradlew build
./gradlew test
./gradlew test --tests "com.prosenjith.todoflow.ServerTest.todos are scoped to authenticated user"
```

## Testing Approach

Tests use Ktor's `testApplication { }` DSL with `configure()` to load the real module stack — the same `application.yaml` modules that run in production. There are no mocks; tests exercise the full request-to-database path.

Coverage includes:

- Auth: registration, duplicate username, login success/failure, unknown user
- JWT guard: missing token → 401, invalid token → 401, valid token → 200
- CRUD: create, read, update, delete with expected status codes
- Input validation: blank title → 400, whitespace trimming
- **User isolation**: User B's token cannot list, fetch, update, or delete User A's todos

## Project Structure

```
src/
├── main/
│   ├── kotlin/
│   │   ├── main.kt                        # EngineMain entry point
│   │   ├── Database.kt                    # configureDatabase()
│   │   ├── Routing.kt                     # configureRouting()
│   │   ├── Serialization.kt               # configureSerialization()
│   │   ├── exceptions/AppExceptions.kt    # ValidationException, NotFoundException
│   │   ├── models/                        # Todo.kt, User.kt
│   │   ├── plugins/
│   │   │   ├── Security.kt                # configureSecurity() — JWT provider
│   │   │   └── StatusPages.kt             # configureStatusPages() — error mapping
│   │   ├── repositories/                  # TodoRepository.kt, UserRepository.kt
│   │   ├── routes/                        # TodoRoutes.kt, AuthRoutes.kt
│   │   └── services/                      # TodoService.kt, AuthService.kt
│   └── resources/
│       ├── application.yaml               # Server config + module registration
│       └── logback.xml
└── test/
    └── kotlin/ServerTest.kt               # Integration tests
```