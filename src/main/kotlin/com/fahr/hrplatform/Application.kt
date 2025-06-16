package com.fahr.hrplatform


import com.fahr.hrplatform.auth.authRoutes
import com.fahr.hrplatform.config.DatabaseFactory
import com.fahr.hrplatform.config.appModule
import com.fahr.hrplatform.models.*
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.SalaryRepository
import com.fahr.hrplatform.routes.*
import com.fahr.hrplatform.security.configureSecurity
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.GlobalContext.startKoin

fun main(args: Array<String>) {
    //EngineMain.main(args)
//    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
//    }.start(wait = true)
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    startKoin {
        modules(appModule)
    }

    configureSecurity()
    DatabaseFactory.init(environment.config)
    configureSerialization()


    transaction {
        SchemaUtils.create(
            UserTable,
            EmployeeTable,
            ProjectTable,
            ProjectAssignmentTable,
            AttendanceTable,
            SalaryRecordTable,
            AttendanceEntity
        )
    }

    install(CORS) {
        // This allows your frontend running on localhost:8081 to make requests
        allowHost("localhost:8081")

        // You can also use allowHost("your-domain.com") in production

        // Allow common methods
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        // Allow essential headers
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        // You can add any other custom headers your frontend sends
        // allowHeader("My-Custom-Header")

        // If your frontend needs to send credentials like cookies
        // allowCredentials = true
    }


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