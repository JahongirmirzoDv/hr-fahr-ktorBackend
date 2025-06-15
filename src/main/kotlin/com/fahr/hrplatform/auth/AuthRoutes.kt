package com.fahr.hrplatform.auth

import com.fahr.hrplatform.models.UserDTO
import com.fahr.hrplatform.repository.UserRepository
import com.fahr.hrplatform.security.JwtConfig
import com.fahr.hrplatform.utils.PasswordHasher
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            val userDTO = call.receive<UserDTO>()
            val userRepository = UserRepository()

            // Check if user already exists
            val existingUser = userRepository.findByEmail(userDTO.email)
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "User with this email already exists"))
                return@post
            }

            // Hash password
            val passwordHash = PasswordHasher.hashPassword(userDTO.password)

            // Create user
            val user = userRepository.create(
                userDTO.fullName,
                userDTO.email,
                passwordHash,
                userDTO.role
            )

            call.respond(HttpStatusCode.Created, mapOf("userId" to user.id))
        }
        post("/login") {

            val loginRequest = call.receive<LoginRequest>()

            val userRepository = UserRepository()
            val user = userRepository.findByEmail(loginRequest.email)

            if (user == null || !PasswordHasher.verifyPassword(loginRequest.password, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            val token = JwtConfig.generateToken(user.email, user.role)
            call.respond(mapOf("token" to token, "user" to mapOf(
                "id" to user.id,
                "fullName" to user.fullName,
                "email" to user.email,
                "role" to user.role
            )))
        }
    }
}
