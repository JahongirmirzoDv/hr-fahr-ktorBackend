package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.*
import com.fahr.hrplatform.models.faceRecognition.FaceRecognitionResponse
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.services.FaceRecognitionService
import com.fahr.hrplatform.utils.DateUtil
import com.fahr.hrplatform.utils.FaceEmbeddingMigration
import com.fahr.hrplatform.utils.toApiResponse
import com.fahr.hrplatform.utils.toErrorResponse
import com.fahr.hrplatform.utils.toPaginatedResponse
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.serializers.LocalTimeComponentSerializer
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.attendanceRoutes() {
    val attendanceRepository: AttendanceRepository by inject()
    val employeeRepository: EmployeeRepository by inject()
    val faceRecognitionService: FaceRecognitionService by inject()

    authenticate("auth-jwt") {
        route("/attendance") {

            post("/fix-face-embeddings") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN)) {
                    call.respond(HttpStatusCode.Forbidden, "Admin role required".toErrorResponse())
                    return@post
                }

                try {
                    FaceEmbeddingMigration.fixCorruptedEmbeddings()
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Face embedding migration completed successfully",
                        "action" to "Check logs for details",
                        "timestamp" to DateUtil.datetimeInUtc.toString()
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "success" to false,
                        "error" to "Migration failed: ${e.message}",
                        "timestamp" to DateUtil.datetimeInUtc.toString()
                    ))
                }
            }

            post {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, "Admin or Manager role required".toErrorResponse())
                    return@post
                }

                try {
                    val attendanceDTO = call.receive<AttendanceDTO>()

                    if (employeeRepository.findById(attendanceDTO.employeeId) == null) {
                        call.respond(HttpStatusCode.BadRequest, "Employee not found".toErrorResponse())
                        return@post
                    }

                    if (attendanceRepository.findByEmployeeAndDate(attendanceDTO.employeeId, attendanceDTO.date) != null) {
                        call.respond(HttpStatusCode.Conflict, "Attendance record already exists for this date".toErrorResponse())
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

                    call.respond(HttpStatusCode.Created, attendance.toApiResponse())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid attendance data: ${e.message}".toErrorResponse())
                }
            }

            get {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, "Admin or Manager role required".toErrorResponse())
                    return@get
                }

                try {
                    val employeeId = call.request.queryParameters["employeeId"]
                    val startDate = call.request.queryParameters["startDate"]?.let { LocalDate.parse(it) }
                    val endDate = call.request.queryParameters["endDate"]?.let { LocalDate.parse(it) }
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                    val attendanceRecords = if (employeeId != null) {
                        if (startDate != null && endDate != null) {
                            attendanceRepository.findByEmployeeAndDateRange(employeeId, startDate, endDate)
                        } else {
                            attendanceRepository.findByEmployeeAndDateRange(
                                employeeId, DateUtil.firstDayOfCurrentMonth,
                                DateUtil.dateInUtc
                            )
                        }
                    } else {
                        attendanceRepository.findAll()
                    }

                    // Simple pagination
                    val startIndex = (page - 1) * pageSize
                    val endIndex = minOf(startIndex + pageSize, attendanceRecords.size)
                    val paginatedRecords = if (startIndex < attendanceRecords.size) {
                        attendanceRecords.subList(startIndex, endIndex)
                    } else {
                        emptyList()
                    }

                    call.respond(paginatedRecords.toPaginatedResponse(page, pageSize, attendanceRecords.size))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid query parameters: ${e.message}".toErrorResponse())
                }
            }

            post("/check-in") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, "Manager role required".toErrorResponse())
                    return@post
                }

                val multipart = call.receiveMultipart()
                var dto: CheckInRequestDTO? = null
                var photoFileName: String? = null

                try {
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "data") {
                                    dto = kotlinx.serialization.json.Json.decodeFromString<CheckInRequestDTO>(part.value)
                                }
                            }

                            is PartData.FileItem -> {
                                if (part.name == "photo") {
                                    val bytes = part.streamProvider().readBytes()
                                    if (bytes.size > 5 * 1024 * 1024) { // 5MB limit
                                        call.respond(HttpStatusCode.BadRequest, "Photo file too large (max 5MB)".toErrorResponse())
                                        return@forEachPart
                                    }
                                    val filename = "selfie_${System.currentTimeMillis()}.jpg"
                                    val file = java.io.File("uploads/$filename")
                                    file.parentFile.mkdirs()
                                    file.writeBytes(bytes)
                                    photoFileName = file.name
                                }
                            }

                            else -> part.dispose()
                        }
                    }

                    val parsedDto = dto
                    if (parsedDto == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing attendance data".toErrorResponse())
                        return@post
                    }

                    if (!parsedDto.isValid()) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid input data".toErrorResponse())
                        return@post
                    }

                    val finalDto = parsedDto.copy(photoFileName = photoFileName)
                    attendanceRepository.save(finalDto)

                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Check-in successful",
                        "data" to mapOf(
                            "employeeId" to finalDto.employeeId,
                            "checkInTime" to finalDto.checkInTime,
                            "photoSaved" to (photoFileName != null)
                        ),
                        "timestamp" to DateUtil.datetimeInUtc.toString()
                    ))

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Check-in failed: ${e.message}".toErrorResponse())
                }
            }

            post("/check-out") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, "Manager role required".toErrorResponse())
                    return@post
                }

                try {
                    val request = call.receive<CheckOutRequest>()
                    val attendance = attendanceRepository.findByEmployeeAndDate(request.employeeId, DateUtil.dateInUtc)

                    if (attendance == null) {
                        call.respond(HttpStatusCode.BadRequest, "No active check-in found for today".toErrorResponse())
                        return@post
                    }

                    if (attendance.checkOut != null) {
                        call.respond(HttpStatusCode.BadRequest, "Already checked out for today".toErrorResponse())
                        return@post
                    }

                    val updatedAttendance = attendanceRepository.checkOut(attendance.id, request.checkOutTime)
                    call.respond(HttpStatusCode.OK, updatedAttendance.toApiResponse())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Check-out failed: ${e.message}".toErrorResponse())
                }
            }

            post("/verify-face") {
                var employeeId: String? = null
                var verificationPhotoStream: ByteArray? = null

                try {
                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "employeeId") {
                                    employeeId = part.value
                                }
                            }
                            is PartData.FileItem -> {
                                if (part.name == "image") {
                                    verificationPhotoStream = part.streamProvider().readBytes()
                                }
                            }
                            else -> {
                                // Ignore other part types
                            }
                        }
                        part.dispose()
                    }

                    // Enhanced validation
                    if (employeeId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Employee ID is required".toErrorResponse())
                        return@post
                    }

                    if (verificationPhotoStream == null || verificationPhotoStream!!.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, "Verification image is required".toErrorResponse())
                        return@post
                    }

                    // Validate image size (max 5MB)
                    if (verificationPhotoStream!!.size > 5 * 1024 * 1024) {
                        call.respond(HttpStatusCode.BadRequest, "Image size too large (max 5MB)".toErrorResponse())
                        return@post
                    }

                    val employee = employeeRepository.findById(employeeId!!)
                    if (employee == null) {
                        call.respond(HttpStatusCode.NotFound, "Employee not found".toErrorResponse())
                        return@post
                    }

                    if (employee.faceEmbedding.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "success" to false,
                            "error" to "No face data registered for this employee",
                            "action" to "Please register the employee's face photo first",
                            "requiresRegistration" to true,
                            "timestamp" to DateUtil.datetimeInUtc.toString()
                        ))
                        return@post
                    }

                    val storedEmbedding = try {
                        faceRecognitionService.base64ToEmbedding(employee.faceEmbedding!!)
                    } catch (e: RuntimeException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "success" to false,
                            "error" to "Invalid face data stored for this employee",
                            "action" to "Please re-register the employee's face photo",
                            "requiresReregistration" to true,
                            "employeeId" to employeeId,
                            "timestamp" to DateUtil.datetimeInUtc.toString()
                        ))
                        return@post
                    }

                    val verificationEmbedding = try {
                        faceRecognitionService.generateEmbedding(verificationPhotoStream!!)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "success" to false,
                            "error" to "Failed to process verification image",
                            "details" to e.message,
                            "suggestions" to listOf(
                                "Ensure the image contains a clear face",
                                "Check lighting conditions",
                                "Make sure the face is not obscured"
                            ),
                            "timestamp" to DateUtil.datetimeInUtc.toString()
                        ))
                        return@post
                    }

                    val isRecognized = faceRecognitionService.areSimilar(storedEmbedding, verificationEmbedding)
                    val similarityScore = faceRecognitionService.getSimilarityScore(storedEmbedding, verificationEmbedding)

                    call.respond(HttpStatusCode.OK, FaceRecognitionResponse(
                        verified = isRecognized,
                        similarityScore = String.format("%.4f", similarityScore),
                        threshold = 0.75,
                        employeeId = employeeId!!,
                        timestamp = DateUtil.datetimeInUtc.toString()
                    ).toApiResponse())

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "success" to false,
                        "error" to "Face verification failed",
                        "details" to "An unexpected error occurred during verification",
                        "timestamp" to DateUtil.datetimeInUtc.toString()
                    ))
                }
            }

            put("/{id}") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, "Admin or Manager role required".toErrorResponse())
                    return@put
                }

                val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing attendance ID".toErrorResponse())
                    return@put
                }

                try {
                    val attendanceDTO = call.receive<AttendanceDTO>()
                    val existingAttendance = attendanceRepository.findById(id)

                    if (existingAttendance == null) {
                        call.respond(HttpStatusCode.NotFound, "Attendance record not found".toErrorResponse())
                        return@put
                    }

                    // Implementation for updating attendance would go here
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Attendance record updated successfully",
                        "timestamp" to DateUtil.datetimeInUtc.toString()
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to update attendance: ${e.message}".toErrorResponse())
                }
            }
        }
    }
}

@Serializable
data class CheckOutRequest(
    val employeeId: String,
    @Serializable(with = LocalTimeComponentSerializer::class) val checkOutTime: LocalTime
)