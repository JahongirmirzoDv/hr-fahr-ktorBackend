package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.EmployeeDTO
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.UserRepository
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.time.LocalDateTime

fun Route.employeeRoutes() {
    val employeeRepository: EmployeeRepository by inject()
    val userRepository: UserRepository by inject()

    authenticate("auth-jwt") {
        route("/employees") {
            post {
                val principal = call.principal<UserPrincipal>()
                // As per your request, only ADMIN can create/update employees
                if (principal == null || !principal.requireRole(Role.ADMIN)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
                    return@post
                }

                val employeeDTO = call.receive<EmployeeDTO>()
                if (userRepository.findById(employeeDTO.userId) == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User not found"))
                    return@post
                }

                if (employeeRepository.findByUserId(employeeDTO.userId) != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Employee record already exists for this user"))
                    return@post
                }

                val employee = employeeRepository.create(
                    userId = employeeDTO.userId,
                    position = employeeDTO.position,
                    department = employeeDTO.department,
                    hireDate = employeeDTO.hireDate ?: DateUtil.datetimeInUtc,
                    salaryType = employeeDTO.salaryType,
                    salaryAmount = employeeDTO.salaryAmount,
                    isActive = employeeDTO.isActive,
                    name = employeeDTO.name,
                    photoUrl = employeeDTO.photoUrl ?: "",
                    salaryRate = employeeDTO.salaryRate ?: 0.0
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