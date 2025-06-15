package com.fahr.hrplatform.config

import com.fahr.hrplatform.repository.*
import com.fahr.hrplatform.services.SalaryService
import org.koin.dsl.module

val appModule = module {
    single { UserRepository() }
    single { EmployeeRepository() }
    single { ProjectRepository() }
    single { ProjectAssignmentRepository() }
    single { AttendanceRepository() }
    single { SalaryRepository() }
    single { ReportRepository() }
    single { SalaryService(get(), get()) } // Add this line
}