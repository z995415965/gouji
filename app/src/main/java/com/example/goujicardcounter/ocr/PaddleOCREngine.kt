// PaddleOCREngine.kt: PaddleOCR Lite integration for card text recognition
package com.example.goujicardcounter.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.baidu.paddle.lite.ocr.OcrResult
import com.baidu.paddle.lite.ocr.PaddleOCR
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.util.concurrent.Executors

/**
 * PaddleOCR Lite wrapper for card text recognition
 * Integrates Baidu's lightweight OCR engine optimized for mobile
 */
class PaddleOCREngine(private val context: Context) {

    companion object {
        private const val TAG = "PaddleOCREngine"
        private const val MODEL_DIR = "paddleocr_models"
        private const val OCR_CONFIDENCE_THRESHOLD = 0.75f
    }

    private var paddleOCR: PaddleOCR? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var isInitialized = false

    /**
     * Initialize the OCR engine
     * Loads models from assets or downloads them if not present
     */
    fun initialize(onComplete: (() -> Unit)? = null) {
        executor.execute {
            try {
                // Check if models exist
                val modelDir = File(context.filesDir, MODEL_DIR)
                if (!modelDir.exists() || !hasModelFiles(modelDir)) {
                    Log.d(TAG, "Downloading OCR models...")
                    downloadModels(context, modelDir)
                }
                
                // Initialize PaddleOCR
                paddleOCR = PaddleOCR.createInstance(
                    context,
                    modelDir.absolutePath,
                    false  // Use CPU (set true for GPU)
                )
                
                isInitialized = true
                Log.d(TAG, "PaddleOCR initialized successfully")
                
                onComplete?.invoke()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize PaddleOCR", e)
                isInitialized = false
            }
        }
    }

    /**
     * Recognize text from a bitmap image
     * Returns list of recognized text with bounding boxes
     */
    fun recognizeText(bitmap: Bitmap): List<TextRegion> {
        if (!isInitialized || paddleOCR == null) {
            Log.w(TAG, "OCR engine not initialized")
            return emptyList()
        }
        
        try {
            // Preprocess image for better OCR results
            val processedBitmap = preprocessForOCR(bitmap)
            
            // Run OCR recognition
            val results = paddleOCR?.recognize(processedBitmap)
            
            // Parse results
            val textRegions = parseOCRResults(results)
            
            Log.d(TAG, "Recognized ${textRegions.size} text regions")
            return textRegions
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during OCR recognition", e)
            return emptyList()
        }
    }

    /**
     * Recognize card values from a cropped card image
     * Returns the recognized card value (e.g., "A", "K", "3")
     */
    fun recognizeCardValue(cardImage: Bitmap): String? {
        val textRegions = recognizeText(cardImage)
        
        if (textRegions.isEmpty()) {
            return null
        }
        
        // Find the most prominent text (usually the card value)
        val primaryText = textRegions
            .filter { it.confidence >= OCR_CONFIDENCE_THRESHOLD }
            .maxByOrNull { it.area }
        
        return primaryText?.let { extractCardValue(it.text) }
    }

    /**
     * Preprocess image for optimal OCR recognition
     */
    private fun preprocessForOCR(bitmap: Bitmap): Bitmap {
        // Convert to grayscale and enhance contrast
        val mat = Mat()
        val grayMat = Mat()
        
        try {
            // Convert bitmap to Mat
            android.graphics.Bitmap.createBitmap(bitmap).copyPixelsToBuffer(java.nio.ByteBuffer.allocateDirect(bitmap.height * bitmap.width * 4))
            mat.fromBitmap(bitmap)
            
            // Convert to grayscale
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)
            
            // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
            val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
            clahe.apply(grayMat, grayMat)
            
            // Threshold to enhance text
            Imgproc.threshold(grayMat, grayMat, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
            
            // Convert back to bitmap
            return grayMat.toBitmap(bitmap.width, bitmap.height)
            
        } finally {
            mat.release()
            grayMat.release()
        }
    }

