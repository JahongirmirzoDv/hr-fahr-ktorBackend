package com.fahr.hrplatform.utils

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {
    private const val BCRYPT_COST = 12

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
    }

    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword)
        return result.verified
    }
}
