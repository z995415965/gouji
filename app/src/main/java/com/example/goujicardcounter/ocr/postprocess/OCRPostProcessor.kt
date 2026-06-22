// OCRPostProcessor.kt: Post-process OCR results and map to Goju card values
package com.example.goujicardcounter.ocr.postprocess

import android.graphics.Rect
import android.util.Log
import com.example.goujicardcounter.ocr.PaddleOCREngine.TextRegion
import java.util.concurrent.ConcurrentHashMap

/**
 * Post-processes OCR results to extract card information
 * Filters noise, validates card values, and maintains recognition history
 */
class OCRPostProcessor {

    companion object {
        private const val TAG = "OCRPostProcessor"
        private const val CONFIDENCE_THRESHOLD = 0.75f
        private const val MIN_TEXT_LENGTH = 1
        private const val MAX_TEXT_LENGTH = 3
        private const val HISTORY_SIZE = 100
    }

    // Recognized card history for temporal consistency checking
    private val recognitionHistory = ConcurrentHashMap<String, RecognitionRecord>()
    
    // Valid Goju card values
    private val validCardValues = setOf(
        "3", "4", "5", "6", "7", "8", "9", "10",
        "J", "Q", "K", "A", "2",
        "小王", "大王", "SPIDER", "JOKER"
    )

    // Position-based card tracking
    private val positionTracking = mutableMapOf<Int, String>()

    /**
     * Process OCR text regions and extract valid card values
     */
    fun processTextRegions(textRegions: List<TextRegion>): List<CardRecognition> {
        val recognizedCards = mutableListOf<CardRecognition>()
        
        for (region in textRegions) {
            val processedRegion = processSingleRegion(region)
            if (processedRegion != null) {
                recognizedCards.add(processedRegion)
            }
        }
        
        // Apply temporal consistency check
        applyTemporalConsistency(recognizedCards)
        
        Log.d(TAG, "Processed ${recognizedCards.size} valid card recognitions")
        return recognizedCards
    }

    /**
     * Process a single OCR text region
     */
    private fun processSingleRegion(region: TextRegion): CardRecognition? {
        val text = region.text.trim()
        
        // Basic validation
        if (text.isEmpty() || text.length > MAX_TEXT_LENGTH) {
            return null
        }
        
        // Confidence check
        if (region.confidence < CONFIDENCE_THRESHOLD) {
            return null
        }
        
        // Normalize text
        val normalizedText = normalizeText(text)
        
        // Validate against known card values
        if (!isValidCardValue(normalizedText)) {
            return null
        }
        
        // Create recognition record
        val record = CardRecognition(
            cardValue = normalizedText,
            confidence = region.confidence,
            boundingBox = region.boundingBox,
            timestamp = System.currentTimeMillis()
        )
        
        // Update position tracking
        region.boundingBox?.let { bbox ->
            val positionKey = calculatePositionKey(bbox)
            positionTracking[positionKey] = normalizedText
        }
        
        // Add to history
        addToHistory(record)
        
        return record
    }

    /**
     * Normalize OCR text to standard card values
     */
    private fun normalizeText(text: String): String {
        return when {
            text.lowercase() == "small" || text.lowercase() == "s" -> "小王"
            text.lowercase() == "big" || text.lowercase() == "b" -> "大王"
            text.lowercase() == "joker" || text.lowercase() == "jk" -> "JOKER"
            text.matches(Regex("[3-9JKQA2]")) -> text.uppercase()
            text == "10" -> "10"
            else -> text
        }
    }

    /**
     * Check if text is a valid Goju card value
     */
    private fun isValidCardValue(text: String): Boolean {
        return text in validCardValues || 
               text.matches(Regex("^[3-9JKQA2]$")) ||
               text == "10"
    }

    /**
     * Apply temporal consistency to reduce false positives
     * Uses previous recognitions to validate current ones
     */
    private fun applyTemporalConsistency(recognitions: List<CardRecognition>) {
        for (recognition in recognitions) {
            val similarHistorical = recognitionHistory.values.filter { 
                it.cardValue == recognition.cardValue &&
                (System.currentTimeMillis() - it.timestamp) < 5000  // Within 5 seconds
            }
            
            if (similarHistorical.isNotEmpty()) {
                // This card value was recently recognized, likely valid
                recognition.isConfirmed = true
            } else {
                // First time seeing this card value, mark as tentative
                recognition.isConfirmed = false
            }
        }
    }

    /**
     * Calculate position key from bounding box
     */
    private fun calculatePositionKey(bbox: Rect?): String {
        if (bbox == null) return "unknown"
        return "${bbox.centerX()}/${bbox.centerY()}"
    }

    /**
     * Add recognition to history
     */
    private fun addToHistory(record: CardRecognition) {
        recognitionHistory[record.timestamp.toString()] = record
        
        // Limit history size
        if (recognitionHistory.size > HISTORY_SIZE) {
            val oldestKey = recognitionHistory.keys.minOrNull()
            oldestKey?.let { recognitionHistory.remove(it) }
        }
    }

    /**
     * Get recent card recognitions
     */
    fun getRecentRecognitions(count: Int = 10): List<CardRecognition> {
        return recognitionHistory.values
            .sortedByDescending { it.timestamp }
            .take(count)
    }

    /**
     * Clear recognition history
     */
    fun clearHistory() {
        recognitionHistory.clear()
        positionTracking.clear()
        Log.d(TAG, "Recognition history cleared")
    }

    /**
     * Check if a specific card value has been recently recognized
     */
    fun isRecentlyRecognized(cardValue: String, timeWindowMs: Long = 5000): Boolean {
        val currentTime = System.currentTimeMillis()
        return recognitionHistory.values.any { 
            it.cardValue == cardValue && 
            (currentTime - it.timestamp) < timeWindowMs
        }
    }

    /**
     * Card recognition result
     */
    data class CardRecognition(
        val cardValue: String,
        val confidence: Float,
        val boundingBox: Rect?,
        val timestamp: Long,
        var isConfirmed: Boolean = false
    ) {
        fun isValid(): Boolean {
            return confidence >= CONFIDENCE_THRESHOLD && cardValue.isNotBlank()
        }
        
        fun getCardNumericValue(): Int {
            return when (cardValue) {
                "3" -> 3
                "4" -> 4
                "5" -> 5
                "6" -> 6
                "7" -> 7
                "8" -> 8
                "9" -> 9
                "10" -> 10
                "J" -> 11
                "Q" -> 12
                "K" -> 13
                "A" -> 14
                "2" -> 15
                "小王" -> 16
                "大王" -> 17
                else -> 0
            }
        }
    }

    /**
     * Recognition history record
     */
    data class RecognitionRecord(
        val cardValue: String,
        val confidence: Float,
        val timestamp: Long,
        val position: String?
    )
}
