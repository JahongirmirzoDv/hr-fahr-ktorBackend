package com.fahr.hrplatform.utils

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val timestamp: String = DateUtil.datetimeInUtc.toString()
)

@Serializable
data class ErrorDetails(
    val code: String,
    val message: String,
    val field: String? = null
)

@Serializable
data class ValidationErrorResponse(
    val success: Boolean = false,
    val errors: List<ErrorDetails>,
    val timestamp: String = DateUtil.datetimeInUtc.toString()
)

@Serializable
data class PaginatedResponse<T>(
    val success: Boolean = true,
    val data: List<T>,
    val pagination: PaginationInfo,
    val timestamp: String = DateUtil.datetimeInUtc.toString()
)

@Serializable
data class PaginationInfo(
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalItems: Int
)

// Extension functions for consistent responses
fun <T> T.toApiResponse(success: Boolean = true): ApiResponse<T> {
    return ApiResponse(
        success = success,
        data = if (success) this else null,
        error = if (!success) this.toString() else null
    )
}

fun String.toErrorResponse(): ApiResponse<Nothing> {
    return ApiResponse(
        success = false,
        data = null,
        error = this
    )
}

fun <T> List<T>.toPaginatedResponse(page: Int, pageSize: Int, totalItems: Int): PaginatedResponse<T> {
    return PaginatedResponse(
        data = this,
        pagination = PaginationInfo(
            page = page,
            pageSize = pageSize,
            totalPages = if (totalItems > 0) (totalItems + pageSize - 1) / pageSize else 0,
            totalItems = totalItems
        )
    )
}