package com.fahr.hrplatform.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime

object ProjectTable : UUIDTable("projects") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val status = varchar("status", 20).default("ACTIVE") // ACTIVE, COMPLETED, CANCELLED, ON_HOLD
    val budget = double("budget").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
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
    val updatedAt: LocalDateTime
)

data class ProjectDTO(
    val name: String,
    val description: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: String = "ACTIVE",
    val budget: Double? = null
)
