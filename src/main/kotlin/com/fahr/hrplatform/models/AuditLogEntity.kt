package com.fahr.hrplatform.models

import com.fahr.hrplatform.utils.DateUtil
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object AuditLogEntity : Table("audit_logs") {
    val id = uuid("id").autoGenerate()
    val action = varchar("action", 255)
    val actorId = text("actor_id")
    val timestamp = varchar("timestamp", 50)
    val details = text("details")
}


// Notifications
object NotificationTable : UUIDTable("notifications") {
    val userId = reference("user_id", UserTable)
    val title = varchar("title", 255)
    val message = text("message")
    val type = varchar("type", 50) // INFO, WARNING, ERROR
    val isRead = bool("is_read").default(false)
    val createdAt = datetime("created_at").default(DateUtil.datetimeInUtc)
}