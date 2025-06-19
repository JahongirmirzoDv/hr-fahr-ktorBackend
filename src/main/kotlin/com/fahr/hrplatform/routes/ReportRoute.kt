package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.MonthlyReport
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.ReportRepository
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import org.koin.ktor.ext.inject

fun Route.reportRoutes() {
    val reportRepository: ReportRepository by inject()

    authenticate("auth-jwt") {
        route("/reports") {
            get("/monthly") {
                val principal = call.principal<UserPrincipal>()
                // Updated role check
                if (principal == null || !principal.requireRole(Role.ADMIN)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Accountant role required"))
                    return@get
                }

                val year = call.request.queryParameters["year"]?.toIntOrNull() ?: DateUtil.year
                val month = call.request.queryParameters["month"]?.toIntOrNull() ?: DateUtil.month


                val summaries = reportRepository.getMonthlySummary(
                    DateUtil.yearMonth(1),
                    DateUtil.yearMonth(1, lastDayOfMonth = true)
                )

                val report = MonthlyReport(
                    reportDate = DateUtil.datetimeInUtc.toString(),
                    period = "$year-$month",
                    summaries = summaries
                )

                call.respond(report)
            }

            get("/department/{dept") {}

            get("/employee/{id}") { }

            get("/attendance") { }
        }
    }
}