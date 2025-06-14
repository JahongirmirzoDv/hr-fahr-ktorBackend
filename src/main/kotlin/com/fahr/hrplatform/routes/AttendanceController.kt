package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.AttendanceDTO
import com.fahr.hrplatform.models.CheckInRequestDTO
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.isValid
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.serializers.LocalDateTimeComponentSerializer
import kotlinx.datetime.serializers.LocalTimeComponentSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Route.attendanceRoutes() {
    val attendanceRepository: AttendanceRepository by inject()
    val employeeRepository: EmployeeRepository by inject()

    authenticate("auth-jwt") {
        route("/attendance") {
            post {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@post
                }

                val attendanceDTO = call.receive<AttendanceDTO>()
                if (employeeRepository.findById(attendanceDTO.employeeId) == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee not found"))
                    return@post
                }

                if (attendanceRepository.findByEmployeeAndDate(attendanceDTO.employeeId, attendanceDTO.date) != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Attendance record already exists for this date"))
                    return@post
                }

                val attendance = attendanceRepository.create(
                    employeeId = attendanceDTO.employeeId,
                    date = attendanceDTO.date,
                    checkIn = attendanceDTO.checkIn,
                    checkOut = attendanceDTO.checkOut,
                    status = attendanceDTO.status,
                    notes = attendanceDTO.notes
                )
                call.respond(HttpStatusCode.Created, attendance.toString())
            }

            get {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@get
                }

                val employeeId = call.request.queryParameters["employeeId"]
                val startDate = call.request.queryParameters["startDate"]?.let { LocalDate.parse(it) }
                val endDate = call.request.queryParameters["endDate"]?.let { LocalDate.parse(it) }

                val attendanceRecords = if (employeeId != null) {
                    if (startDate != null && endDate != null) {
                        attendanceRepository.findByEmployeeAndDateRange(employeeId, startDate, endDate)
                    } else {
                        attendanceRepository.findByEmployeeAndDateRange(employeeId, LocalDate.now().withDayOfMonth(1), LocalDate.now())
                    }
                } else {
                    attendanceRepository.findAll()
                }
                call.respond(attendanceRecords)
            }

            // This route now has a specific role check for Manager
            post("/check-in") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Manager role required"))
                    return@post
                }

                val multipart = call.receiveMultipart()
                var dto: CheckInRequestDTO? = null
                var photoFileName: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "data") {
                                dto = Json.decodeFromString<CheckInRequestDTO>(part.value)
                            }
                        }
                        is PartData.FileItem -> {
                            if (part.name == "photo") {
                                val bytes = part.streamProvider().readBytes()
                                val filename = "selfie_${System.currentTimeMillis()}.jpg"
                                val file = File("uploads/$filename")
                                file.parentFile.mkdirs() // Ensure directory exists
                                file.writeBytes(bytes)
                                photoFileName = file.name
                            }
                        }
                        else -> part.dispose()
                    }
                }

                val parsedDto = dto
                if (parsedDto == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing attendance data")
                    return@post
                }

                if (!parsedDto.isValid()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                    return@post
                }

                val finalDto = parsedDto.copy(photoFileName = photoFileName)
                attendanceRepository.save(finalDto)
                call.respond(HttpStatusCode.OK, "Check-in successful")

            }

            post("/check-out") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Manager role required"))
                    return@post
                }
                val request = call.receive<CheckOutRequest>()
                val attendance = attendanceRepository.findByEmployeeAndDate(request.employeeId, LocalDate.now())

                if (attendance == null) {
                    call.respond(HttpStatusCode.BadRequest, "No active check-in found for today")
                    return@post
                }

                if (attendance.checkOut != null) {
                    call.respond(HttpStatusCode.BadRequest, "Already checked out for today")
                    return@post
                }

                val updatedAttendance = attendanceRepository.checkOut(attendance.id, request.checkOutTime)
                call.respond(HttpStatusCode.OK, updatedAttendance.toString())
            }
        }
    }
}

@Serializable
data class CheckOutRequest(
    val employeeId: String,
    @Serializable(with = LocalTimeComponentSerializer::class) val checkOutTime: LocalTime
)