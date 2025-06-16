package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.SalaryRecord
import com.fahr.hrplatform.models.SalaryRecordTable
import com.fahr.hrplatform.utils.DateUtil
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import java.util.*

class SalaryRepository {

    suspend fun create(
        employeeId: String,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        baseAmount: Double,
        bonus: Double,
        deductions: Double,
        netAmount: Double,
        paymentStatus: String = "PENDING",
        paymentDate: LocalDate? = null
    ): SalaryRecord? = dbQuery {
        val id = UUID.randomUUID()
        val now = DateUtil.datetimeInUtc

        SalaryRecordTable.insert {
            it[SalaryRecordTable.id] = id
            it[SalaryRecordTable.employeeId] = UUID.fromString(employeeId)
            it[SalaryRecordTable.periodStart] = periodStart
            it[SalaryRecordTable.periodEnd] = periodEnd
            it[SalaryRecordTable.baseAmount] = baseAmount
            it[SalaryRecordTable.bonus] = bonus
            it[SalaryRecordTable.deductions] = deductions
            it[SalaryRecordTable.netAmount] = netAmount
            it[SalaryRecordTable.paymentStatus] = paymentStatus
            it[SalaryRecordTable.paymentDate] = paymentDate
            it[createdAt] = now
            it[updatedAt] = now
        }
        findById(id.toString())
    }

    suspend fun findById(id: String): SalaryRecord? = dbQuery {
        SalaryRecordTable.select { SalaryRecordTable.id eq UUID.fromString(id) }
            .mapNotNull { toSalaryRecord(it) }
            .singleOrNull()
    }

    suspend fun findByEmployeeAndPeriod(employeeId: String, periodStart: LocalDate, periodEnd: LocalDate): SalaryRecord? = dbQuery {
        SalaryRecordTable.select {
            (SalaryRecordTable.employeeId eq UUID.fromString(employeeId)) and
                    (SalaryRecordTable.periodStart eq periodStart) and
                    (SalaryRecordTable.periodEnd eq periodEnd)
        }
            .mapNotNull { toSalaryRecord(it) }
            .singleOrNull()
    }

    suspend fun findByEmployee(employeeId: String): List<SalaryRecord> = dbQuery {
        SalaryRecordTable.select { SalaryRecordTable.employeeId eq UUID.fromString(employeeId) }
            .map { toSalaryRecord(it) }
    }

    suspend fun findAll(): List<SalaryRecord> = dbQuery {
        SalaryRecordTable.selectAll().map { toSalaryRecord(it) }
    }

    private fun toSalaryRecord(row: ResultRow): SalaryRecord {
        return SalaryRecord(
            id = row[SalaryRecordTable.id].toString(),
            employeeId = row[SalaryRecordTable.employeeId].toString(),
            periodStart = row[SalaryRecordTable.periodStart],
            periodEnd = row[SalaryRecordTable.periodEnd],
            baseAmount = row[SalaryRecordTable.baseAmount],
            bonus = row[SalaryRecordTable.bonus],
            deductions = row[SalaryRecordTable.deductions],
            netAmount = row[SalaryRecordTable.netAmount],
            paymentStatus = row[SalaryRecordTable.paymentStatus],
            paymentDate = row[SalaryRecordTable.paymentDate],
            createdAt = row[SalaryRecordTable.createdAt],
            updatedAt = row[SalaryRecordTable.updatedAt]
        )
    }
}