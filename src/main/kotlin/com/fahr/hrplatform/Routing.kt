// Create a new file: RouterConfig.kt
package com.fahr.hrplatform

import com.fahr.hrplatform.auth.authRoutes
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.SalaryRepository
import com.fahr.hrplatform.repository.UserRepository
import com.fahr.hrplatform.routes.*
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Public routes
        authRoutes()

        // Health check
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy", "timestamp" to System.currentTimeMillis()))
        }

        // API routes with version prefix
        authenticate("auth-jwt") {
            // Admin routes
            route("/admin") {
                intercept(ApplicationCallPipeline.Call) {
                    val principal = call.principal<UserPrincipal>()
                    if (principal == null || !principal.requireRole(Role.ADMIN)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                        finish()
                    }
                }
                userRoutes()
                salaryRoutes()
                reportRoutes()
                employeeRoutes()
                attendanceRoutes()
                projectRoutes()
            }

            // Manager routes
            route("/manager") {
                intercept(ApplicationCallPipeline.Call) {
                    val principal = call.principal<UserPrincipal>()
                    if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Manager access required"))
                        finish()
                    }
                }
                employeeRoutes()
                attendanceRoutes()
                projectRoutes()
            }

            // User routes
            route("/user") {
                intercept(ApplicationCallPipeline.Call) {
                    val principal = call.principal<UserPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                        finish()
                    }
                }

                get("/profile") {
                    val principal = call.principal<UserPrincipal>()!!
                    val userRepository = UserRepository()
                    val user = userRepository.findById(principal.userId.toString())

                    if (user != null) {
                        call.respond(
                            mapOf(
                                "id" to user.id,
                                "fullName" to user.fullName,
                                "email" to user.email,
                                "role" to user.role
                            )
                        )
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    }
                }

                get("/attendance") {
                    val principal = call.principal<UserPrincipal>()!!
                    val employeeRepository = EmployeeRepository()
                    val employee = employeeRepository.findByUserId(principal.userId.toString())

                    if (employee == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee record not found"))
                        return@get
                    }

                    val attendanceRepository = AttendanceRepository()
                    val records = attendanceRepository.findByEmployeeAndDateRange(
                        employee.id,
                        DateUtil.firstDayOfCurrentMonth,
                        DateUtil.dateInUtc
                    )
                    call.respond(mapOf("attendance" to records, "employee" to employee.name))
                }

                get("/salary") {
                    val principal = call.principal<UserPrincipal>()!!
                    val employeeRepository = EmployeeRepository()
                    val employee = employeeRepository.findByUserId(principal.userId.toString())

                    if (employee == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee record not found"))
                        return@get
                    }

                    val salaryRepository = SalaryRepository()
                    val records = salaryRepository.findByEmployee(employee.id)
                    call.respond(mapOf("salaryRecords" to records, "employee" to employee.name))
                }
            }
        }
    }
}