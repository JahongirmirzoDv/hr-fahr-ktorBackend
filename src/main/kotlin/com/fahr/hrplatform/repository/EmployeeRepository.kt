package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class EmployeeRepository {

    suspend fun create(
        userId: String,
        position: String,
        department: String,
        hireDate: LocalDateTime,
        salaryType: SalaryType,
        salaryAmount: Double,
        isActive: Boolean = true
    ): Employee = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        EmployeeTable.insert {
            it[EmployeeTable.id] = UUID.fromString(id)
            it[EmployeeTable.userId] = UUID.fromString(userId)
            it[EmployeeTable.position] = position
            it[EmployeeTable.department] = department
            it[EmployeeTable.hireDate] = hireDate
            it[EmployeeTable.salaryType] = salaryType.name
            it[EmployeeTable.salaryAmount] = salaryAmount
            it[EmployeeTable.isActive] = isActive
            it[createdAt] = now
            it[updatedAt] = now
        }

        Employee(
            id = id,
            userId = userId,
            position = position,
            department = department,
            hireDate = hireDate,
            salaryType = salaryType,
            salaryAmount = salaryAmount,
            isActive = isActive,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun findById(id: String): Employee? = dbQuery {
        EmployeeTable.select { EmployeeTable.id eq UUID.fromString(id) }
            .mapNotNull { toEmployee(it) }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: String): Employee? = dbQuery {
        EmployeeTable.select { EmployeeTable.userId eq UUID.fromString(userId) }
            .mapNotNull { toEmployee(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<Employee> = dbQuery {
        EmployeeTable.selectAll().map { toEmployee(it) }
    }

    private fun toEmployee(row: ResultRow): Employee {
        return Employee(
            id = row[EmployeeTable.id].toString(),
            userId = row[EmployeeTable.userId].toString(),
            position = row[EmployeeTable.position],
            department = row[EmployeeTable.department],
            hireDate = row[EmployeeTable.hireDate],
            salaryType = SalaryType.valueOf(row[EmployeeTable.salaryType]),
            salaryAmount = row[EmployeeTable.salaryAmount],
            isActive = row[EmployeeTable.isActive],
            createdAt = row[EmployeeTable.createdAt],
            updatedAt = row[EmployeeTable.updatedAt]
        )
    }
}
