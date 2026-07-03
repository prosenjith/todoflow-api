package com.prosenjith.todoflow.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)
