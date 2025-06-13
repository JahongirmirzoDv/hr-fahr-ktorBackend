package com.fahr.hrplatform.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.time
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object AttendanceTable : UUIDTable("attendance") {
    val employeeId = reference("employee_id", EmployeeTable, onDelete = ReferenceOption.CASCADE)
    val date = date("date")
    val checkIn = time("check_in").nullable()
    val checkOut = time("check_out").nullable()
    val status = varchar("status", 20) // PRESENT, ABSENT, HALF_DAY, LEAVE, etc.
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

data class Attendance(
    val id: String,
    val employeeId: String,
    val date: LocalDate,
    val checkIn: LocalTime?,
    val checkOut: LocalTime?,
    val status: String,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class AttendanceDTO(
    val employeeId: String,
    val date: LocalDate,
    val checkIn: LocalTime? = null,
    val checkOut: LocalTime? = null,
    val status: String,
    val notes: String? = null
)
