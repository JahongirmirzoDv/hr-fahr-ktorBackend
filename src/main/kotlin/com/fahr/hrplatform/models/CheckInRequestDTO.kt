package com.fahr.hrplatform.models

import kotlinx.serialization.Serializable

@Serializable
data class CheckInRequestDTO(
    val employeeId: String,
    val checkInTime: String,
    val gpsLat: Double,
    val gpsLng: Double,
    val nfcCardId: String,
    val photoFileName: String? = null,
)

enum class AttendanceStatus {
    PENDING, APPROVED, REJECTED
}
