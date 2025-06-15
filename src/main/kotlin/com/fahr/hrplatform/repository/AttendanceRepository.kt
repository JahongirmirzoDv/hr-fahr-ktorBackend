package com.fahr.hrplatform.repository

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.Attendance
import com.fahr.hrplatform.models.AttendanceEntity
import com.fahr.hrplatform.models.AttendanceTable
import com.fahr.hrplatform.models.CheckInRequestDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class AttendanceRepository {

    suspend fun create(
        employeeId: String,
        date: LocalDate,
        checkIn: LocalTime?,
        checkOut: LocalTime?,
        status: String,
        notes: String?
    ): Attendance = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        AttendanceTable.insert {
            it[AttendanceTable.id] = UUID.fromString(id)
            it[AttendanceTable.employeeId] = UUID.fromString(employeeId)
            it[AttendanceTable.date] = date
            it[AttendanceTable.checkIn] = checkIn
            it[AttendanceTable.checkOut] = checkOut
            it[AttendanceTable.status] = status
            it[AttendanceTable.notes] = notes
            it[createdAt] = now
            it[updatedAt] = now
        }

        Attendance(
            id = id,
            employeeId = employeeId,
            date = date,
            checkIn = checkIn,
            checkOut = checkOut,
            status = status,
            notes = notes,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun findById(id: String): Attendance? = dbQuery {
        AttendanceTable.select { AttendanceTable.id eq UUID.fromString(id) }
            .mapNotNull { toAttendance(it) }
            .singleOrNull()
    }

    suspend fun findByEmployeeAndDate(employeeId: String, date: LocalDate): Attendance? = dbQuery {
        AttendanceTable.select {
            (AttendanceTable.employeeId eq UUID.fromString(employeeId)) and
                    (AttendanceTable.date eq date)
        }
            .mapNotNull { toAttendance(it) }
            .singleOrNull()
    }

    suspend fun findByEmployeeAndDateRange(
        employeeId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Attendance> = dbQuery {
        AttendanceTable.select {
            (AttendanceTable.employeeId eq UUID.fromString(employeeId)) and
                    (AttendanceTable.date greaterEq startDate) and
                    (AttendanceTable.date lessEq endDate)
        }
            .map { toAttendance(it) }
    }

    suspend fun findAll(): List<Attendance> = dbQuery {
        AttendanceTable.selectAll().map { toAttendance(it) }
    }

    private fun toAttendance(row: ResultRow): Attendance {
        return Attendance(
            id = row[AttendanceTable.id].toString(),
            employeeId = row[AttendanceTable.employeeId].toString(),
            date = row[AttendanceTable.date],
            checkIn = row[AttendanceTable.checkIn],
            checkOut = row[AttendanceTable.checkOut],
            status = row[AttendanceTable.status],
            notes = row[AttendanceTable.notes],
            createdAt = row[AttendanceTable.createdAt],
            updatedAt = row[AttendanceTable.updatedAt]
        )
    }

    private val attendances = mutableListOf<CheckInRequestDTO>()

    fun save(dto: CheckInRequestDTO) {
            transaction {
                AttendanceEntity.insert {
                    it[id] = UUID.randomUUID()
                    it[employeeId] = dto.employeeId
                    it[checkInTime] = dto.checkInTime
                    it[gpsLat] = dto.gpsLat
                    it[gpsLng] = dto.gpsLng
                    it[nfcCardId] = dto.nfcCardId
                    it[photoFileName] = dto.photoFileName
                    it[createdAt] = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
            }
        AuditLogRepository.log(
            action = "CHECK_IN",
            actorId = dto.employeeId,
            details = "Check-in from NFC: ${dto.nfcCardId} at ${dto.checkInTime}"
        )
    }

    fun getAll() = attendances

}
