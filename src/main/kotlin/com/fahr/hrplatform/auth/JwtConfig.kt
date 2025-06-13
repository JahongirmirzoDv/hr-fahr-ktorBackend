package com.fahr.hrplatform.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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

    fun generateToken(email: String, role: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("email", email)
        .withClaim("role", role)
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)
}

fun Application.configureSecurity() {
    authentication {
        jwt {
            realm = "Access to HR Platform"
            verifier(
                JWT.require(Algorithm.HMAC256("your_secret_key_here"))
                    .withIssuer("com.fahr.hrplatform")
                    .withAudience("com.fahr.hrplatform.users")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}
