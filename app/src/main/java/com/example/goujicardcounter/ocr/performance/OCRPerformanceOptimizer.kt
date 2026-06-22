// OCRPerformanceOptimizer.kt: Optimizes OCR recognition performance for real-time card counting
package com.example.goujicardcounter.ocr.performance

import android.graphics.Bitmap
import android.util.Log
import com.example.goujicardcounter.ocr.PaddleOCREngine
import com.example.goujicardcounter.preprocessing.ImagePreprocessor
import com.example.goujicardcounter.detection.CardRegionDetector
import java.util.concurrent.*
import kotlin.math.min

/**
 * Performance optimizer for OCR recognition pipeline
 * Implements caching, batching, and adaptive processing strategies
 */
class OCRPerformanceOptimizer(
    private val ocrEngine: PaddleOCREngine,
    private val preprocessor: ImagePreprocessor,
    private val cardDetector: CardRegionDetector
) {

    companion object {
        private const val TAG = "OCRPerfOptimizer"
        private const val CACHE_MAX_SIZE = 50
        private const val BATCH_TIMEOUT_MS = 16  // ~60fps target
        private const val MIN_RECOGNITION_INTERVAL_MS = 100  // 10fps max
    }

    private val executorService: ExecutorService = ThreadPoolExecutor(
        2,  // Core pool size
        4,  // Max pool size
        60L, TimeUnit.SECONDS,
        LinkedBlockingQueue(100),
        ThreadFactory {
            Thread(it, "OCR-Pipeline").apply { isDaemon = true }
        }
    )

    // Recognition result cache
    private val resultCache = LinkedHashMap<String, CachedResult>(CACHE_MAX_SIZE, 0.75f, true)
    private val cacheLock = ReentrantLock()

    // Frame buffer for batching
    private val frameBuffer = ConcurrentLinkedQueue<FrameData>()
    private val batchLock = ReentrantLock()

    // Last recognition timestamp for rate limiting
    private var lastRecognitionTime = 0L

    /**
     * Optimized card recognition pipeline
     * Uses caching, batching, and adaptive processing for real-time performance
     */
    fun recognizeCards(bitmap: Bitmap, callback: (List<RecognizedCard>) -> Unit) {
        // Rate limiting: don't recognize more than 10fps
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRecognitionTime < MIN_RECOGNITION_INTERVAL_MS) {
            Log.v(TAG, "Rate limiting: skipping recognition")
            return
        }

        // Generate cache key from bitmap hash
        val cacheKey = generateCacheKey(bitmap)
        
        // Check cache first
        cacheLock.withLock {
            resultCache[cacheKey]?.let { cached ->
                if (cached.isValid(currentTime)) {
                    callback(cached.cards)
                    return
                }
            }
        }

        // Submit recognition task
        executorService.submit {
            try {
                lastRecognitionTime = currentTime
                val startTime = System.nanoTime()
                
                // Step 1: Preprocess image
                val processedBitmap = preprocessor.preprocess(bitmap)
                
                // Step 2: Detect card regions
                val cardRegions = cardDetector.detectCards(processedBitmap)
                
                // Step 3: Recognize text in card regions
                val recognizedCards = mutableListOf<RecognizedCard>()
                
                for (region in cardRegions) {
                    // Crop card region
                    val cardBitmap = cropCardRegion(processedBitmap, region)
                    
                    // Recognize card value
                    val cardValue = ocrEngine.recognizeCardValue(cardBitmap)
                    
                    if (cardValue != null) {
                        recognizedCards.add(RecognizedCard(
                            value = cardValue,
                            position = region,
                            confidence = calculateConfidence(cardBitmap),
                            timestamp = currentTime
                        ))
                    }
                    
                    // Limit processing time per frame
                    if (System.nanoTime() - startTime > 50_000_000) {  // 50ms budget
                        break
                    }
                }
                
                // Cache results
                cacheLock.withLock {
                    resultCache[cacheKey] = CachedResult(
                        cards = recognizedCards.toList(),
                        timestamp = currentTime,
                        bitmapHash = cacheKey.hashCode()
                    )
                    
                    // Evict old entries if cache is full
                    while (resultCache.size > CACHE_MAX_SIZE) {
                        val oldestKey = resultCache.keys.firstOrNull()
                        oldestKey?.let { resultCache.remove(it) }
                    }
                }
                
                // Post-process results
                val finalCards = postProcessResults(recognizedCards)
                
                // Callback on main thread (in production, use Handler)
                callback(finalCards)
                
                val elapsed = (System.nanoTime() - startTime) / 1_000_000
                Log.d(TAG, "Recognition completed in ${elapsed}ms, found ${finalCards.size} cards")
                
            } catch (e: Exception) {
                Log.e(TAG, "Recognition failed", e)
                callback(emptyList())
            }
        }
    }

    /**
     * Generate cache key from bitmap
     */
    private fun generateCacheKey(bitmap: Bitmap): String {
        // Simple hash based on bitmap dimensions and content
        val contentHash = bitmap.hashCode()
        return "${bitmap.width}_${bitmap.height}_$contentHash"
    }

    /**
     * Crop card region from bitmap
     */
    private fun cropCardRegion(bitmap: Bitmap, region: CardRegionDetector.CardRegion): Bitmap {
        return try {
            Bitmap.createBitmap(
                bitmap,
                region.x,
                region.y,
                region.width,
                region.height
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop card region", e)
            bitmap
        }
    }

    /**
     * Calculate confidence score for card recognition
     */
    private fun calculateConfidence(cardBitmap: Bitmap): Float {
        // Simple heuristic based on bitmap quality
        val qualityScore = min(cardBitmap.width.toFloat() / 100f, 1.0f) *
                          min(cardBitmap.height.toFloat() / 150f, 1.0f)
        return qualityScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * Post-process recognition results for consistency
     */
    private fun postProcessResults(cards: List<RecognizedCard>): List<RecognizedCard> {
        // Remove duplicates based on card value and position
        val uniqueCards = mutableSetOf<String>()
        val result = mutableListOf<RecognizedCard>()
        
        for (card in cards) {
            val cardKey = "${card.value}_${card.position.x}_${card.position.y}"
            if (cardKey !in uniqueCards) {
                uniqueCards.add(cardKey)
                result.add(card)
            }
        }
        
        return result.sortedBy { it.position.x }
    }

    /**
     * Batch multiple frames for improved accuracy
     */
    fun batchRecognition(frames: List<Bitmap>): List<RecognizedCard> {
        val allCards = mutableListOf<RecognizedCard>()
        
        for (frame in frames) {
            val cards = recognizeCardsSync(frame)
            allCards.addAll(cards)
        }
        
        // Aggregate results across frames
        return aggregateResults(allCards)
    }

    /**
     * Synchronous card recognition (for testing/batching)
     */
    private fun recognizeCardsSync(bitmap: Bitmap): List<RecognizedCard> {
        val latch = CountDownLatch(1)
        var result = listOf<RecognizedCard>()
        
        recognizeCards(bitmap) { cards ->
            result = cards
            latch.countDown()
        }
        
        try {
            latch.await(1000, TimeUnit.MILLISECONDS)  // 1 second timeout
        } catch (e: InterruptedException) {
            Log.e(TAG, "Recognition timed out", e)
        }
        
        return result
    }

    /**
     * Aggregate recognition results across multiple frames
     */
    private fun aggregateResults(cards: List<RecognizedCard>): List<RecognizedCard> {
        // Group by card value and calculate average confidence
        val grouped = cards.groupBy { it.value }
        
        return grouped.map { (value, cardList) ->
            val avgConfidence = cardList.average { it.confidence }
            val mostCommonPosition = cardList.groupBy { it.position }.entries
                .maxByOrNull { it.value.size }?.key
            
            RecognizedCard(
                value = value,
                position = mostCommonPosition ?: cardList.first().position,
                confidence = avgConfidence.toFloat(),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Clear recognition cache
     */
    fun clearCache() {
        cacheLock.withLock {
            resultCache.clear()
        }
        Log.d(TAG, "Recognition cache cleared")
    }

    /**
     * Shutdown optimizer and release resources
     */
    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
        
        clearCache()
        Log.d(TAG, "OCR Performance Optimizer shut down")
    }

    /**
     * Recognized card data
     */
    data class RecognizedCard(
        val value: String,
        val position: CardRegionDetector.CardRegion,
        val confidence: Float,
        val timestamp: Long
    )

    /**
     * Cached recognition result
     */
    private data class CachedResult(
        val cards: List<RecognizedCard>,
        val timestamp: Long,
        val bitmapHash: Int
    ) {
        companion object {
            private const val CACHE_TTL_MS = 500  // 500ms cache lifetime
        }
        
        fun isValid(currentTime: Long): Boolean {
            return (currentTime - timestamp) < CACHE_TTL_MS
        }
    }
}
