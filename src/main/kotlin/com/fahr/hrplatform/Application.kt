package com.fahr.hrplatform


import com.fahr.hrplatform.config.DatabaseFactory
import com.fahr.hrplatform.config.appModule
import com.fahr.hrplatform.models.*
import com.fahr.hrplatform.security.configureSecurity
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
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
    configureSerialization()


    DatabaseFactory.init(environment.config)
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

    configureRouting()
}