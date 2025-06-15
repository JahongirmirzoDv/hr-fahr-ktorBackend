package com.fahr.hrplatform.models

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class EmployeeSalarySummary(
    val employeeId: String,
    val fullName: String,
    val department: String,
    val totalBaseAmount: Double,
    val totalBonus: Double,
    val totalDeductions: Double,
    val totalNetAmount: Double
)

@Serializable
data class MonthlyReport(
    val reportDate: String,
    val period: String,
    val summaries: List<EmployeeSalarySummary>
)