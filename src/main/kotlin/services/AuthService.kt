package com.prosenjith.todoflow.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.prosenjith.todoflow.exceptions.ValidationException
import com.prosenjith.todoflow.models.AuthResponse
import com.prosenjith.todoflow.models.User
import com.prosenjith.todoflow.repositories.UserRepository
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

private const val TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours

class AuthService(private val userRepository: UserRepository) {

    // DEV ONLY — set JWT_SECRET to a strong random value in production
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "dev-only-secret-do-not-use-in-production"

    fun hashPassword(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt())

    fun verifyPassword(plain: String, hash: String): Boolean = BCrypt.checkpw(plain, hash)

    fun generateToken(user: User): String = JWT.create()
        .withClaim("userId", user.id)
        .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_EXPIRY_MS))
        .sign(Algorithm.HMAC256(jwtSecret))

    fun register(username: String, password: String): User {
        if (userRepository.findByUsername(username) != null) {
            throw ValidationException("Username already taken")
        }
        return userRepository.create(username, hashPassword(password))
    }

    fun login(username: String, password: String): AuthResponse {
        val (user, hash) = userRepository.findByUsername(username)
            ?: throw ValidationException("Invalid credentials")
        if (!verifyPassword(password, hash)) throw ValidationException("Invalid credentials")
        return AuthResponse(token = generateToken(user), user = user)
    }
}
