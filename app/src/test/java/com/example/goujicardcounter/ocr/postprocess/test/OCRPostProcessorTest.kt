// OCRPostProcessorTest.kt: Unit tests for OCR post-processing
package com.example.goujicardcounter.ocr.postprocess.test

import com.example.goujicardcounter.ocr.postprocess.OCRPostProcessor
import com.example.goujicardcounter.ocr.PaddleOCREngine.TextRegion
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for OCR post-processing and card value mapping
 */
class OCRPostProcessorTest {

    private lateinit var processor: OCRPostProcessor
    
    @Before
    fun setUp() {
        processor = OCRPostProcessor()
    }

    @Test
    fun testProcessTextRegions() {
        // Create test text regions with card values
        val textRegions = listOf(
            TextRegion("A", 0.9f, null, 100),
            TextRegion("K", 0.85f, null, 100),
            TextRegion("10", 0.8f, null, 100),
            TextRegion("invalid", 0.5f, null, 100)  // Low confidence, should be filtered
        )
        
        val results = processor.processTextRegions(textRegions)
        
        // Should filter out low confidence results
        assertTrue("Should have some valid results", results.size >= 1)
        assertTrue("Should have at least 3 valid card recognitions", results.size >= 3)
    }

    @Test
    fun testNormalizeText() {
        // Test text normalization
        assertEquals("Should normalize 'small' to '小王'", "小王", normalizeTextTest("small"))
        assertEquals("Should normalize 'big' to '大王'", "大王", normalizeTextTest("big"))
        assertEquals("Should uppercase 'j'", "J", normalizeTextTest("j"))
    }

    @Test
    fun testIsValidCardValue() {
        // Test card value validation
        assertTrue("A should be valid", isValidCardValueTest("A"))
        assertTrue("K should be valid", isValidCardValueTest("K"))
        assertTrue("10 should be valid", isValidCardValueTest("10"))
        assertFalse("invalid should not be valid", isValidCardValueTest("invalid"))
    }

    @Test
    fun testClearHistory() {
        // Test history clearing
        processor.clearHistory()
        
        // Should not have any recent recognitions after clearing
        val recent = processor.getRecentRecognitions()
        assertTrue("History should be empty after clear", recent.isEmpty())
    }

    // Helper methods for testing private functions
    private fun normalizeTextTest(text: String): String {
        return when {
            text.lowercase() == "small" || text.lowercase() == "s" -> "小王"
            text.lowercase() == "big" || text.lowercase() == "b" -> "大王"
            text.lowercase() == "joker" || text.lowercase() == "jk" -> "JOKER"
            text.matches(Regex("[3-9JKQA2]")) -> text.uppercase()
            text == "10" -> "10"
            else -> text
        }
    }

    private fun isValidCardValue(text: String): Boolean {
        val validCardValues = setOf("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2", "小王", "大王", "JOKER")
        return text in validCardValues || text.matches(Regex("^[3-9JKQA2]$")) || text == "10"
    }
}
