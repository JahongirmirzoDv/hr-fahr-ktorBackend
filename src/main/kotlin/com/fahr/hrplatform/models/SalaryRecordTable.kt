package com.fahr.hrplatform.models

import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

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
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
    val updatedAt = datetime("updated_at").default(DateUtil.datetimeInUtc)
}

@Serializable
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

@Serializable
data class SalaryCalculationDTO(
    val employeeId: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val bonus: Double? = 0.0,
    val deductions: Double? = 0.0
)