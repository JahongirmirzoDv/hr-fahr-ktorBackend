package com.fahr.hrplatform.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.stringParam
import java.util.*

object AttendanceEntity : Table("attendances") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val employeeId = text("employee_id")
    val checkInTime = varchar("check_in_time", 50)
    val gpsLat = double("gps_lat")
    val gpsLng = double("gps_lng")
    val nfcCardId = varchar("nfc_card_id", 100)
    val photoFileName = varchar("photo_file_name", 255).nullable()
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}
