package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.AttendanceDTO
import com.fahr.hrplatform.models.CheckInRequestDTO
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.isValid
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.services.FaceRecognitionService
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.serializers.LocalTimeComponentSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.io.File
import java.io.InputStream

fun Route.attendanceRoutes() {
    val attendanceRepository: AttendanceRepository by inject()
    val employeeRepository: EmployeeRepository by inject()
    val faceRecognitionService: FaceRecognitionService by inject() // Inject


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
                        attendanceRepository.findByEmployeeAndDateRange(employeeId, DateUtil.firstDayOfCurrentMonth,
                            DateUtil.dateInUtc)
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
                val attendance = attendanceRepository.findByEmployeeAndDate(request.employeeId, DateUtil.dateInUtc)

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

            // THIS IS THE NEW /verify-face ROUTE
            post("/verify-face") {
                var employeeId: String? = null
                var verificationPhotoStream: ByteArray? = null

                try {
                    // Receive multipart data (employeeId and the verification image)
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
                        part.dispose() // Always dispose parts
                    }

                    // Validate input parameters
                    if (employeeId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee ID is required"))
                        return@post
                    }

                    if (verificationPhotoStream == null || verificationPhotoStream!!.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Verification image is required"))
                        return@post
                    }

                    // Initialize face recognition service

                    // 1. Fetch the stored employee record
                    val employee = employeeRepository.findById(employeeId!!)
                    if (employee == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee not found"))
                        return@post
                    }

                    if (employee.faceEmbedding.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "error" to "No face data registered for this employee",
                            "action" to "Please register the employee's face photo first"
                        ))
                        return@post
                    }

                    // 2. Convert stored Base64 embedding back to a float array
                    val storedEmbedding = try {
                        faceRecognitionService.base64ToEmbedding(employee.faceEmbedding!!)
                    } catch (e: RuntimeException) {
                        // Log the error for debugging
                        println("Error decoding face embedding for employee $employeeId: ${e.message}")

                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "error" to "Invalid face data stored for this employee",
                            "details" to e.message,
                            "action" to "Please re-register the employee's face photo",
                            "employeeId" to employeeId
                        ))
                        return@post
                    }

                    // 3. Generate a new embedding from the live verification photo
                    val verificationEmbedding = try {
                        faceRecognitionService.generateEmbedding(verificationPhotoStream!!)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "error" to "Failed to process verification image",
                            "details" to e.message,
                            "action" to "Please ensure the image contains a clear face and try again"
                        ))
                        return@post
                    }

                    // 4. Compare the two embeddings
                    val isRecognized = faceRecognitionService.areSimilar(storedEmbedding, verificationEmbedding)
                    val similarityScore = faceRecognitionService.getSimilarityScore(storedEmbedding, verificationEmbedding)

                    // Log the verification attempt
                    println("Face verification attempt for employee $employeeId:")
                    println("- Similarity score: ${String.format("%.4f", similarityScore)}")
                    println("- Recognition result: $isRecognized")

                    // 5. Respond with the verification result
                    call.respond(HttpStatusCode.OK, mapOf(
                        "verified" to isRecognized,
                        "similarityScore" to String.format("%.4f", similarityScore),
                        "threshold" to 0.75,
                        "employeeId" to employeeId,
                        "timestamp" to DateUtil.datetimeInUtc.toString()
                    ))

                } catch (e: Exception) {
                    println("Unexpected error in face verification: ${e.message}")
                    e.printStackTrace()

                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "error" to "Face verification failed",
                        "details" to "An unexpected error occurred during verification",
                        "timestamp" to DateUtil.datetimeInUtc.toString()
                    ))
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