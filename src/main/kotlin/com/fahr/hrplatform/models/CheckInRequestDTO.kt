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

fun CheckInRequestDTO.isValid(): Boolean {
    return employeeId.toString().isNotBlank() &&
            checkInTime.isNotBlank() &&
            nfcCardId.isNotBlank() &&
            gpsLat in -90.0..90.0 &&
            gpsLng in -180.0..180.0
}
