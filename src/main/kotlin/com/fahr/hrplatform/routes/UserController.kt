package com.fahr.hrplatform.routes

import com.fahr.hrplatform.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        route("/users") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins can access this resource"))
                    return@get
                }

                val userRepository = UserRepository()
                val users = userRepository.findAll().map {
                    mapOf(
                        "id" to it.id,
                        "fullName" to it.fullName,
                        "email" to it.email,
                        "role" to it.role,
                        "createdAt" to it.createdAt
                    )
                }
                call.respond(users)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""
                val userEmail = principal?.payload?.getClaim("email")?.asString() ?: ""

                val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id parameter"))
                    return@get
                }

                val userRepository = UserRepository()
                val user = userRepository.findById(id)

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@get
                }

                // Check if the user is requesting their own data or is an admin
                if (role != "admin" && userEmail != user.email) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You can only access your own user information"))
                    return@get
                }

                call.respond(mapOf(
                    "id" to user.id,
                    "fullName" to user.fullName,
                    "email" to user.email,
                    "role" to user.role,
                    "createdAt" to user.createdAt
                ))
            }
        }
    }
}
