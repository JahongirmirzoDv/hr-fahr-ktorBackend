package com.fahr.hrplatform.models

import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.serializers.LocalDateComponentSerializer
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.time.format.DateTimeFormatter


object ProjectTable : UUIDTable("projects") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val status = varchar("status", 20).default("ACTIVE") // ACTIVE, COMPLETED, CANCELLED, ON_HOLD
    val budget = double("budget").nullable()
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
    val updatedAt = datetime("updated_at").default(DateUtil.datetimeInUtc)
    val managerId = reference("manager_id", UserTable.id).nullable()
    val employeeIds = text("employee_ids").default("")
    val location = varchar("location", 255).nullable()

}

data class Project(
    val id: String,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: String,
    val budget: Double?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val managerId: String,
    val employeeIds: List<String>,
    val location: String
)


//@Serializable
data class ProjectDTO(
    val name: String,
    val description: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: String = "ACTIVE",
    val budget: Double? = null,
    val managerId: String? = null,
    val employeeIds: List<String> = emptyList(),
    val location: String? = null
)
