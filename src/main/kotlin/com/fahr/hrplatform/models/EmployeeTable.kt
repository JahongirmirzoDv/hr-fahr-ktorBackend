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
    val id = uuid("id").autoGenerate()

    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)

    val name = varchar("name", 255)

    val faceEmbedding = text("face_embedding").nullable() // Storing as Base64 text

    val salaryRate = double("salary_rate").default(0.0)
    val position = varchar("position", 255)
    val department = varchar("department", 255)
    val hireDate = datetime("hire_date").default(DateUtil.datetimeInUtc)
    val salaryType = varchar("salary_type", 20) // MONTHLY, DAILY, HOURLY
    val salaryAmount = double("salary_amount")
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
    val updatedAt = datetime("updated_at").default(DateUtil.datetimeInUtc)


}

@Serializable
data class Employee(
    val id: String,
    val name: String,
    val faceEmbedding: String?, // ADDED
    val position: String,
    val salaryRate: Double,
    val salaryType: SalaryType,
    val salaryAmount: Double,
    val userId: String,
    val department: String,
    val hireDate: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean
)

@Serializable
data class EmployeeDTO(
    val name: String,
    val position: String,
    val salaryRate: Double? = null,
    val salaryType: SalaryType,
    val salaryAmount: Double,
    val userId: String,
    val department: String,
    val hireDate: LocalDateTime = DateUtil.datetimeInUtc,
    val createdAt: LocalDateTime = DateUtil.datetimeInUtc,
    val updatedAt: LocalDateTime = DateUtil.datetimeInUtc,
    val isActive: Boolean = true

)

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
    val isActive: Boolean,
    val createdAt: LocalDateTime = DateUtil.datetimeInUtc,
    val updatedAt: LocalDateTime = DateUtil.datetimeInUtc,
    val salaryRate: Double? = null
)
