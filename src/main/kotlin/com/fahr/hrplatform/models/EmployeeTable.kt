package com.fahr.hrplatform.models

import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.Date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

@Serializable
enum class SalaryType {
    MONTHLY, DAILY, HOURLY
}

object EmployeeTable : UUIDTable("employees") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val name = varchar("name", 255)
    val faceEmbedding = text("face_embedding").nullable()
    val salaryRate = double("salary_rate").default(0.0)
    val position = varchar("position", 255)
    val department = varchar("department", 255)
    val hireDate = datetime("hire_date").default(DateUtil.datetimeInUtc)
    val salaryType = varchar("salary_type", 20).default("MONTHLY")
    val salaryAmount = double("salary_amount").default(0.0)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
    val updatedAt = datetime("updated_at").default(DateUtil.datetimeInUtc)
}

@Serializable
data class Employee(
    val id: String,
    val userId: String,
    val name: String,
    val position: String,
    val department: String,
    val hireDate: LocalDateTime,
    val salaryType: SalaryType,
    val salaryAmount: Double,
    val salaryRate: Double,
    val isActive: Boolean,
    val faceEmbedding: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class EmployeeDTO(
    val userId: String,
    val name: String,
    val position: String,
    val department: String,
    val salaryType: SalaryType,
    val salaryAmount: Double,
    val salaryRate: Double? = null,
    val hireDate: LocalDateTime? = null,
    val isActive: Boolean = true
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (name.isBlank()) errors.add("Name cannot be empty")
        if (position.isBlank()) errors.add("Position cannot be empty")
        if (department.isBlank()) errors.add("Department cannot be empty")
        if (salaryAmount <= 0) errors.add("Salary amount must be greater than 0")
        if (salaryRate != null && salaryRate < 0) errors.add("Salary rate cannot be negative")

        return errors
    }
}

@Serializable
data class EmployeeResponse(
    val id: String,
    val userId: String,
    val name: String,
    val email: String,
    val position: String,
    val department: String,
    val hireDate: String,
    val salaryType: SalaryType,
    val salaryAmount: Double,
    val salaryRate: Double?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)