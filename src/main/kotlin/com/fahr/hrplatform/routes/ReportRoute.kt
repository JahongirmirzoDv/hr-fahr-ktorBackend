package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.MonthlyReport
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.ReportRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.time.YearMonth

fun Route.reportRoutes() {
    authenticate("auth-jwt") {
        route("/reports") {
            get("/monthly") {
                val principal = call.principal<UserPrincipal>()
                // Updated role check
                if (principal == null || !principal.requireRole(Role.ADMIN)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin or Accountant role required"))
                    return@get
                }

                val year = call.request.queryParameters["year"]?.toIntOrNull() ?: LocalDate.now().year
                val month = call.request.queryParameters["month"]?.toIntOrNull() ?: LocalDate.now().monthValue

                val yearMonth = YearMonth.of(year, month)
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()

                val reportRepository = ReportRepository()
                val summaries = reportRepository.getMonthlySummary(startDate, endDate)

                val report = MonthlyReport(
                    reportDate = LocalDate.now().toString(),
                    period = yearMonth.toString(),
                    summaries = summaries
                )

                call.respond(report)
            }
        }
    }
}