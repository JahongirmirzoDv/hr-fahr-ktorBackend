package com.fahr.hrplatform.models

import org.jetbrains.exposed.sql.Table

object AuditLogEntity : Table("audit_logs") {
    val id = uuid("id").autoGenerate()
    val action = varchar("action", 255)
    val actorId = text("actor_id")
    val timestamp = varchar("timestamp", 50)
    val details = text("details")
}
