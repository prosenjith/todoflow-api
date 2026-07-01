package com.example.services

import org.mindrot.jbcrypt.BCrypt

class AuthService {

    fun hashPassword(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt())

    fun verifyPassword(plain: String, hash: String): Boolean = BCrypt.checkpw(plain, hash)
}
