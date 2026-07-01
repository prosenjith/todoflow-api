package com.example.services

import com.example.exceptions.NotFoundException
import com.example.exceptions.ValidationException
import com.example.models.CreateTodoRequest
import com.example.models.Todo
import com.example.models.UpdateTodoRequest
import com.example.repositories.TodoRepository

private const val MAX_TITLE_LENGTH = 200

class TodoService(private val repository: TodoRepository) {

    fun getAll(): List<Todo> = repository.getAll()

    fun getById(id: String): Todo =
        repository.getById(id) ?: throw NotFoundException("Todo not found")

    fun create(request: CreateTodoRequest): Todo {
        val title = request.title.trim()
        if (title.isBlank()) throw ValidationException("Title cannot be blank")
        if (title.length > MAX_TITLE_LENGTH) throw ValidationException("Title cannot exceed $MAX_TITLE_LENGTH characters")
        return repository.create(title)
    }

    fun update(id: String, request: UpdateTodoRequest): Todo {
        val title = request.title?.trim()
        if (title != null) {
            if (title.isBlank()) throw ValidationException("Title cannot be blank")
            if (title.length > MAX_TITLE_LENGTH) throw ValidationException("Title cannot exceed $MAX_TITLE_LENGTH characters")
        }
        return repository.update(id, title, request.completed)
            ?: throw NotFoundException("Todo not found")
    }

    fun delete(id: String) {
        if (!repository.delete(id)) throw NotFoundException("Todo not found")
    }
}
