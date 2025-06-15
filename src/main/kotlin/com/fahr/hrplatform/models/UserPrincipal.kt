package com.fahr.hrplatform.models

import io.ktor.server.auth.Principal
import java.util.UUID


data class UserPrincipal(
    val userId: UUID,
    val email: String,
    val role: Role
) : Principal

enum class Role {
    ADMIN, MANAGER, USER
}

fun UserPrincipal.requireRole(vararg allowed: Role): Boolean {
    return this.role in allowed
}
