package com.example.routes

import com.example.models.LoginRequest
import com.example.models.RegisterRequest
import com.example.services.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(service: AuthService) {
    post("/register") {
        val request = call.receive<RegisterRequest>()
        call.respond(HttpStatusCode.Created, service.register(request.username, request.password))
    }

    post("/login") {
        val request = call.receive<LoginRequest>()
        call.respond(HttpStatusCode.OK, service.login(request.username, request.password))
    }
}
