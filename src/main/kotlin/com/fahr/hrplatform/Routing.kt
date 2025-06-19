package com.fahr.hrplatform

import com.fahr.hrplatform.auth.authRoutes
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.SalaryRepository
import com.fahr.hrplatform.routes.attendanceRoutes
import com.fahr.hrplatform.routes.employeeRoutes
import com.fahr.hrplatform.routes.projectRoutes
import com.fahr.hrplatform.routes.reportRoutes
import com.fahr.hrplatform.routes.salaryRoutes
import com.fahr.hrplatform.routes.userRoutes
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Public authentication routes
        authRoutes()

        // Routes for ADMIN role
        route("/admin") {
            userRoutes()
            salaryRoutes()
            projectRoutes()
            reportRoutes()
            attendanceRoutes()
            employeeRoutes()
        }
        // Routes for MANAGER role
        route("/manager") {
            employeeRoutes()
            attendanceRoutes()
        }

        // --- NEW: Added dedicated routes for USER role ---
        authenticate("auth-jwt") {
            route("/user") {
                // Route for a user to get their own attendance
                get("/attendance") {
                    val principal = call.principal<UserPrincipal>()
                    if (principal == null || !principal.requireRole(Role.USER)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "User role required"))
                        return@get
                    }

                    val employeeRepository = EmployeeRepository()
                    val employee = employeeRepository.findByUserId(principal.userId.toString())
                    if (employee == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee record not found"))
                        return@get
                    }

                    val attendanceRepository = AttendanceRepository()
                    val records = attendanceRepository.findByEmployeeAndDateRange(
                        employee.id,
                        DateUtil.firstDayOfCurrentMonth, // Default to current month
                        DateUtil.dateInUtc
                    )
                    call.respond(records)
                }

                // Route for a user to get their own salary records
                get("/salary") {
                    val principal = call.principal<UserPrincipal>()
                    if (principal == null || !principal.requireRole(Role.USER)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "User role required"))
                        return@get
                    }

                    val employeeRepository = EmployeeRepository()
                    val employee = employeeRepository.findByUserId(principal.userId.toString())
                    if (employee == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee record not found"))
                        return@get
                    }

                    val salaryRepository = SalaryRepository()
                    val records = salaryRepository.findByEmployee(employee.id)
                    call.respond(records)
                }
            }
        }
    }
}
