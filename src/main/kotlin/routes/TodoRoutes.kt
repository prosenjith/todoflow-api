package com.example.routes

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
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
            try {
                call.respond(HttpStatusCode.OK, service.getById(id))
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, e.message ?: "Todo not found")
            }
        }

        post {
            val request = call.receive<CreateTodoRequest>()
            try {
                call.respond(HttpStatusCode.Created, service.create(request))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing id")
            val request = call.receive<UpdateTodoRequest>()
            try {
                call.respond(HttpStatusCode.OK, service.update(id, request))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, e.message ?: "Todo not found")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")
            try {
                service.delete(id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, e.message ?: "Todo not found")
            }
        }
    }
}
