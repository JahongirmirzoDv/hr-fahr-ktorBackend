package com.fahr.hrplatform.services

import com.fahr.hrplatform.models.SalaryCalculationDTO
import com.fahr.hrplatform.models.SalaryType
import com.fahr.hrplatform.repository.AttendanceRepository
import com.fahr.hrplatform.repository.EmployeeRepository

class SalaryService(
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository
) {
    suspend fun calculateSalary(salaryCalcDTO: SalaryCalculationDTO): Double {
        val employee = employeeRepository.findById(salaryCalcDTO.employeeId)
            ?: throw IllegalArgumentException("Employee not found")

        val attendanceRecords = attendanceRepository.findByEmployeeAndDateRange(
            salaryCalcDTO.employeeId,
            salaryCalcDTO.periodStart,
            salaryCalcDTO.periodEnd
        )
        val presentDays = attendanceRecords.count { it.status == "PRESENT" }
        val halfDays = attendanceRecords.count { it.status == "HALF_DAY" }
        val totalDays = salaryCalcDTO.periodEnd.toEpochDays() - salaryCalcDTO.periodStart.toEpochDays() + 1

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
        return baseAmount
    }
}