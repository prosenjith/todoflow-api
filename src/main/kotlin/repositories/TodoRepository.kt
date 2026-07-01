package com.example.repositories

import com.example.models.Todo
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

class TodoRepository {
    private val todos = ConcurrentHashMap<String, Todo>()

    fun getAll(): List<Todo> = todos.values.toList()

    fun getById(id: String): Todo? = todos[id]

    fun create(title: String): Todo {
        val id = UUID.randomUUID().toString()
        val todo = Todo(id = id, title = title)
        todos[id] = todo
        return todo
    }

    fun update(id: String, title: String?, completed: Boolean?): Todo? {
        val existing = todos[id] ?: return null
        val updated = existing.copy(
            title = title ?: existing.title,
            completed = completed ?: existing.completed
        )
        todos[id] = updated
        return updated
    }

    fun delete(id: String): Boolean = todos.remove(id) != null
}