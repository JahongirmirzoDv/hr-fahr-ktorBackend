package com.fahr.hrplatform

import com.fahr.hrplatform.auth.authRoutes
import com.fahr.hrplatform.config.DatabaseFactory
import com.fahr.hrplatform.models.AttendanceEntity
import com.fahr.hrplatform.models.AttendanceTable
import com.fahr.hrplatform.models.EmployeeTable
import com.fahr.hrplatform.models.ProjectAssignmentTable
import com.fahr.hrplatform.models.ProjectTable
import com.fahr.hrplatform.models.SalaryRecordTable
import com.fahr.hrplatform.models.UserTable

import com.fahr.hrplatform.routes.*
import com.fahr.hrplatform.security.JwtConfig
import com.fahr.hrplatform.security.configureSecurity
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.EngineMain
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    //EngineMain.main(args)
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            get("/") {
                call.respondText("Hello, world!")
            }
        }
    }.start(wait = true)
}

fun Application.module() {
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
        route("/admin"){
            userRoutes()
            salaryRoutes()
            projectRoutes()
            reportRoutes()
            attendanceRoutes()
            employeeRoutes()
        }
        route("/manager"){
            employeeRoutes()
            attendanceRoutes()
        }

        authRoutes()

    }
}
