package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.Employee
import com.fahr.hrplatform.models.EmployeeTable
import com.fahr.hrplatform.models.SalaryType
import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import java.util.*

class EmployeeRepository {

    suspend fun create(
        userId: String,
        name: String,
        position: String,
        department: String,
        hireDate: LocalDateTime,
        salaryType: SalaryType,
        salaryAmount: Double,
        salaryRate: Double,
        isActive: Boolean = true,
        faceEmbedding: String
    ): Employee? = dbQuery {
        val now = DateUtil.datetimeInUtc

        val insertedId = EmployeeTable.insertAndGetId {
            // No need to set 'id' - UUIDTable handles it automatically
            it[EmployeeTable.userId] = UUID.fromString(userId)
            it[EmployeeTable.name] = name
            it[EmployeeTable.position] = position
            it[EmployeeTable.department] = department
            it[EmployeeTable.hireDate] = hireDate
            it[EmployeeTable.salaryType] = salaryType.name
            it[EmployeeTable.salaryAmount] = salaryAmount
            it[EmployeeTable.salaryRate] = salaryRate
            it[EmployeeTable.isActive] = isActive
            it[EmployeeTable.faceEmbedding] = faceEmbedding
            it[EmployeeTable.createdAt] = now
            it[EmployeeTable.updatedAt] = now
        }

        findById(insertedId.toString())
    }

    suspend fun findById(id: String): Employee? = dbQuery {
        EmployeeTable.selectAll().where { EmployeeTable.id eq UUID.fromString(id) }
            .mapNotNull { toEmployee(it) }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: String): Employee? = dbQuery {
        EmployeeTable.selectAll().where { EmployeeTable.userId eq UUID.fromString(userId) }
            .mapNotNull { toEmployee(it) }
            .singleOrNull()
    }

    suspend fun findByDepartment(department: String): List<Employee> = dbQuery {
        EmployeeTable.selectAll().where { EmployeeTable.department eq department }
            .map { toEmployee(it) }
    }

    suspend fun findAll(): List<Employee> = dbQuery {
        EmployeeTable.selectAll().map { toEmployee(it) }
    }

    private fun toEmployee(row: ResultRow): Employee {
        return Employee(
            id = row[EmployeeTable.id].toString(),
            userId = row[EmployeeTable.userId].toString(),
            name = row[EmployeeTable.name],
            position = row[EmployeeTable.position],
            department = row[EmployeeTable.department],
            hireDate = row[EmployeeTable.hireDate],
            salaryType = SalaryType.valueOf(row[EmployeeTable.salaryType]),
            salaryAmount = row[EmployeeTable.salaryAmount],
            salaryRate = row[EmployeeTable.salaryRate],
            isActive = row[EmployeeTable.isActive],
            faceEmbedding = row[EmployeeTable.faceEmbedding],
            createdAt = row[EmployeeTable.createdAt],
            updatedAt = row[EmployeeTable.updatedAt]
        )
    }
}