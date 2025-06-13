package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.Project
import com.fahr.hrplatform.models.ProjectTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ProjectRepository {

    suspend fun create(
        name: String,
        description: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        status: String = "ACTIVE",
        budget: Double?
    ): Project = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        ProjectTable.insert {
            it[ProjectTable.id] = UUID.fromString(id)
            it[ProjectTable.name] = name
            it[ProjectTable.description] = description
            it[ProjectTable.startDate] = startDate
            it[ProjectTable.endDate] = endDate
            it[ProjectTable.status] = status
            it[ProjectTable.budget] = budget
            it[createdAt] = now
            it[updatedAt] = now
        }

        Project(
            id = id,
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            status = status,
            budget = budget,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun findById(id: String): Project? = dbQuery {
        ProjectTable.select { ProjectTable.id eq UUID.fromString(id) }
            .mapNotNull { toProject(it) }
            .singleOrNull()
    }

    suspend fun findByStatus(status: String): List<Project> = dbQuery {
        ProjectTable.select { ProjectTable.status eq status }
            .map { toProject(it) }
    }

    suspend fun findAll(): List<Project> = dbQuery {
        ProjectTable.selectAll().map { toProject(it) }
    }

    private fun toProject(row: ResultRow): Project {
        return Project(
            id = row[ProjectTable.id].toString(),
            name = row[ProjectTable.name],
            description = row[ProjectTable.description],
            startDate = row[ProjectTable.startDate],
            endDate = row[ProjectTable.endDate],
            status = row[ProjectTable.status],
            budget = row[ProjectTable.budget],
            createdAt = row[ProjectTable.createdAt],
            updatedAt = row[ProjectTable.updatedAt]
        )
    }
}
