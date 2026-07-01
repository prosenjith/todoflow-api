package com.example

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test todos endpoint returns 200`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/todos").status)
    }

}
