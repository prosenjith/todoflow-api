package com.example

import com.example.repositories.Todos
import io.ktor.server.application.*
import org.h2.tools.Server
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    try {
        Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092").start()
        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
    } catch (e: Exception) {
        log.warn("H2 servers could not start: ${e.message}")
    }

    Database.connect(
        url = "jdbc:h2:mem:todos;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
    transaction {
        SchemaUtils.create(Todos)
    }
}