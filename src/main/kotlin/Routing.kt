package com.example

import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.example.repositories.TodoRepository
import com.example.routes.todoRoutes

fun Application.configureRouting() {
    val todoRepository = TodoRepository()

    routing {
        todoRoutes(todoRepository)
    }
}