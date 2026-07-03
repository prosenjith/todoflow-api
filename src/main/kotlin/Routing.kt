package com.prosenjith.todoflow

import com.prosenjith.todoflow.repositories.TodoRepository
import com.prosenjith.todoflow.repositories.UserRepository
import com.prosenjith.todoflow.routes.authRoutes
import com.prosenjith.todoflow.routes.todoRoutes
import com.prosenjith.todoflow.services.AuthService
import com.prosenjith.todoflow.services.TodoService
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
