package com.prosenjith.todoflow.routes

import com.prosenjith.todoflow.exceptions.ValidationException
import com.prosenjith.todoflow.models.CreateTodoRequest
import com.prosenjith.todoflow.models.UpdateTodoRequest
import com.prosenjith.todoflow.services.TodoService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun ApplicationCall.userId(): String =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()

fun Route.todoRoutes(service: TodoService) {
    authenticate("auth-jwt") {
        route("/todos") {

            get {
                call.respond(HttpStatusCode.OK, service.getAll(call.userId()))
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: throw ValidationException("Missing id")
                call.respond(HttpStatusCode.OK, service.getById(id, call.userId()))
            }

            post {
                val request = call.receive<CreateTodoRequest>()
                call.respond(HttpStatusCode.Created, service.create(request, call.userId()))
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: throw ValidationException("Missing id")
                val request = call.receive<UpdateTodoRequest>()
                call.respond(HttpStatusCode.OK, service.update(id, call.userId(), request))
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: throw ValidationException("Missing id")
                service.delete(id, call.userId())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
