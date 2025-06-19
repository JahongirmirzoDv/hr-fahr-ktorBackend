package com.fahr.hrplatform.utils

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class ApiErrorResponse(
    val error: String
)

//@Serializable
//data class ApiResponse<T>(
//    val success: Boolean = true,
//    val data: T? = null,
//    val error: String? = null
//)

// In your HTTP client
suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> HttpResponse
): Result<T> {
    return try {
        val response = apiCall()
        when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    val data = response.body<T>()
                    Result.success(data)
                } catch (e: SerializationException) {
                    // Try to parse as error response
                    val errorResponse = response.body<ApiErrorResponse>()
                    Result.failure(Exception(errorResponse.error))
                }
            }
            else -> {
                val errorResponse = response.body<ApiErrorResponse>()
                Result.failure(Exception(errorResponse.error))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Usage example for salaries endpoint
//suspend fun getSalaries(): Result<List<SalaryRecord>> {
//    return safeApiCall<List<SalaryRecord>> {
//        httpClient.get("/admin/salaries")
//    }
//}