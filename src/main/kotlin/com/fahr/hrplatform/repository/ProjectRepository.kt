package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.Project
import com.fahr.hrplatform.models.ProjectTable
import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ProjectRepository {

    suspend fun create(
        name: String,
        description: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        status: String = "ACTIVE",
        budget: Double?,
        managerId: String?,
        employeeIds: List<String>,
        location: String? = null
    ): Project? = dbQuery {
        val id = UUID.randomUUID()
        val now = DateUtil.datetimeInUtc

        ProjectTable.insert {
            it[ProjectTable.id] = id
            it[ProjectTable.name] = name
            it[ProjectTable.description] = description
            it[ProjectTable.startDate] = startDate
            it[ProjectTable.endDate] = endDate
            it[ProjectTable.status] = status
            it[ProjectTable.budget] = budget
            it[createdAt] = now
            it[updatedAt] = now
            it[ProjectTable.managerId] = managerId?.let { id -> UUID.fromString(id) }
            it[ProjectTable.employeeIds] = employeeIds.joinToString(",").ifEmpty { "" }
            it[ProjectTable.location] = location
        }
        findById(id.toString())
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

    // FIXED: Handle nullable values properly
    private fun toProject(row: ResultRow): Project {
        return Project(
            id = row[ProjectTable.id].toString(),
            name = row[ProjectTable.name],
            description = row[ProjectTable.description] ?: "",
            startDate = row[ProjectTable.startDate],
            endDate = row[ProjectTable.endDate] ?: row[ProjectTable.startDate], // FIXED: Provide valid default
            status = row[ProjectTable.status],
            budget = row[ProjectTable.budget] ?: 0.0,
            createdAt = row[ProjectTable.createdAt],
            updatedAt = row[ProjectTable.updatedAt],
            managerId = row[ProjectTable.managerId]?.toString() ?: "", // FIXED: Handle nullable UUID
            employeeIds = row[ProjectTable.employeeIds].split(",").map { it.trim() }.filter { it.isNotEmpty() }, // FIXED: Filter empty strings
            location = row[ProjectTable.location] ?: ""
        )
    }
}