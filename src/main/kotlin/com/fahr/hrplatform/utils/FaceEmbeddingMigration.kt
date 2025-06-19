package com.fahr.hrplatform.utils

import com.fahr.hrplatform.config.DatabaseFactory.dbQuery
import com.fahr.hrplatform.models.EmployeeTable
import com.fahr.hrplatform.services.FaceRecognitionService
import org.jetbrains.exposed.sql.*
import java.util.*

object FaceEmbeddingMigration {

    suspend fun fixCorruptedEmbeddings() {
        val faceRecognitionService = FaceRecognitionService()
        var fixedCount = 0
        var clearedCount = 0

        dbQuery {
            val employees = EmployeeTable
                .select { EmployeeTable.faceEmbedding.isNotNull() }
                .mapNotNull { row ->
                    val id = row[EmployeeTable.id]
                    val embedding = row[EmployeeTable.faceEmbedding]
                    
                    if (embedding != null) {
                        try {
                            // Try to decode the embedding
                            faceRecognitionService.base64ToEmbedding(embedding)
                            null // Valid embedding, no action needed
                        } catch (e: Exception) {
                            println("Found corrupted face embedding for employee $id: ${e.message}")
                            Pair(id, embedding)
                        }
                    } else {
                        null
                    }
                }

            employees.forEach { (employeeId, corruptedEmbedding) ->
                try {
                    // Try to clean and fix the Base64 string
                    val cleanedBase64 = corruptedEmbedding.trim().replace("\\s".toRegex(), "")
                    val paddedBase64 = when (cleanedBase64.length % 4) {
                        0 -> cleanedBase64
                        2 -> cleanedBase64 + "=="
                        3 -> cleanedBase64 + "="
                        else -> null
                    }

                    if (paddedBase64 != null) {
                        // Try to decode the cleaned version
                        faceRecognitionService.base64ToEmbedding(paddedBase64)
                        
                        // If successful, update the database with the cleaned version
                        EmployeeTable.update({ EmployeeTable.id eq employeeId }) {
                            it[faceEmbedding] = paddedBase64
                        }
                        
                        fixedCount++
                        println("Fixed face embedding for employee $employeeId")
                    } else {
                        throw Exception("Cannot fix corrupted embedding")
                    }
                } catch (e: Exception) {
                    // If we can't fix it, clear the embedding so the user can re-register
                    EmployeeTable.update({ EmployeeTable.id eq employeeId }) {
                        it[faceEmbedding] = null
                    }
                    
                    clearedCount++
                    println("Cleared corrupted face embedding for employee $employeeId - user needs to re-register")
                }
            }

            println("Face embedding migration completed:")
            println("- Fixed embeddings: $fixedCount")
            println("- Cleared embeddings: $clearedCount")
            println("- Total processed: ${fixedCount + clearedCount}")
        }
    }
}