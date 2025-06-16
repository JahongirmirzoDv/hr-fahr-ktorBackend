package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.ProjectAssignment
import com.fahr.hrplatform.models.ProjectAssignmentTable
import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import java.util.*

class ProjectAssignmentRepository {

    suspend fun create(
        projectId: String,
        employeeId: String,
        role: String,
        assignmentDate: LocalDate = DateUtil.dateInUtc,
        endDate: LocalDate? = null,
        isActive: Boolean = true
    ): ProjectAssignment? = dbQuery {
        val id = UUID.randomUUID()
        val now = DateUtil.datetimeInUtc

        ProjectAssignmentTable.insert {
            it[ProjectAssignmentTable.id] = id
            it[ProjectAssignmentTable.projectId] = UUID.fromString(projectId)
            it[ProjectAssignmentTable.employeeId] = UUID.fromString(employeeId)
            it[ProjectAssignmentTable.role] = role
            it[ProjectAssignmentTable.assignmentDate] = assignmentDate
            it[ProjectAssignmentTable.endDate] = endDate
            it[ProjectAssignmentTable.isActive] = isActive
            it[createdAt] = now
            it[updatedAt] = now
        }
        findById(id.toString())
    }

    suspend fun findById(id: String): ProjectAssignment? = dbQuery {
        ProjectAssignmentTable.select { ProjectAssignmentTable.id eq UUID.fromString(id) }
            .mapNotNull { toProjectAssignment(it) }
            .singleOrNull()
    }

    suspend fun findByProject(projectId: String): List<ProjectAssignment> = dbQuery {
        ProjectAssignmentTable.select { ProjectAssignmentTable.projectId eq UUID.fromString(projectId) }
            .map { toProjectAssignment(it) }
    }

    suspend fun findByEmployee(employeeId: String): List<ProjectAssignment> = dbQuery {
        ProjectAssignmentTable.select { ProjectAssignmentTable.employeeId eq UUID.fromString(employeeId) }
            .map { toProjectAssignment(it) }
    }

    suspend fun findActiveByEmployee(employeeId: String): List<ProjectAssignment> = dbQuery {
        ProjectAssignmentTable.select {
            (ProjectAssignmentTable.employeeId eq UUID.fromString(employeeId)) and
                    (ProjectAssignmentTable.isActive eq true)
        }
            .map { toProjectAssignment(it) }
    }

    suspend fun findAll(): List<ProjectAssignment> = dbQuery {
        ProjectAssignmentTable.selectAll().map { toProjectAssignment(it) }
    }

    private fun toProjectAssignment(row: ResultRow): ProjectAssignment {
        return ProjectAssignment(
            id = row[ProjectAssignmentTable.id].toString(),
            projectId = row[ProjectAssignmentTable.projectId].toString(),
            employeeId = row[ProjectAssignmentTable.employeeId].toString(),
            role = row[ProjectAssignmentTable.role],
            assignmentDate = row[ProjectAssignmentTable.assignmentDate],
            endDate = row[ProjectAssignmentTable.endDate],
            isActive = row[ProjectAssignmentTable.isActive],
            createdAt = row[ProjectAssignmentTable.createdAt],
            updatedAt = row[ProjectAssignmentTable.updatedAt]
        )
    }
}