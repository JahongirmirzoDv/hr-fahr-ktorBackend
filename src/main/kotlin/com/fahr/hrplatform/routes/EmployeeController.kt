package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.*
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.UserRepository
import com.fahr.hrplatform.services.FaceRecognitionService
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

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

                var employeeDataMap = mutableMapOf<String, String>()
                var photoBytes: ByteArray? = null

                try {
                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                part.name?.let { name ->
                                    employeeDataMap[name] = part.value
                                }
                            }

                            is PartData.FileItem -> {
                                if (part.name == "photo") {
                                    photoBytes = part.streamProvider().readBytes()
                                }
                            }

                            else -> {
                                // Ignore other part types
                            }
                        }
                        part.dispose()
                    }

                    // Construct EmployeeDTO from the collected form data
                    val employeeDTO = try {
                        EmployeeDTO(
                            userId = employeeDataMap["userId"] ?: throw IllegalArgumentException("userId is missing"),
                            name = employeeDataMap["name"] ?: throw IllegalArgumentException("name is missing"),
                            position = employeeDataMap["position"]
                                ?: throw IllegalArgumentException("position is missing"),
                            department = employeeDataMap["department"]
                                ?: throw IllegalArgumentException("department is missing"),
                            salaryType = SalaryType.valueOf(employeeDataMap["salaryType"] ?: ""),
                            salaryAmount = employeeDataMap["salaryAmount"]?.toDouble()
                                ?: throw IllegalArgumentException("salaryAmount is missing"),
                            salaryRate = employeeDataMap["salaryRate"]?.toDouble(),
                            isActive = employeeDataMap["isActive"]?.toBoolean()
                                ?: throw IllegalArgumentException("isActive is missing or invalid"),
                        )
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid employee data: ${e.message}"))
                        return@post
                    }

                    if (photoBytes == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing photo file"))
                        return@post
                    }

                    // Validate image size and format
                    if (photoBytes!!.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Photo file is empty"))
                        return@post
                    }

                    // Validate that the image contains a valid face (using the updated method)
                    val isValidImage = try {
                        faceRecognitionService.validateImageBytes(photoBytes!!)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid image format: ${e.message}"))
                        return@post
                    }

                    if (!isValidImage) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "No valid face detected in the uploaded image or image is too small")
                        )
                        return@post
                    }

                    // Generate face embedding using the updated method
                    val faceEmbedding = try {
                        faceRecognitionService.generateEmbedding(photoBytes!!)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Face registration failed: ${e.message}")
                        )
                        return@post
                    }

                    // Convert embedding to base64 for storage
                    val embeddingBase64 = try {
                        faceRecognitionService.embeddingToBase64(faceEmbedding)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to encode face embedding: ${e.message}")
                        )
                        return@post
                    }

                    // Create the employee with the face embedding
                    val employee = try {
                        employeeRepository.create(
                            userId = employeeDTO.userId,
                            name = employeeDTO.name,
                            position = employeeDTO.position,
                            department = employeeDTO.department,
                            hireDate = employeeDTO.hireDate ?: DateUtil.datetimeInUtc,
                            salaryType = employeeDTO.salaryType,
                            salaryAmount = employeeDTO.salaryAmount,
                            salaryRate = employeeDTO.salaryRate ?: 0.0,
                            isActive = employeeDTO.isActive,
                            faceEmbedding = embeddingBase64
                        )
                    } catch (e: Exception) {
                        println("Error creating employee: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to create employee: ${e.message}")
                        )
                        return@post
                    }

                    if (employee == null) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to create employee - returned null")
                        )
                        return@post
                    }

                    // Log successful registration
                    println("Employee registered successfully:")
                    println("- ID: ${employee.id}")
                    println("- User ID: ${employee.userId}")
                    println("- Name: ${employee.name}")
                    println("- Position: ${employee.position}")
                    println("- Department: ${employee.department}")
                    println("- Face registered: Yes")
                    println("- Registered by: ${principal.userId}")
                    println("- Timestamp: ${DateUtil.datetimeInUtc}")

                    // Get user info for complete response
                    val user = userRepository.findById(employee.userId)

                    call.respond(
                        HttpStatusCode.Created,
                        mapOf(
                            "success" to true,
                            "message" to "Employee created successfully with face registration!",
                            "employee" to EmployeeResponse(
                                id = employee.id,
                                userId = employee.userId,
                                name = user?.fullName ?: employee.name,
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
                            ),
                            "registeredBy" to principal.userId,
                            "timestamp" to DateUtil.datetimeInUtc
                        )
                    )

                } catch (e: Exception) {
                    // Log the error for debugging
                    println("Error during employee registration:")
                    println("- Error message: ${e.message}")
                    println("- Admin user: ${principal?.userId}")
                    println("- Timestamp: ${DateUtil.datetimeInUtc}")
                    e.printStackTrace()

                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf(
                            "success" to false,
                            "error" to "Internal server error during employee registration: ${e.message}",
                            "timestamp" to DateUtil.datetimeInUtc
                        )
                    )
                }
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

                call.respond(
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
                        "isActive" to employee.isActive,
                        "faceEmbedding" to employee.faceEmbedding // Include face embedding info
                    )
                )
            }

            get("/department/{departmentName}") {
                val departmentName = call.parameters["departmentName"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing department name"
                )
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

            put("/{id}"){}

            delete("/{id}") {  }

        }
    }
}