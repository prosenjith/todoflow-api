package com.example.services

import com.example.exceptions.ValidationException
import com.example.models.User
import com.example.repositories.UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthService(private val userRepository: UserRepository) {

    fun hashPassword(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt())

    fun verifyPassword(plain: String, hash: String): Boolean = BCrypt.checkpw(plain, hash)

    fun register(username: String, password: String): User {
        if (userRepository.findByUsername(username) != null) {
            throw ValidationException("Username already taken")
        }
        return userRepository.create(username, hashPassword(password))
    }

    fun login(username: String, password: String): User {
        val (user, hash) = userRepository.findByUsername(username)
            ?: throw ValidationException("Invalid credentials")
        if (!verifyPassword(password, hash)) throw ValidationException("Invalid credentials")
        return user
    }
}
