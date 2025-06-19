package com.fahr.hrplatform.models.faceRecognition

import kotlinx.serialization.Serializable

@Serializable
data class FaceRecognitionRequest(
    val image: String, // Base64 encoded image
    val userId: String? = null
)

@Serializable
data class FaceRecognitionResponse(
    val success: Boolean,
    val message: String,
    val confidence: Double? = null,
    val userId: String? = null,
    val faces: List<DetectedFace> = emptyList()
)

@Serializable
data class DetectedFace(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val confidence: Double
)

@Serializable
data class RegisterFaceRequest(
    val userId: String,
    val userName: String,
    val image: String // Base64 encoded image
)

@Serializable
data class RegisterFaceResponse(
    val success: Boolean,
    val message: String,
    val userId: String? = null
)

@Serializable
data class User(
    val userId: String,
    val userName: String,
    val faceEncoding: String // Base64 encoded face features
)