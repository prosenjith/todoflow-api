package com.example


import com.example.models.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.util.UUID
import kotlin.test.*

class ServerTest {

    private fun uniqueUsername() = "user_${UUID.randomUUID().toString().take(8)}"

    private suspend fun ApplicationTestBuilder.registerAndGetToken(
        username: String,
        password: String = "password123"
    ): String {
        val c = createClient { install(ContentNegotiation) { json() } }
        c.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, password))
        }
        return c.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body<AuthResponse>().token
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test
    fun `POST register creates user and returns 201`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val username = uniqueUsername()
        val response = c.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, "password123"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val user = response.body<User>()
        assertEquals(username, user.username)
        assertNotNull(user.id)
    }

    @Test
    fun `POST register with duplicate username returns 400`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val username = uniqueUsername()
        c.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, "password123"))
        }
        val response = c.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, "password123"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST login with valid credentials returns token and user`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val username = uniqueUsername()
        c.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, "password123"))
        }
        val response = c.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, "password123"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertTrue(body.token.isNotBlank())
        assertEquals(username, body.user.username)
    }

    @Test
    fun `POST login with wrong password returns 400`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val username = uniqueUsername()
        c.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, "password123"))
        }
        val response = c.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, "wrongpassword"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST login with unknown username returns 400`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val response = c.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("nobody_${uniqueUsername()}", "password123"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ── Todo auth guard ───────────────────────────────────────────────────────

    @Test
    fun `GET todos without token returns 401`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.Unauthorized, client.get("/todos").status)
    }

    @Test
    fun `GET todos with invalid token returns 401`() = testApplication {
        configure()
        val response = client.get("/todos") { bearerAuth("not.a.valid.token") }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ── Todo CRUD (authenticated) ─────────────────────────────────────────────

    @Test
    fun `GET todos with valid token returns empty list for new user`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndGetToken(uniqueUsername())
        val response = c.get("/todos") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<Todo>>())
    }

    @Test
    fun `POST todos creates todo and GET returns it`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndGetToken(uniqueUsername())

        val created = c.post("/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateTodoRequest("Buy milk"))
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val todo = created.body<Todo>()
        assertEquals("Buy milk", todo.title)
        assertFalse(todo.completed)

        val list = c.get("/todos") { bearerAuth(token) }.body<List<Todo>>()
        assertEquals(1, list.size)
        assertEquals(todo.id, list[0].id)
    }

    @Test
    fun `POST todos trims whitespace from title`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndGetToken(uniqueUsername())
        val response = c.post("/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateTodoRequest("  Buy milk  "))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("Buy milk", response.body<Todo>().title)
    }

    @Test
    fun `POST todos with blank title returns 400`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndGetToken(uniqueUsername())
        val response = c.post("/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateTodoRequest("   "))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT todo updates title and completed`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndGetToken(uniqueUsername())

        val todo = c.post("/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateTodoRequest("Original"))
        }.body<Todo>()

        val updated = c.put("/todos/${todo.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateTodoRequest(title = "Updated", completed = true))
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        val body = updated.body<Todo>()
        assertEquals("Updated", body.title)
        assertTrue(body.completed)
    }

    @Test
    fun `DELETE todo removes it and subsequent GET returns 404`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndGetToken(uniqueUsername())

        val todo = c.post("/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateTodoRequest("To delete"))
        }.body<Todo>()

        assertEquals(HttpStatusCode.NoContent, c.delete("/todos/${todo.id}") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.NotFound, c.get("/todos/${todo.id}") { bearerAuth(token) }.status)
    }

    // ── User isolation ────────────────────────────────────────────────────────

    @Test
    fun `todos are scoped to authenticated user`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val tokenA = registerAndGetToken(uniqueUsername())
        val tokenB = registerAndGetToken(uniqueUsername())

        val todo = c.post("/todos") {
            bearerAuth(tokenA)
            contentType(ContentType.Application.Json)
            setBody(CreateTodoRequest("User A's private todo"))
        }.body<Todo>()

        // B's list is empty
        assertEquals(emptyList(), c.get("/todos") { bearerAuth(tokenB) }.body<List<Todo>>())
        // B cannot fetch A's todo by id
        assertEquals(HttpStatusCode.NotFound, c.get("/todos/${todo.id}") { bearerAuth(tokenB) }.status)
    }

    @Test
    fun `PUT and DELETE on another user's todo returns 404`() = testApplication {
        configure()
        val c = createClient { install(ContentNegotiation) { json() } }
        val tokenA = registerAndGetToken(uniqueUsername())
        val tokenB = registerAndGetToken(uniqueUsername())

        val todo = c.post("/todos") {
            bearerAuth(tokenA)
            contentType(ContentType.Application.Json)
            setBody(CreateTodoRequest("User A's todo"))
        }.body<Todo>()

        assertEquals(
            HttpStatusCode.NotFound,
            c.put("/todos/${todo.id}") {
                bearerAuth(tokenB)
                contentType(ContentType.Application.Json)
                setBody(UpdateTodoRequest(title = "Hijacked"))
            }.status
        )
        assertEquals(HttpStatusCode.NotFound, c.delete("/todos/${todo.id}") { bearerAuth(tokenB) }.status)
    }
}
