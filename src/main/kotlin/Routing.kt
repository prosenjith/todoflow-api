package com.example

import com.example.repositories.TodoRepository
import com.example.repositories.UserRepository
import com.example.routes.authRoutes
import com.example.routes.todoRoutes
import com.example.services.AuthService
import com.example.services.TodoService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val todoRepository = TodoRepository()
    val todoService = TodoService(todoRepository)

    val userRepository = UserRepository()
    val authService = AuthService(userRepository)

    routing {
        todoRoutes(todoService)
        authRoutes(authService)
    }
}
