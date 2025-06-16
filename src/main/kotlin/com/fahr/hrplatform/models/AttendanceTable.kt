package com.fahr.hrplatform.models

import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.Date
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.time

object AttendanceTable : UUIDTable("attendance") {
    val employeeId = reference("employee_id", EmployeeTable, onDelete = ReferenceOption.CASCADE)
    val date = date("date")
    val checkIn = time("check_in").nullable()
    val checkOut = time("check_out").nullable()
    val status = varchar("status", 20) // PRESENT, ABSENT, HALF_DAY, LEAVE, etc.
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
    val updatedAt = datetime("updated_at").default(DateUtil.datetimeInUtc)
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
