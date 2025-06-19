package com.fahr.hrplatform.utils

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.EmployeeTable
import com.fahr.hrplatform.services.FaceRecognitionService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object DatabaseCleanup {

    suspend fun cleanupInvalidFaceEmbeddings() {
        val faceRecognitionService = FaceRecognitionService()

        dbQuery {
            val employeesWithInvalidEmbeddings = EmployeeTable
                .select { EmployeeTable.faceEmbedding.isNotNull() }
                .mapNotNull { row ->
                    val id = row[EmployeeTable.id].toString()
                    val embedding = row[EmployeeTable.faceEmbedding]

                    if (embedding != null) {
                        try {
                            // Try to decode the embedding
                            faceRecognitionService.base64ToEmbedding(embedding)
                            null // Valid embedding
                        } catch (e: Exception) {
                            println("Found invalid face embedding for employee $id: ${e.message}")
                            id // Invalid embedding
                        }
                    } else {
                        null
                    }
                }

            // Clear invalid embeddings
            if (employeesWithInvalidEmbeddings.isNotEmpty()) {
                println("Clearing ${employeesWithInvalidEmbeddings.size} invalid face embeddings...")

                EmployeeTable.update({ EmployeeTable.id inList employeesWithInvalidEmbeddings.map { UUID.fromString(it) } }) {
                    it[faceEmbedding] = null
                }

                println("Cleanup completed. Affected employees: ${employeesWithInvalidEmbeddings.joinToString(", ")}")
            } else {
                println("No invalid face embeddings found.")
            }
        }
    }
}