package com.fahr.hrplatform.utils

import kotlinx.datetime.*

/**
 * A utility object to get current date and time information.
 * Each property is computed on access to ensure the value is always current.
 */
object DateUtil {

    // Gets the current local date in the UTC time zone.
    // e.g., 2025-06-16
    val dateInUtc: LocalDate
        get() = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

    // Gets the current full date and time in the UTC time zone.
    // e.g., 2025-06-16T04:50:30.123
    val datetimeInUtc: LocalDateTime
        get() = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    // Gets the current full date and time in the system's default time zone.
    // For you in Tashkent, this would be UTC+5.
    // e.g., 2025-06-16T09:50:30.123
    val datetimeInSystemZone: LocalDateTime
        get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    // Gets the date for the first day of the current month in UTC.
    // e.g., If today is 2025-06-16, this will return 2025-06-01.
    val firstDayOfCurrentMonth: LocalDate
        get() {
            val today = dateInUtc
            // CORRECT WAY: Construct a new date with the day set to 1.
            return LocalDate(today.year, today.month, 1)
        }

    // Gets the date for the last day of the current month in UTC.
    val lastDayOfCurrentMonth: LocalDate
        get() {
            val today = dateInUtc
            // CORRECT WAY: Go to the 1st of this month, add 1 month, then subtract 1 day.
            return LocalDate(today.year, today.month, 1)
                .plus(1, DateTimeUnit.MONTH)
                .minus(1, DateTimeUnit.DAY)
        }

    val year: Int
        get() = dateInUtc.year

    val month: Int
        get() = dateInUtc.monthNumber

    fun yearMonth(dayOfMonth: Int, lastDayOfMonth: Boolean = false): LocalDate {
        return if (lastDayOfMonth) {
            LocalDate(year, month, lastDayOfCurrentMonth.dayOfMonth)
        } else {
            LocalDate(year, month, dayOfMonth)
        }
    }
}