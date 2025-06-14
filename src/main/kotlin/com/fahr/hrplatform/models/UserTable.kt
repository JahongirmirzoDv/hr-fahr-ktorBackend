package com.fahr.hrplatform.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime


object UserTable : UUIDTable("users") {
    val fullName = varchar("full_name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 50) // admin, manager, accountant, employee
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

@Serializable
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class UserDTO(
    val fullName: String,
    val email: String,
    val password: String,
    val role: String
)
