package com.fahr.hrplatform.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class EmployeeSalarySummary(
    val employeeId: String,
    val fullName: String,
    val department: String,
    val position: String? = null, // ADDED: position field
    val totalBaseAmount: Double,
    val totalBonus: Double,
    val totalDeductions: Double,
    val totalNetAmount: Double,
    val paymentStatus: String? = null, // ADDED: payment status
    val totalHours: Double? = null // ADDED: total hours for hourly employees
)

@Serializable
data class MonthlyReport(
    val reportDate: String,
    val period: String,
    val summaries: List<EmployeeSalarySummary>,
    val totalEmployees: Int = summaries.size, // ADDED: summary statistics
    val totalAmount: Double = summaries.sumOf { it.totalNetAmount },
    val departmentBreakdown: Map<String, DepartmentSummary>? = null
)

@Serializable
data class DepartmentSummary(
    val department: String,
    val employeeCount: Int,
    val totalAmount: Double,
    val averageAmount: Double
)

@Serializable
data class AttendanceReport(
    val reportDate: String,
    val period: String,
    val employeeAttendance: List<EmployeeAttendanceSummary>
)

@Serializable
data class EmployeeAttendanceSummary(
    val employeeId: String,
    val fullName: String,
    val department: String,
    val totalDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val halfDays: Int,
    val attendancePercentage: Double
)