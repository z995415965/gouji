// CardRecognizer.kt: OCR card recognition engine using PaddleOCR
package com.example.goujicardcounter.recognition

import android.graphics.Bitmap
import org.opencv.core.Mat
import org.opencv.android.Utils
import java.util.concurrent.ConcurrentHashMap

/**
 * Local OCR card recognizer
 * Uses PaddleOCR lite model for offline card detection
 * Filters results to only recognize playing card symbols
 */
class CardRecognizer {

    // Recognized card values
    private val VALID_CARDS = setOf(
        "3", "4", "5", "6", "7", "8", "9", "10",
        "J", "Q", "K", "A", "2",
        "小", "大", "王"
    )

    // Recognition confidence threshold
    private val CONFIDENCE_THRESHOLD = 0.75f

    // Debounce: last recognized card and time
    private var lastRecognizedCard: String? = null
    private var lastRecognitionTime = 0L
    private val DEBOUNCE_MS = 1500L

    // OCR model placeholder - initialize with PaddleOCR
    private var ocrInitialized = false

    /** Initialize the OCR model */
    fun initialize(context: android.content.Context): Boolean {
        return try {
            // Load PaddleOCR lite model from assets
            // In production, this would load the actual model files
            ocrInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Recognize cards from a bitmap screenshot
     * Returns list of recognized cards with confidence scores
     */
    fun recognizeCards(bitmap: Bitmap): List<CardDetection> {
        if (!ocrInitialized) {
            return emptyList()
        }

        // Convert Bitmap to OpenCV Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        try {
            // Preprocess image: crop to play area, enhance contrast
            val processedMat = preprocessImage(mat)

            // Run OCR recognition
            // In production, this would call PaddleOCR native library
            val detections = performOCR(processedMat)

            // Filter and deduplicate results
            val filtered = detections.filter { detection ->
                detection.confidence >= CONFIDENCE_THRESHOLD &&
                isValidCard(detection.text) &&
                isNotDuplicate(detection.text)
            }

            // Update last recognized card
            filtered.firstOrNull()?.let {
                lastRecognizedCard = it.text
                lastRecognitionTime = System.currentTimeMillis()
            }

            return filtered.map { it.copy(isValid = true) }

        } finally {
            mat.release()
        }
    }

    /** Preprocess image for better OCR accuracy */
    private fun preprocessImage(source: Mat): Mat {
        val processed = source.clone()

        // Convert to grayscale
        org.opencv.imgproc Imgproc.cvtColor(processed, processed, Imgproc.COLOR_RGBA2GRAY)

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(processed, processed, org.opencv.core.Size(3, 3), 0)

        // Adaptive threshold for better text detection
        Imgproc.adaptiveThreshold(
            processed, processed,
            255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV, 11, 2
        )

        return processed
    }

    /** Perform OCR on preprocessed image */
    private fun performOCR(image: Mat): List<CardDetection> {
        // Placeholder for actual PaddleOCR call
        // In production, this would invoke the native OCR engine
        // Return mock detections for now
        return emptyList()
    }

    /** Check if detected text is a valid card */
    private fun isValidCard(text: String): Boolean {
        return VALID_CARDS.any { text.contains(it) }
    }

    /** Check if this is not a duplicate of recently recognized card */
    private fun isNotDuplicate(card: String): Boolean {
        val currentTime = System.currentTimeMillis()
        if (lastRecognizedCard == card && (currentTime - lastRecognitionTime) < DEBOUNCE_MS) {
            return false
        }
        return true
    }

    /** Card detection result */
    data class CardDetection(
        val text: String,
        val confidence: Float,
        val boundingBox: Rect? = null,
        val isValid: Boolean = false
    )

    /** Rectangle for bounding box */
    data class Rect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    /** Reset recognition state (new game) */
    fun reset() {
        lastRecognizedCard = null
        lastRecognitionTime = 0L
    }
}
