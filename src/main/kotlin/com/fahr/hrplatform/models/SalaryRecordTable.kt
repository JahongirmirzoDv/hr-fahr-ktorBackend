package com.fahr.hrplatform.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime

object SalaryRecordTable : UUIDTable("salary_records") {
    val employeeId = reference("employee_id", EmployeeTable, onDelete = ReferenceOption.CASCADE)
    val periodStart = date("period_start")
    val periodEnd = date("period_end")
    val baseAmount = double("base_amount")
    val bonus = double("bonus").default(0.0)
    val deductions = double("deductions").default(0.0)
    val netAmount = double("net_amount")
    val paymentStatus = varchar("payment_status", 20).default("PENDING") // PENDING, PAID, CANCELLED
    val paymentDate = date("payment_date").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

data class SalaryRecord(
    val id: String,
    val employeeId: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val baseAmount: Double,
    val bonus: Double,
    val deductions: Double,
    val netAmount: Double,
    val paymentStatus: String,
    val paymentDate: LocalDate?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class SalaryCalculationDTO(
    val employeeId: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val bonus: Double? = 0.0,
    val deductions: Double? = 0.0
)
