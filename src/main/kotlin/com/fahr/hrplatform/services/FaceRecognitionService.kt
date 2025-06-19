package com.fahr.hrplatform.services

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.*

class FaceRecognitionService {

    companion object {
        private const val EMBEDDING_SIZE = 512
        private const val SIMILARITY_THRESHOLD = 0.75 // Adjust based on your requirements
        private const val MIN_IMAGE_SIZE = 100
    }

    /**
     * Generates face embedding from photo bytes
     */
    fun generateEmbedding(photoBytes: ByteArray): FloatArray {
        return try {
            // Validate image
            if (!validateImageBytes(photoBytes)) {
                throw IllegalArgumentException("Invalid image format or size")
            }

            // Convert bytes to BufferedImage
            val inputStream = ByteArrayInputStream(photoBytes)
            val image = ImageIO.read(inputStream)
                ?: throw IllegalArgumentException("Cannot read image from provided bytes")

            // Extract face features
            extractFaceEmbedding(image)
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate face embedding: ${e.message}", e)
        }
    }

    /**
     * Converts base64 string back to face embedding array
     */
    fun base64ToEmbedding(base64String: String): FloatArray {
        return try {
            if (base64String.isBlank()) {
                throw IllegalArgumentException("Base64 string is empty")
            }

            val byteArray = Base64.getDecoder().decode(base64String)

            if (byteArray.size % 4 != 0) {
                throw IllegalArgumentException("Invalid embedding data length")
            }

            val embedding = FloatArray(byteArray.size / 4)

            for (i in embedding.indices) {
                val startIndex = i * 4
                val bits = ((byteArray[startIndex].toInt() and 0xFF) shl 24) or
                        ((byteArray[startIndex + 1].toInt() and 0xFF) shl 16) or
                        ((byteArray[startIndex + 2].toInt() and 0xFF) shl 8) or
                        (byteArray[startIndex + 3].toInt() and 0xFF)
                embedding[i] = Float.fromBits(bits)
            }

            embedding
        } catch (e: Exception) {
            throw RuntimeException("Failed to decode embedding from base64: ${e.message}", e)
        }
    }

    /**
     * Compares two face embeddings and determines if they represent the same person
     */
    fun areSimilar(embedding1: FloatArray, embedding2: FloatArray): Boolean {
        return try {
            if (embedding1.isEmpty() || embedding2.isEmpty()) {
                false
            } else {
                val similarity = calculateCosineSimilarity(embedding1, embedding2)
                println("Face similarity score: $similarity (threshold: $SIMILARITY_THRESHOLD)")
                similarity >= SIMILARITY_THRESHOLD
            }
        } catch (e: Exception) {
            println("Error comparing embeddings: ${e.message}")
            false
        }
    }

    /**
     * Gets the similarity score between two embeddings (0.0 to 1.0)
     */
    fun getSimilarityScore(embedding1: FloatArray, embedding2: FloatArray): Double {
        return if (embedding1.isEmpty() || embedding2.isEmpty()) {
            0.0
        } else {
            calculateCosineSimilarity(embedding1, embedding2)
        }
    }

    /**
     * Validates image bytes before processing
     */
     fun validateImageBytes(photoBytes: ByteArray): Boolean {
        return try {
            if (photoBytes.isEmpty()) return false

            val inputStream = ByteArrayInputStream(photoBytes)
            val image = ImageIO.read(inputStream) ?: return false

            // Check minimum image dimensions
            image.width >= MIN_IMAGE_SIZE && image.height >= MIN_IMAGE_SIZE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts face embedding from BufferedImage
     * This is a sophisticated simulation - replace with actual ML model in production
     */
    private fun extractFaceEmbedding(image: BufferedImage): FloatArray {
        val embedding = FloatArray(EMBEDDING_SIZE)

        // Enhanced feature extraction based on multiple image characteristics
        val features = analyzeImageFeatures(image)

        // Generate deterministic embedding based on image analysis
        val seed = generateSeed(features)
        val random = Random(seed)

        // Generate embedding with Gaussian distribution
        for (i in embedding.indices) {
            embedding[i] = random.nextGaussian().toFloat()
        }

        // Apply image-specific modifications
        applyImageSpecificFeatures(embedding, features)

        // Normalize to unit vector
        return normalizeEmbedding(embedding)
    }

    /**
     * Analyzes various features of the image for more realistic embedding generation
     */
    private fun analyzeImageFeatures(image: BufferedImage): ImageFeatures {
        val width = image.width
        val height = image.height
        val centerX = width / 2
        val centerY = height / 2

        // Analyze different regions of the image
        val faceRegion = analyzeRegion(image, centerX - 75, centerY - 75, 150, 150)
        val eyeRegion = analyzeRegion(image, centerX - 50, centerY - 30, 100, 20)
        val mouthRegion = analyzeRegion(image, centerX - 30, centerY + 20, 60, 25)

        // Calculate image statistics
        val brightness = calculateAverageBrightness(image)
        val contrast = calculateContrast(image)
        val edgeIntensity = calculateEdgeIntensity(image)

        return ImageFeatures(
            width = width,
            height = height,
            faceRegionHash = faceRegion,
            eyeRegionHash = eyeRegion,
            mouthRegionHash = mouthRegion,
            brightness = brightness,
            contrast = contrast,
            edgeIntensity = edgeIntensity
        )
    }

    /**
     * Analyzes a specific region of the image
     */
    private fun analyzeRegion(image: BufferedImage, startX: Int, startY: Int, width: Int, height: Int): Long {
        var pixelSum = 0L
        var pixelCount = 0
        val maxX = minOf(image.width, startX + width)
        val maxY = minOf(image.height, startY + height)
        val minX = maxOf(0, startX)
        val minY = maxOf(0, startY)

        for (y in minY until maxY) {
            for (x in minX until maxX) {
                pixelSum += image.getRGB(x, y).toLong()
                pixelCount++
            }
        }

        return if (pixelCount > 0) pixelSum / pixelCount else 0L
    }

    /**
     * Calculates average brightness of the image
     */
    private fun calculateAverageBrightness(image: BufferedImage): Double {
        var totalBrightness = 0.0
        val totalPixels = image.width * image.height

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                totalBrightness += (r + g + b) / 3.0
            }
        }

        return totalBrightness / totalPixels
    }

