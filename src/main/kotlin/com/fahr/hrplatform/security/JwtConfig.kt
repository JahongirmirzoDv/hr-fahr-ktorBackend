package com.fahr.hrplatform.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.User
import com.fahr.hrplatform.models.UserPrincipal
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

object JwtConfig {
    private const val validityInMs = 36_000_00 * 24 // 24 hours

    // Read from configuration in a real app
    private const val secret = "your_secret_key_here"
    private const val issuer = "com.fahr.hrplatform"
    private const val audience = "com.fahr.hrplatform.users"
    private val algorithm = Algorithm.HMAC256(secret)

    // Modified to accept User object and include userId
    fun generateToken(user: User): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", user.id) // Added userId claim
        .withClaim("email", user.email)
        .withClaim("role", user.role)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") { // Name the provider "auth-jwt"
            realm = "Access to HR Platform"
            verifier(
                JWT.require(Algorithm.HMAC256("your_secret_key_here"))
                    .withIssuer("com.fahr.hrplatform")
                    .withAudience("com.fahr.hrplatform.users")
                    .build()
            )
            // Modified to validate and build a UserPrincipal
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                val roleString = credential.payload.getClaim("role").asString()

                if (userId != null && email != null && roleString != null) {
                    try {
                        UserPrincipal(
                            userId = UUID.fromString(userId),
                            email = email,
                            role = Role.valueOf(roleString.uppercase())
                        )
                    } catch (e: IllegalArgumentException) {
                        // Invalid role
                        null
                    }
                } else {
                    null
                }
            }
        }
    }
}