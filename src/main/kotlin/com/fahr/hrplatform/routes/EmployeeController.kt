package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.EmployeeDTO
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime

fun Route.employeeRoutes() {
    authenticate("auth-jwt") {
        route("/employees") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin" && role != "manager") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins and managers can create employees"))
                    return@post
                }

                val employeeDTO = call.receive<EmployeeDTO>()

                // Validate that the user exists
                val userRepository = UserRepository()
                val user = userRepository.findById(employeeDTO.userId)
                if (user == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User not found"))
                    return@post
                }

                val employeeRepository = EmployeeRepository()

                // Check if employee already exists for this user
                val existingEmployee = employeeRepository.findByUserId(employeeDTO.userId)
                if (existingEmployee != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Employee record already exists for this user"))
                    return@post
                }

                val hireDate = employeeDTO.hireDate ?: LocalDateTime.now()

                val employee = employeeRepository.create(
                    userId = employeeDTO.userId,
                    position = employeeDTO.position,
                    department = employeeDTO.department,
                    hireDate = hireDate,
                    salaryType = employeeDTO.salaryType,
                    salaryAmount = employeeDTO.salaryAmount,
                    isActive = employeeDTO.isActive
                )

                call.respond(HttpStatusCode.Created, mapOf(
                    "id" to employee.id,
                    "userId" to employee.userId,
                    "position" to employee.position,
                    "department" to employee.department,
                    "salaryType" to employee.salaryType,
                    "salaryAmount" to employee.salaryAmount
                ))
            }

            get {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin" && role != "manager") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins and managers can view all employees"))
                    return@get
                }

                val employeeRepository = EmployeeRepository()
                val userRepository = UserRepository()

                val employees = employeeRepository.findAll().map { employee ->
                    val user = userRepository.findById(employee.userId)
                    mapOf(
                        "id" to employee.id,
                        "userId" to employee.userId,
                        "fullName" to (user?.fullName ?: ""),
                        "email" to (user?.email ?: ""),
                        "position" to employee.position,
                        "department" to employee.department,
                        "hireDate" to employee.hireDate,
                        "salaryType" to employee.salaryType,
                        "salaryAmount" to employee.salaryAmount,
                        "isActive" to employee.isActive
                    )
                }

                call.respond(employees)
            }

            get("/{id}") {
                val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id parameter"))
                    return@get
                }

                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""
                val userEmail = principal?.payload?.getClaim("email")?.asString() ?: ""

                val employeeRepository = EmployeeRepository()
                val employee = employeeRepository.findById(id)

                if (employee == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee not found"))
                    return@get
                }

                val userRepository = UserRepository()
                val user = userRepository.findById(employee.userId)

                // Check if the user is requesting their own data or is an admin/manager
                if (role == "employee" && userEmail != (user?.email ?: "")) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You can only access your own employee information"))
                    return@get
                }

                call.respond(mapOf(
                    "id" to employee.id,
                    "userId" to employee.userId,
                    "fullName" to (user?.fullName ?: ""),
                    "email" to (user?.email ?: ""),
                    "position" to employee.position,
                    "department" to employee.department,
                    "hireDate" to employee.hireDate,
                    "salaryType" to employee.salaryType,
                    "salaryAmount" to employee.salaryAmount,
                    "isActive" to employee.isActive
                ))
            }
        }
    }
}
