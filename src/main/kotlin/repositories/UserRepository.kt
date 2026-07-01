package com.example.repositories

import com.example.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object Users : Table("users") {
    val id = varchar("id", 36)
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    override val primaryKey = PrimaryKey(id)
}

class UserRepository {

    fun create(username: String, passwordHash: String): User = transaction {
        val id = UUID.randomUUID().toString()
        Users.insert {
            it[Users.id] = id
            it[Users.username] = username
            it[Users.passwordHash] = passwordHash
        }
        User(id = id, username = username)
    }

    fun findByUsername(username: String): Pair<User, String>? = transaction {
        Users.selectAll().where { Users.username eq username }.singleOrNull()?.let {
            User(id = it[Users.id], username = it[Users.username]) to it[Users.passwordHash]
        }
    }

    fun findById(id: String): User? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.let {
            User(id = it[Users.id], username = it[Users.username])
        }
    }
}
