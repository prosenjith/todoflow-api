package com.example.routes

import com.example.models.CreateTodoRequest
import com.example.models.UpdateTodoRequest
import com.example.repositories.TodoRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.todoRoutes(repository: TodoRepository) {
    route("/todos") {

        // GET /todos
        get {
            call.respond(HttpStatusCode.OK, repository.getAll())
        }

        // GET /todos/{id}
        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")

            val todo = repository.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Todo not found")

            call.respond(HttpStatusCode.OK, todo)
        }

        // POST /todos
        post {
            val request = call.receive<CreateTodoRequest>()

            if (request.title.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, "Title cannot be blank")
            }

            val created = repository.create(request.title)
            call.respond(HttpStatusCode.Created, created)
        }

        // PUT /todos/{id}
        put("/{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing id")

            val request = call.receive<UpdateTodoRequest>()

            val updated = repository.update(id, request.title, request.completed)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Todo not found")

            call.respond(HttpStatusCode.OK, updated)
        }

        // DELETE /todos/{id}
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")

            val deleted = repository.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Todo not found")
            }
        }
    }
}