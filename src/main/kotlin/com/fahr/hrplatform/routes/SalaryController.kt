package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.SalaryCalculationDTO
import com.fahr.hrplatform.models.SalaryType
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
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
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@get
                }

                val employeeId = call.request.queryParameters["employeeId"]
                val salaryRepository = SalaryRepository()
                val salaryRecords = if (employeeId != null) {
                    salaryRepository.findByEmployee(employeeId)
                } else {
                    salaryRepository.findAll()
                }
                call.respond(salaryRecords)
            }

            post("/calculate") {
                val principal = call.principal<UserPrincipal>()
                // As per your request, only ADMIN can calculate salary
                if (principal == null || !principal.requireRole(Role.ADMIN)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
                    return@post
                }

                val salaryCalcDTO = call.receive<SalaryCalculationDTO>()
                val employeeRepository = EmployeeRepository()
                val employee = employeeRepository.findById(salaryCalcDTO.employeeId)
                if (employee == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee not found"))
                    return@post
                }

                val salaryRepository = SalaryRepository()
                if (salaryRepository.findByEmployeeAndPeriod(salaryCalcDTO.employeeId, salaryCalcDTO.periodStart, salaryCalcDTO.periodEnd) != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Salary record already exists for this period"))
                    return@post
                }

                val attendanceRepository = AttendanceRepository()
                val attendanceRecords = attendanceRepository.findByEmployeeAndDateRange(salaryCalcDTO.employeeId, salaryCalcDTO.periodStart, salaryCalcDTO.periodEnd)
                val presentDays = attendanceRecords.count { it.status == "PRESENT" }
                val halfDays = attendanceRecords.count { it.status == "HALF_DAY" }
                val totalDays = salaryCalcDTO.periodEnd.toEpochDay() - salaryCalcDTO.periodStart.toEpochDay() + 1

                val baseAmount = when (employee.salaryType) {
                    SalaryType.MONTHLY -> employee.salaryAmount * (presentDays + (halfDays * 0.5)) / totalDays
                    SalaryType.DAILY -> employee.salaryAmount * (presentDays + (halfDays * 0.5))
                    SalaryType.HOURLY -> {
                        val totalHours = attendanceRecords
                            .filter { it.checkIn != null && it.checkOut != null }
                            .sumOf {
                                val checkIn = it.checkIn!!.toSecondOfDay()
                                val checkOut = it.checkOut!!.toSecondOfDay()
                                (checkOut - checkIn) / 3600.0
                            }
                        employee.salaryAmount * totalHours
                    }
                }

                val bonus = salaryCalcDTO.bonus ?: 0.0
                val deductions = salaryCalcDTO.deductions ?: 0.0
                val netAmount = baseAmount + bonus - deductions

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
        }
    }
}
