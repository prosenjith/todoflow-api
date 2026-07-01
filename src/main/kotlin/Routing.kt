package com.example

import com.example.repositories.TodoRepository
import com.example.routes.todoRoutes
import com.example.services.TodoService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val todoRepository = TodoRepository()
    val todoService = TodoService(todoRepository)

    routing {
        todoRoutes(todoService)
    }
}
