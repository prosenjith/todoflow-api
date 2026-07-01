package com.example.repositories

import com.example.models.Todo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object Todos : Table("todos") {
    val id = varchar("id", 36)
    val title = varchar("title", 255)
    val completed = bool("completed").default(false)
    val userId = varchar("user_id", 36)
    override val primaryKey = PrimaryKey(id)
}

class TodoRepository {

    fun getAll(userId: String): List<Todo> = transaction {
        Todos.selectAll().where { Todos.userId eq userId }.map { it.toTodo() }
    }

    fun getById(id: String, userId: String): Todo? = transaction {
        Todos.selectAll()
            .where { (Todos.id eq id) and (Todos.userId eq userId) }
            .singleOrNull()?.toTodo()
    }

    fun create(title: String, userId: String): Todo = transaction {
        val id = UUID.randomUUID().toString()
        Todos.insert {
            it[Todos.id] = id
            it[Todos.title] = title
            it[completed] = false
            it[Todos.userId] = userId
        }
        Todo(id = id, title = title, completed = false)
    }

    fun update(id: String, userId: String, title: String?, completed: Boolean?): Todo? = transaction {
        val existing = Todos.selectAll()
            .where { (Todos.id eq id) and (Todos.userId eq userId) }
            .singleOrNull()?.toTodo()
            ?: return@transaction null
        Todos.update({ (Todos.id eq id) and (Todos.userId eq userId) }) {
            if (title != null) it[Todos.title] = title
            if (completed != null) it[Todos.completed] = completed
        }
        existing.copy(
            title = title ?: existing.title,
            completed = completed ?: existing.completed
        )
    }

    fun delete(id: String, userId: String): Boolean = transaction {
        Todos.deleteWhere { (Todos.id eq id) and (Todos.userId eq userId) } > 0
    }

    private fun ResultRow.toTodo() = Todo(
        id = this[Todos.id],
        title = this[Todos.title],
        completed = this[Todos.completed]
    )
}
