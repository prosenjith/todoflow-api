package com.prosenjith.todoflow.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    // DEV ONLY — set JWT_SECRET to a strong random value in production
    val jwtSecret = System.getenv("JWT_SECRET") ?: "dev-only-secret-do-not-use-in-production"

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret)).build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}
