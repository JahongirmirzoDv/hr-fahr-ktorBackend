package com.fahr.hrplatform.services

import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelZoo
import ai.djl.training.util.ProgressBar
import java.io.InputStream
import java.util.*
import kotlin.math.sqrt

class FaceRecognitionService {

    companion object {
        private const val EMBEDDING_SIZE = 512
        // You might need to adjust this threshold after testing with your own images.
        // Higher value = stricter matching.
        private const val SIMILARITY_THRESHOLD = 0.85
    }

    // --- THIS IS THE CORRECTED PART ---
    // We now load the model more explicitly by its group and artifact ID
    // from the DJL Model Zoo, which is more reliable.
    private val predictor: Predictor<Image, FloatArray> by lazy {
        val criteria = Criteria.builder()
            .setTypes(Image::class.java, FloatArray::class.java)
            .optGroupId("ai.djl.onnxruntime")
            .optArtifactId("face_feature")
            .optEngine("OnnxRuntime")
            .optProgress(ProgressBar())
            .build()
        val model = ModelZoo.loadModel(criteria)
        model.newPredictor()
    }

    /**
     * Generates a 512-dimension face embedding from an image input stream.
     */
    fun generateEmbedding(inputStream: InputStream): FloatArray {
        val image = ImageFactory.getInstance().fromInputStream(inputStream)
        return predictor.predict(image)
    }

    /**
     * Compares two face embeddings using cosine similarity.
     * @return true if the similarity is above the defined threshold.
     */
    fun areSimilar(embedding1: FloatArray, embedding2: FloatArray): Boolean {
        require(embedding1.size == EMBEDDING_SIZE && embedding2.size == EMBEDDING_SIZE) {
            "Embeddings must have the same size of $EMBEDDING_SIZE"
        }
        val similarity = cosineSimilarity(embedding1, embedding2)
        println("Calculated similarity: $similarity") // For debugging purposes
        return similarity >= SIMILARITY_THRESHOLD
    }

    /**
     * Calculates the cosine similarity between two vectors.
     * Result is between -1.0 (opposite) and 1.0 (identical).
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }
        val norm1 = sqrt(vec1.sumOf { (it * it).toDouble() })
        val norm2 = sqrt(vec2.sumOf { (it * it).toDouble() })
        return dotProduct / (norm1 * norm2)
    }

    /**
     * Converts a float array embedding to a Base64 string for database storage.
     */
    fun embeddingToBase64(embedding: FloatArray): String {
        val bytes = embedding.fold(java.nio.ByteBuffer.allocate(embedding.size * 4)) { buffer, value ->
            buffer.putFloat(value)
        }.array()
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * Converts a Base64 string from the database back to a float array embedding.
     */
    fun base64ToEmbedding(base64: String): FloatArray {
        val bytes = Base64.getDecoder().decode(base64)
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        val floatArray = FloatArray(bytes.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = buffer.getFloat()
        }
        return floatArray
    }
}