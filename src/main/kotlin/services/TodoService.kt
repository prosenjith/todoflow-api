package com.example.services

import com.example.exceptions.NotFoundException
import com.example.exceptions.ValidationException
import com.example.models.CreateTodoRequest
import com.example.models.Todo
import com.example.models.UpdateTodoRequest
import com.example.repositories.TodoRepository

private const val MAX_TITLE_LENGTH = 200

class TodoService(private val repository: TodoRepository) {

    fun getAll(userId: String): List<Todo> = repository.getAll(userId)

    fun getById(id: String, userId: String): Todo =
        repository.getById(id, userId) ?: throw NotFoundException("Todo not found")

    fun create(request: CreateTodoRequest, userId: String): Todo {
        val title = request.title.trim()
        if (title.isBlank()) throw ValidationException("Title cannot be blank")
        if (title.length > MAX_TITLE_LENGTH) throw ValidationException("Title cannot exceed $MAX_TITLE_LENGTH characters")
        return repository.create(title, userId)
    }

    fun update(id: String, userId: String, request: UpdateTodoRequest): Todo {
        val title = request.title?.trim()
        if (title != null) {
            if (title.isBlank()) throw ValidationException("Title cannot be blank")
            if (title.length > MAX_TITLE_LENGTH) throw ValidationException("Title cannot exceed $MAX_TITLE_LENGTH characters")
        }
        return repository.update(id, userId, title, request.completed)
            ?: throw NotFoundException("Todo not found")
    }

    fun delete(id: String, userId: String) {
        if (!repository.delete(id, userId)) throw NotFoundException("Todo not found")
    }
}
