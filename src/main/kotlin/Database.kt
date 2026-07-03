package com.prosenjith.todoflow

import com.prosenjith.todoflow.repositories.Todos
import com.prosenjith.todoflow.repositories.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/todos"
    val user = System.getenv("DATABASE_USER") ?: "postgres"
    val password = System.getenv("DATABASE_PASSWORD") ?: "devpassword"

    Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password
    )
    transaction {
        @Suppress("DEPRECATION")
        SchemaUtils.createMissingTablesAndColumns(Todos, Users)
    }
}