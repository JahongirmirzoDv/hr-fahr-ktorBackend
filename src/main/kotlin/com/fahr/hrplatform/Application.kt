package com.fahr.hrplatform

import com.fahr.hrplatform.auth.authRoutes
import com.fahr.hrplatform.config.DatabaseFactory
import com.fahr.hrplatform.models.AttendanceTable
import com.fahr.hrplatform.models.EmployeeTable
import com.fahr.hrplatform.models.ProjectAssignmentTable
import com.fahr.hrplatform.models.ProjectTable
import com.fahr.hrplatform.models.SalaryRecordTable
import com.fahr.hrplatform.models.UserTable

import com.fahr.hrplatform.routes.*
import com.fahr.hrplatform.security.JwtConfig
import com.fahr.hrplatform.security.configureSecurity
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.EngineMain
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    //EngineMain.main(args)
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
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
            SalaryRecordTable
        )
    }


    routing {
        authRoutes()
        userRoutes()
        employeeRoutes()
        attendanceRoutes()
        salaryRoutes()
        projectRoutes()
    }
}
