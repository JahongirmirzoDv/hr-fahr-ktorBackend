package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.SalaryCalculationDTO
import com.fahr.hrplatform.models.SalaryType
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.SalaryRepository
import com.fahr.hrplatform.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.salaryRoutes() {
    authenticate("auth-jwt") {
        route("/salaries") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""
                val userEmail = principal?.payload?.getClaim("email")?.asString() ?: ""

                val employeeId = call.request.queryParameters["employeeId"]

                val salaryRepository = SalaryRepository()
                val userRepository = UserRepository()
                val employeeRepository = EmployeeRepository()

                // If user is an employee, they can only see their own salary records
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
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You can only access your own salary records"))
                        return@get
                    }

                    val salaryRecords = salaryRepository.findByEmployee(userEmployee.id)
                    call.respond(salaryRecords)
                } else {
                    // Admin and managers can see all salary records or filter by employee
                    val salaryRecords = if (employeeId != null) {
                        salaryRepository.findByEmployee(employeeId)
                    } else {
                        salaryRepository.findAll()
                    }

                    call.respond(salaryRecords)
                }
            }

            post("/calculate") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin" && role != "manager") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins and managers can calculate salaries"))
                    return@post
                }

                val salaryCalcDTO = call.receive<SalaryCalculationDTO>()

                // Validate that employee exists
                val employeeRepository = EmployeeRepository()
                val employee = employeeRepository.findById(salaryCalcDTO.employeeId)
                if (employee == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee not found"))
                    return@post
                }

                val attendanceRepository = AttendanceRepository()
                val salaryRepository = SalaryRepository()

                // Check if salary record already exists for this period
                val existingSalary = salaryRepository.findByEmployeeAndPeriod(
                    salaryCalcDTO.employeeId,
                    salaryCalcDTO.periodStart,
                    salaryCalcDTO.periodEnd
                )

                if (existingSalary != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Salary record already exists for this period"))
                    return@post
                }

                // Get attendance records for the period
                val attendanceRecords = attendanceRepository.findByEmployeeAndDateRange(
                    salaryCalcDTO.employeeId,
                    salaryCalcDTO.periodStart,
                    salaryCalcDTO.periodEnd
                )

                // Calculate base salary based on salary type and attendance
                val presentDays = attendanceRecords.count { it.status == "PRESENT" }
                val halfDays = attendanceRecords.count { it.status == "HALF_DAY" }
                val totalDays = salaryCalcDTO.periodEnd.toEpochDay() - salaryCalcDTO.periodStart.toEpochDay() + 1

                val baseAmount = when (employee.salaryType) {
                    SalaryType.MONTHLY -> employee.salaryAmount * (presentDays + (halfDays * 0.5)) / totalDays
                    SalaryType.DAILY -> employee.salaryAmount * (presentDays + (halfDays * 0.5))
                    SalaryType.HOURLY -> {
                        // Sum up hours worked from attendance records
                        val totalHours = attendanceRecords
                            .filter { it.checkIn != null && it.checkOut != null }
                            .sumOf { 
                                val checkIn = it.checkIn!!.toSecondOfDay()
                                val checkOut = it.checkOut!!.toSecondOfDay()
                                (checkOut - checkIn) / 3600.0 // Convert seconds to hours
                            }
                        employee.salaryAmount * totalHours
                    }
                }

                // Calculate net amount
                val bonus = salaryCalcDTO.bonus ?: 0.0
                val deductions = salaryCalcDTO.deductions ?: 0.0
                val netAmount = baseAmount + bonus - deductions

                // Create salary record
                val salaryRecord = salaryRepository.create(
                    employeeId = salaryCalcDTO.employeeId,
                    periodStart = salaryCalcDTO.periodStart,
                    periodEnd = salaryCalcDTO.periodEnd,
                    baseAmount = baseAmount,
                    bonus = bonus,
                    deductions = deductions,
                    netAmount = netAmount
                )

                call.respond(HttpStatusCode.Created, salaryRecord)
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

                val salaryRepository = SalaryRepository()
                val salaryRecord = salaryRepository.findById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Salary record not found"))
                    return@get
                }

                // If user is an employee, they can only see their own salary records
                if (role == "employee") {
                    val userRepository = UserRepository()
                    val employeeRepository = EmployeeRepository()

                    val user = userRepository.findByEmail(userEmail) ?: run {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found"))
                        return@get
                    }

                    val userEmployee = employeeRepository.findByUserId(user.id) ?: run {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Employee record not found"))
                        return@get
                    }

                    if (salaryRecord.employeeId != userEmployee.id) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You can only access your own salary records"))
                        return@get
                    }
                }

                call.respond(salaryRecord)
            }
        }
    }
}
