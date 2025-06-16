package com.fahr.hrplatform.models

import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ProjectAssignmentTable : UUIDTable("project_assignments") {
    val projectId = reference("project_id", ProjectTable, onDelete = ReferenceOption.CASCADE)
    val employeeId = reference("employee_id", EmployeeTable, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 255) // Project role: Developer, Designer, Manager, etc.
    val assignmentDate = date("assignment_date").default(DateUtil.dateInUtc)
    val endDate = date("end_date").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
    val updatedAt = datetime("updated_at").default(DateUtil.datetimeInUtc)
}

data class ProjectAssignment(
    val id: String,
    val projectId: String,
    val employeeId: String,
    val role: String,
    val assignmentDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ProjectAssignmentDTO(
    val projectId: String,
    val employeeId: String,
    val role: String,
    val assignmentDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true
)
