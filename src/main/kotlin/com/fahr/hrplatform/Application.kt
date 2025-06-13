package com.fahr.hrplatform

import com.fahr.hrplatform.auth.authRoutes
import com.fahr.hrplatform.auth.configureSecurity
import com.fahr.hrplatform.routes.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.routing

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    install(Authentication) { configureSecurity() }
    configureSerialization()

    routing {
        authRoutes()
        userRoutes()
        employeeRoutes()
        attendanceRoutes()
        salaryRoutes()
        projectRoutes()
    }
}