    /**
     * Calculates contrast of the image
     */
    private fun calculateContrast(image: BufferedImage): Double {
        val brightness = calculateAverageBrightness(image)
        var varianceSum = 0.0
        val totalPixels = image.width * image.height

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                val pixelBrightness = (r + g + b) / 3.0
                varianceSum += (pixelBrightness - brightness).pow(2)
            }
        }

        return sqrt(varianceSum / totalPixels)
    }

    /**
     * Calculates edge intensity using simple gradient
     */
    private fun calculateEdgeIntensity(image: BufferedImage): Double {
        var edgeSum = 0.0
        val width = image.width
        val height = image.height

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = getGrayValue(image.getRGB(x, y))
                val right = getGrayValue(image.getRGB(x + 1, y))
                val bottom = getGrayValue(image.getRGB(x, y + 1))

                val gradientX = abs(right - center)
                val gradientY = abs(bottom - center)
                edgeSum += sqrt((gradientX * gradientX + gradientY * gradientY).toDouble())
            }
        }

        return edgeSum / ((width - 2) * (height - 2))
    }

    /**
     * Converts RGB to grayscale value
     */
    private fun getGrayValue(rgb: Int): Int {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    /**
     * Generates a seed for random number generation based on image features
     */
    private fun generateSeed(features: ImageFeatures): Long {
        return features.faceRegionHash +
                features.eyeRegionHash * 31 +
                features.mouthRegionHash * 17 +
                (features.brightness * 1000).toLong() * 13 +
                (features.contrast * 1000).toLong() * 7 +
                (features.edgeIntensity * 1000).toLong() * 3
    }

    /**
     * Applies image-specific modifications to the embedding
     */
    private fun applyImageSpecificFeatures(embedding: FloatArray, features: ImageFeatures) {
        // Modify embedding based on brightness
        val brightnessMultiplier = (features.brightness / 128.0).toFloat()
        for (i in 0 until minOf(50, embedding.size)) {
            embedding[i] *= brightnessMultiplier
        }

        // Modify embedding based on contrast
        val contrastMultiplier = (features.contrast / 50.0).toFloat()
        for (i in 50 until minOf(100, embedding.size)) {
            embedding[i] *= contrastMultiplier
        }

        // Modify embedding based on edge intensity
        val edgeMultiplier = (features.edgeIntensity / 20.0).toFloat()
        for (i in 100 until minOf(150, embedding.size)) {
            embedding[i] *= edgeMultiplier
        }
    }

    /**
     * Calculates cosine similarity between two vectors
     */
    private fun calculateCosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        if (vec1.size != vec2.size) {
            throw IllegalArgumentException("Vectors must be the same size")
        }

        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            magnitude1 += vec1[i] * vec1[i]
            magnitude2 += vec2[i] * vec2[i]
        }

        magnitude1 = sqrt(magnitude1)
        magnitude2 = sqrt(magnitude2)

        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0.0
        }
    }

    /**
     * Normalizes embedding to unit vector
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        val magnitude = sqrt(embedding.map { it * it }.sum())
        return if (magnitude > 0) {
            embedding.map { it / magnitude }.toFloatArray()
        } else {
            embedding
        }
    }

    /**
     * Encodes face embedding to base64 string for storage
     */
    fun embeddingToBase64(embedding: FloatArray): String {
        val byteArray = embedding.flatMap { value ->
            val bits = value.toBits()
            listOf(
                (bits shr 24).toByte(),
                (bits shr 16).toByte(),
                (bits shr 8).toByte(),
                bits.toByte()
            )
        }.toByteArray()

        return Base64.getEncoder().encodeToString(byteArray)
    }

    /**
     * Data class to hold image analysis results
     */
    private data class ImageFeatures(
        val width: Int,
        val height: Int,
        val faceRegionHash: Long,
        val eyeRegionHash: Long,
        val mouthRegionHash: Long,
        val brightness: Double,
        val contrast: Double,
        val edgeIntensity: Double
    )
}