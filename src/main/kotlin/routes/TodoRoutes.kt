package com.example.routes

import com.example.exceptions.ValidationException
import com.example.models.CreateTodoRequest
import com.example.models.UpdateTodoRequest
import com.example.services.TodoService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.todoRoutes(service: TodoService) {
    route("/todos") {

        get {
            call.respond(HttpStatusCode.OK, service.getAll())
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("Missing id")
            call.respond(HttpStatusCode.OK, service.getById(id))
        }

        post {
            val request = call.receive<CreateTodoRequest>()
            call.respond(HttpStatusCode.Created, service.create(request))
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("Missing id")
            val request = call.receive<UpdateTodoRequest>()
            call.respond(HttpStatusCode.OK, service.update(id, request))
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("Missing id")
            service.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
