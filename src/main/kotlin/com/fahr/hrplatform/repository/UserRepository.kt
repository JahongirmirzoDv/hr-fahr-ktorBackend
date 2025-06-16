package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.User
import com.fahr.hrplatform.models.UserTable
import com.fahr.hrplatform.utils.DateUtil
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class UserRepository {

    suspend fun create(fullName: String, email: String, passwordHash: String, role: String): User? = dbQuery {
        val id = UUID.randomUUID()
        val now = DateUtil.datetimeInUtc

        UserTable.insert {
            it[UserTable.id] = id
            it[UserTable.fullName] = fullName
            it[UserTable.email] = email
            it[UserTable.passwordHash] = passwordHash
            it[UserTable.role] = role
            it[createdAt] = now
            it[updatedAt] = now
        }

        findById(id.toString())
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
            createdAt = row[UserTable.createdAt].toString(),
            updatedAt = row[UserTable.updatedAt].toString()
        )
    }
}