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
import com.fahr.hrplatform.services.SalaryService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.salaryRoutes() {
    val salaryRepository: SalaryRepository by inject()
    val employeeRepository: EmployeeRepository by inject()
    val salaryService: SalaryService by inject()

    authenticate("auth-jwt") {
        route("/salaries") {
            get {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@get
                }

                val employeeId = call.request.queryParameters["employeeId"]
                val salaryRecords = if (employeeId != null) {
                    salaryRepository.findByEmployee(employeeId)
                } else {
                    salaryRepository.findAll()
                }
                call.respond(salaryRecords)
            }

            get("/history/{employeeId}") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Manager role required"))
                    return@get
                }

                val employeeId = call.parameters["employeeId"]
                if (employeeId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing employeeId parameter"))
                    return@get
                }

                val salaryRecords = salaryRepository.findByEmployee(employeeId)
                call.respond(salaryRecords)
            }

            post("/calculate") {
                val principal = call.principal<UserPrincipal>()
                if (principal == null || !principal.requireRole(Role.ADMIN)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
                    return@post
                }

                val salaryCalcDTO = call.receive<SalaryCalculationDTO>()
                val employee = employeeRepository.findById(salaryCalcDTO.employeeId)
                if (employee == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee not found"))
                    return@post
                }

                if (salaryRepository.findByEmployeeAndPeriod(
                        salaryCalcDTO.employeeId,
                        salaryCalcDTO.periodStart,
                        salaryCalcDTO.periodEnd
                    ) != null
                ) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Salary record already exists for this period"))
                    return@post
                }

                val baseAmount = salaryService.calculateSalary(salaryCalcDTO) // Use the service
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

                call.respond(HttpStatusCode.Created, salaryRecord.toString())
            }
        }
    }
}