package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.User
import com.fahr.hrplatform.models.UserTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class UserRepository {

    suspend fun create(fullName: String, email: String, passwordHash: String, role: String): User = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        UserTable.insert {
            it[UserTable.id] = UUID.fromString(id)
            it[UserTable.fullName] = fullName
            it[UserTable.email] = email
            it[UserTable.passwordHash] = passwordHash
            it[UserTable.role] = role
            it[createdAt] = now
            it[updatedAt] = now
        }

        User(
            id = id,
            fullName = fullName,
            email = email,
            passwordHash = passwordHash,
            role = role,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        UserTable.select { UserTable.email eq email }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }

    suspend fun findById(id: String): User? = dbQuery {
        UserTable.select { UserTable.id eq UUID.fromString(id) }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<User> = dbQuery {
        UserTable.selectAll().map { toUser(it) }
    }

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[UserTable.id].toString(),
            fullName = row[UserTable.fullName],
            email = row[UserTable.email],
            passwordHash = row[UserTable.passwordHash],
            role = row[UserTable.role],
            createdAt = row[UserTable.createdAt],
            updatedAt = row[UserTable.updatedAt]
        )
    }
}