    /**
     * Parse PaddleOCR results into structured TextRegion objects
     */
    private fun parseOCRResults(ocrResults: List<OcrResult>?): List<TextRegion> {
        val regions = mutableListOf<TextRegion>()
        
        ocrResults?.forEach { result ->
            // Filter by confidence threshold
            if (result.confidence >= OCR_CONFIDENCE_THRESHOLD) {
                regions.add(TextRegion(
                    text = result.text,
                    confidence = result.confidence,
                    boundingBox = result.boundingBox,
                    area = calculateBoundingBoxArea(result.boundingBox)
                ))
            }
        }
        
        return regions
    }

    /**
     * Extract card value from recognized text
     * Maps OCR results to Goju card values
     */
    private fun extractCardValue(text: String): String? {
        val cleanedText = text.trim()
        
        // Direct mapping for common card values
        val cardValues = mapOf(
            "3" to "3", "4" to "4", "5" to "5", "6" to "6", "7" to "7",
            "8" to "8", "9" to "9", "10" to "10", "J" to "J", "Q" to "Q",
            "K" to "K", "A" to "A", "2" to "2",
            "小王" to "小王", "大王" to "大王",
            "small_joker" to "小王", "big_joker" to "大王",
            "小" to "小王", "大" to "大王"
        )
        
        // Try direct match first
        cardValues[cleanedText]?.let { return it }
        
        // Try case-insensitive match
        cardValues.entries.find { it.key.equals(cleanedText, ignoreCase = true) }?.let { return it.value }
        
        // Try to extract card value from mixed text (e.g., "A of spades")
        val words = cleanedText.split(Regex("\\s+"))
        for (word in words) {
            cardValues[word]?.let { return it }
        }
        
        // Fallback: return first word if it looks like a card value
        if (cleanedText.length <= 3) {
            return cleanedText.uppercase()
        }
        
        return null
    }

    /**
     * Download OCR models if not present
     */
    private fun downloadModels(context: Context, modelDir: File) {
        // In production, download from asset bundle or remote server
        // For now, create dummy model structure
        modelDir.mkdirs()
        
        // Create model files (these would normally be actual PaddleOCR models)
        val modelFiles = listOf(
            "ocrdet_v2.0_cls.nb",
            "ocrrec_v2.0_cls.nb",
            "cls_model.nb"
        )
        
        for (fileName in modelFiles) {
            val file = File(modelDir, fileName)
            if (!file.exists()) {
                // Copy from assets or download
                Log.d(TAG, "Model file missing: $fileName")
            }
        }
    }

    /**
     * Check if required model files exist
     */
    private fun hasModelFiles(modelDir: File): Boolean {
        val requiredFiles = listOf(
            "ocrdet_v2.0_cls.nb",
            "ocrrec_v2.0_cls.nb"
        )
        
        return requiredFiles.all { file ->
            File(modelDir, file).exists()
        }
    }

    /**
     * Calculate area of bounding box
     */
    private fun calculateBoundingBoxArea(boundingBox: OcrResult.BoundingBox?): Int {
        boundingBox ?: return 0
        
        val points = boundingBox.points
        if (points.size < 4) return 0
        
        // Calculate convex hull area
        val matOfPoints = MatOfPoint2f(
            points[0].x.toFloat(), points[0].y.toFloat(),
            points[1].x.toFloat(), points[1].y.toFloat(),
            points[2].x.toFloat(), points[2].y.toFloat(),
            points[3].x.toFloat(), points[3].y.toFloat()
        )
        
        val area = Imgproc.contourArea(matOfPoints)
        matOfPoints.release()
        
        return area.toInt()
    }

    /**
     * Release resources
     */
    fun destroy() {
        paddleOCR?.release()
        paddleOCR = null
        isInitialized = false
        executor.shutdown()
    }

    /**
     * Text region recognized by OCR
     */
    data class TextRegion(
        val text: String,
        val confidence: Float,
        val boundingBox: OcrResult.BoundingBox?,
        val area: Int
    ) {
        fun isLikelyCardValue(): Boolean {
            // Card values are typically short (1-3 characters)
            return text.length <= 3 && confidence >= OCR_CONFIDENCE_THRESHOLD
        }
    }
}
