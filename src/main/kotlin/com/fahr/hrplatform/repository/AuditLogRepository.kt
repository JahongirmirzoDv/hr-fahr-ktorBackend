package com.fahr.hrplatform.repository

import com.fahr.hrplatform.models.AuditLogEntity
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object AuditLogRepository {
    fun log(action: String, actorId: String, details: String) {
        transaction {
            AuditLogEntity.insert {
                it[id] = UUID.randomUUID()
                it[AuditLogEntity.action] = action
                it[AuditLogEntity.actorId] = actorId
                it[timestamp] = Clock.System.now().toString()
                it[AuditLogEntity.details] = details
            }
        }
    }
}
