package com.fahr.hrplatform.models

import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.Date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

enum class SalaryType {
    MONTHLY, DAILY, HOURLY
}

object EmployeeTable : UUIDTable("employees") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val position = varchar("position", 255)
    val department = varchar("department", 255)
    val hireDate = datetime("hire_date").default(DateUtil.datetimeInUtc)
    val salaryType = varchar("salary_type", 20) // MONTHLY, DAILY, HOURLY
    val salaryAmount = double("salary_amount")
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
    val updatedAt = datetime("updated_at").default(DateUtil.datetimeInUtc)
}

data class Employee(
    val id: String,
    val userId: String,
    val position: String,
    val department: String,
    val hireDate: LocalDateTime,
    val salaryType: SalaryType,
    val salaryAmount: Double,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class EmployeeDTO(
    val userId: String,
    val position: String,
    val department: String,
    val hireDate: LocalDateTime? = null,
    val salaryType: SalaryType,
    val salaryAmount: Double,
    val isActive: Boolean = true
)
