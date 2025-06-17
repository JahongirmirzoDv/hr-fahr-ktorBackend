package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.EmployeeDTO
import com.fahr.hrplatform.models.EmployeeResponse
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.UserRepository
import com.fahr.hrplatform.services.FaceRecognitionService
import com.fahr.hrplatform.utils.DateUtil
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
import org.koin.ktor.ext.inject
import java.io.InputStream

fun Route.employeeRoutes() {
    val employeeRepository: EmployeeRepository by inject()
    val userRepository: UserRepository by inject()
    val faceRecognitionService: FaceRecognitionService by inject() // Inject the service


    authenticate("auth-jwt") {
        route("/employees") {
            post {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
                    return@post
                }

                var employeeDTO: EmployeeDTO? = null
                var photoStream: InputStream? = null

                // Receive a multipart request
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "data") {
                                // The employee JSON data
                                employeeDTO = Json.decodeFromString<EmployeeDTO>(part.value)
                            }
                        }
                        is PartData.FileItem -> {
                            if (part.name == "photo") {
                                // The employee photo
                                photoStream = part.streamProvider()
                            }
                        }
                        else -> part.dispose()
                    }
                }

                if (employeeDTO == null || photoStream == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing employee data or photo"))
                    return@post
                }

                // Generate and encode the face embedding
                val embedding = faceRecognitionService.generateEmbedding(photoStream!!)
                val embeddingBase64 = faceRecognitionService.embeddingToBase64(embedding)

                val dto = employeeDTO!!
                val employee = employeeRepository.create(
                    userId = dto.userId,
                    position = dto.position,
                    department = dto.department,
                    hireDate = dto.hireDate ?: DateUtil.datetimeInUtc,
                    salaryType = dto.salaryType,
                    salaryAmount = dto.salaryAmount,
                    isActive = dto.isActive,
                    name = dto.name,
                    faceEmbedding = embeddingBase64, // Save the new embedding
                    salaryRate = dto.salaryRate ?: 0.0,
                )
                call.respond(HttpStatusCode.Created, employee.toString())
            }

            get {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@get
                }

                val employees = employeeRepository.findAll().map { employee ->
                    val user = userRepository.findById(employee.userId)
                    EmployeeResponse(
                        id = employee.id,
                        userId = employee.userId,
                        name = user?.fullName ?: "",
                        email = user?.email ?: "",
                        position = employee.position,
                        department = employee.department,
                        hireDate = employee.hireDate.toString(),
                        salaryType = employee.salaryType,
                        salaryAmount = employee.salaryAmount,
                        salaryRate = employee.salaryRate,
                        isActive = employee.isActive,
                        createdAt = employee.createdAt,
                        updatedAt = employee.updatedAt,
                    )
                }
                call.respond(employees)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
                val principal = call.principal<UserPrincipal>()!!

                val employee = employeeRepository.findById(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                // Admin/Manager can view any employee.
                if (!principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@get
                }

                val user = userRepository.findById(employee.userId)

                call.respond(mapOf(
                    "id" to employee.id,
                    "userId" to employee.userId,
                    "fullName" to (user?.fullName ?: ""),
                    "email" to (user?.email ?: ""),
                    "position" to employee.position,
                    "department" to employee.department,
                    "hireDate" to employee.hireDate.toString(),
                    "salaryType" to employee.salaryType,
                    "salaryAmount" to employee.salaryAmount,
                    "isActive" to employee.isActive
                ))
            }

            // New function to get employees by department
            get("/department/{departmentName}") {
                val departmentName = call.parameters["departmentName"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing department name")
                val principal = call.principal<UserPrincipal>()!!

                if (!principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@get
                }

                val employees = employeeRepository.findByDepartment(departmentName).map { employee ->
                    val user = userRepository.findById(employee.userId)
                    mapOf(
                        "id" to employee.id,
                        "userId" to employee.userId,
                        "fullName" to (user?.fullName ?: ""),
                        "email" to (user?.email ?: ""),
                        "position" to employee.position,
                        "department" to employee.department,
                        "hireDate" to employee.hireDate.toString(),
                        "salaryType" to employee.salaryType,
                        "salaryAmount" to employee.salaryAmount,
                        "isActive" to employee.isActive
                    )
                }
                call.respond(employees)
            }
        }
    }
}