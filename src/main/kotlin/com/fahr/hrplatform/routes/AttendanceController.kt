package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.AttendanceDTO
import com.fahr.hrplatform.models.CheckInRequestDTO
import com.fahr.hrplatform.models.isValid
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.UserRepository
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

fun Route.attendanceRoutes() {
    authenticate("auth-jwt") {
        route("/attendance") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin" && role != "manager") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins and managers can record attendance"))
                    return@post
                }

                val attendanceDTO = call.receive<AttendanceDTO>()

                // Validate that employee exists
                val employeeRepository = EmployeeRepository()
                val employee = employeeRepository.findById(attendanceDTO.employeeId)
                if (employee == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee not found"))
                    return@post
                }

                val attendanceRepository = AttendanceRepository()

                // Check if attendance already exists for this date
                val existingAttendance = attendanceRepository.findByEmployeeAndDate(
                    attendanceDTO.employeeId, 
                    attendanceDTO.date
                )

                if (existingAttendance != null) {
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

                call.respond(HttpStatusCode.Created, mapOf(
                    "id" to attendance.id,
                    "employeeId" to attendance.employeeId,
                    "date" to attendance.date,
                    "status" to attendance.status
                ))
            }

            get {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""
                val userEmail = principal?.payload?.getClaim("email")?.asString() ?: ""

                val employeeId = call.request.queryParameters["employeeId"]
                val startDate = call.request.queryParameters["startDate"]?.let { LocalDate.parse(it) }
                val endDate = call.request.queryParameters["endDate"]?.let { LocalDate.parse(it) }

                val attendanceRepository = AttendanceRepository()
                val employeeRepository = EmployeeRepository()
                val userRepository = UserRepository()

                // If user is an employee, they can only see their own attendance
                if (role == "employee") {
                    val user = userRepository.findByEmail(userEmail) ?: run {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found"))
                        return@get
                    }

                    val userEmployee = employeeRepository.findByUserId(user.id) ?: run {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee record not found"))
                        return@get
                    }

                    // If employeeId is provided, make sure it matches the current user's employee record
                    if (employeeId != null && employeeId != userEmployee.id) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You can only access your own attendance records"))
                        return@get
                    }

                    val attendanceRecords = if (startDate != null && endDate != null) {
                        attendanceRepository.findByEmployeeAndDateRange(userEmployee.id, startDate, endDate)
                    } else {
                        attendanceRepository.findByEmployeeAndDateRange(
                            userEmployee.id,
                            LocalDate.now().withDayOfMonth(1), // First day of current month
                            LocalDate.now()
                        )
                    }

                    call.respond(attendanceRecords)
                } else {
                    // Admin and managers can see all attendance records or filter by employee
                    val attendanceRecords = if (employeeId != null) {
                        if (startDate != null && endDate != null) {
                            attendanceRepository.findByEmployeeAndDateRange(employeeId, startDate, endDate)
                        } else {
                            attendanceRepository.findByEmployeeAndDateRange(
                                employeeId,
                                LocalDate.now().withDayOfMonth(1), // First day of current month
                                LocalDate.now()
                            )
                        }
                    } else {
                        // Without filters, return all attendance for the current month
                        attendanceRepository.findAll()
                    }

                    call.respond(attendanceRecords)
                }
            }

            post("/attendance/check-in") {
                val multipart = call.receiveMultipart()
                val dto: CheckInRequestDTO? = null
                var photoFileName: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "data") {
                                val dto = Json.decodeFromString<CheckInRequestDTO>(part.value)
                            }
                        }
                        is PartData.FileItem -> {
                            if (part.name == "photo") {
                                val bytes = part.streamProvider().readBytes()
                                val filename = "selfie_${System.currentTimeMillis()}.jpg"
                                val file = File("uploads/$filename")
                                file.writeBytes(bytes)
                                photoFileName = file.name
                            }
                        }
                        else -> part.dispose()
                    }
                }

                if (dto == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing attendance data")
                    return@post
                }

                if (dto.isValid()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                    return@post
                }

                val attendanceRepository = AttendanceRepository()
                val finalDto = dto.copy(photoFileName = photoFileName)
                attendanceRepository.save(finalDto) // bu sizda hali yozilmagan bo'lishi mumkin
                call.respond(HttpStatusCode.OK, "Check-in successful")
            }
        }

    }
}
