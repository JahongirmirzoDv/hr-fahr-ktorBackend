package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import kotlinx.datetime.LocalDate // FIXED: Use kotlinx.datetime instead of java.time

class ReportRepository {

    suspend fun getMonthlySummary(startDate: LocalDate, endDate: LocalDate): List<EmployeeSalarySummary> = dbQuery {
        (SalaryRecordTable innerJoin EmployeeTable innerJoin UserTable)
            .slice(
                EmployeeTable.id,
                UserTable.fullName,
                EmployeeTable.department,
                SalaryRecordTable.baseAmount.sum(),
                SalaryRecordTable.bonus.sum(),
                SalaryRecordTable.deductions.sum(),
                SalaryRecordTable.netAmount.sum()
            )
            .select {
                (SalaryRecordTable.periodStart greaterEq startDate) and
                        (SalaryRecordTable.periodEnd lessEq endDate)
            }
            .groupBy(EmployeeTable.id, UserTable.fullName, EmployeeTable.department)
            .map {
                EmployeeSalarySummary(
                    employeeId = it[EmployeeTable.id].toString(),
                    fullName = it[UserTable.fullName],
                    department = it[EmployeeTable.department],
                    totalBaseAmount = it[SalaryRecordTable.baseAmount.sum()] ?: 0.0,
                    totalBonus = it[SalaryRecordTable.bonus.sum()] ?: 0.0,
                    totalDeductions = it[SalaryRecordTable.deductions.sum()] ?: 0.0,
                    totalNetAmount = it[SalaryRecordTable.netAmount.sum()] ?: 0.0
                )
            }
    }
}