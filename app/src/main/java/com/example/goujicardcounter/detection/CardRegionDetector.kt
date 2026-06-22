// CardRegionDetector.kt: Detects and extracts card regions from screen capture
package com.example.goujicardcounter.detection

import android.graphics.Bitmap
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Detects playing card regions in a screenshot
 * Uses color-based detection combined with shape analysis
 */
class CardRegionDetector {

    companion object {
        private const val TAG = "CardRegionDetector"
        private const val MIN_CARD_AREA = 2000      // Minimum card pixel area
        private const val MAX_CARD_AREA = 50000    // Maximum card pixel area
        private const val ASPECT_RATIO_TOLERANCE = 0.15  // Tolerance for card aspect ratio
        
        // Goju card color ranges (RGB)
        // Cards typically have white background with black text and colored borders
        private val CARD_WHITE_LOWER = Scalar(200.0, 200.0, 200.0)
        private val CARD_WHITE_UPPER = Scalar(255.0, 255.0, 255.0)
    }

    private val lock = ReentrantLock()
    private val hsvMat = Mat()
    private val grayMat = Mat()
    private val threshMat = Mat()
    private val contoursList = ArrayList<MatOfPoint>()
    private val hierarchy = Mat()

    /**
     * Detect card regions in the given bitmap
     * Returns list of Rect objects representing card bounding boxes
     */
    fun detectCards(bitmap: Bitmap): List<CardRegion> {
        lock.withLock {
            try {
                val regions = mutableListOf<CardRegion>()
                
                // Convert to OpenCV format
                val inputMat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
                android.graphics.Bitmap.createBitmap(bitmap).copyTo(inputMat)
                
                // Step 1: Convert to HSV color space
                Imgproc.cvtColor(inputMat, hsvMat, Imgproc.COLOR_RGBA2HSV_FULL)
                
                // Step 2: Detect white/light regions (card backgrounds)
                inRange(hsvMat, Scalar(0.0, 0.0, 200.0), Scalar(180.0, 25.0, 255.0), threshMat)
                
                // Step 3: Morphological operations to clean up noise
                val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
                Imgproc.morphologyEx(threshMat, threshMat, Imgproc.MORPH_CLOSE, kernel)
                Imgproc.morphologyEx(threshMat, threshMat, Imgproc.MORPH_OPEN, kernel)
                kernel.release()
                
                // Step 4: Find contours
                Imgproc.findContours(
                    threshMat,
                    contoursList,
                    hierarchy,
                    Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE
                )
                
                // Step 5: Filter and validate contours as card regions
                for (contour in contoursList) {
                    val rect = Imgproc.boundingRect(contour)
                    val area = Imgproc.contourArea(contour)
                    
                    // Validate region
                    if (isValidCardRegion(rect, area, bitmap.width, bitmap.height)) {
                        // Refine the rectangle to better fit the card
                        val refinedRect = refineCardBoundary(contour, rect)
                        
                        regions.add(CardRegion(
                            x = refinedRect.x,
                            y = refinedRect.y,
                            width = refinedRect.width,
                            height = refinedRect.height,
                            confidence = calculateConfidence(contour, refinedRect)
                        ))
                    }
                }
                
                // Step 6: Remove overlapping regions (keep highest confidence)
                val uniqueRegions = removeOverlappingRegions(regions)
                
                // Sort by position (left to right, top to bottom)
                uniqueRegions.sortWith { a, b ->
                    if (Math.abs(a.y - b.y) < 50) {
                        a.x.compareTo(b.x)  // Same row, sort by x
                    } else {
                        a.y.compareTo(b.y)  // Different rows, sort by y
                    }
                }
                
                Log.d(TAG, "Detected ${uniqueRegions.size} card regions")
                return uniqueRegions
                
            } finally {
                releaseMats()
            }
        }
    }

    /**
     * Check if a contour represents a valid card region
     */
    private fun isValidCardRegion(rect: Rect, area: Double, screenWidth: Int, screenHeight: Int): Boolean {
        // Check area bounds
        if (area < MIN_CARD_AREA || area > MAX_CARD_AREA) {
            return false
        }
        
        // Check aspect ratio (playing cards are typically 2.5:3.5 or similar)
        val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
        val expectedAspectRatio = 0.71  // Typical card aspect ratio
        if (Math.abs(aspectRatio - expectedAspectRatio) > ASPECT_RATIO_TOLERANCE) {
            return false
        }
        
        // Check if region is not too close to screen edges
        val margin = 20
        if (rect.x < margin || rect.y < margin ||
            rect.x + rect.width > screenWidth - margin ||
            rect.y + rect.height > screenHeight - margin) {
            return false
        }
        
        return true
    }

    /**
     * Refine card boundary using contour approximation
     */
    private fun refineCardBoundary(contour: MatOfPoint, originalRect: Rect): Rect {
        // Create a 2D version of the contour for approximation
        val contour2f = MatOfPoint2f()
        contour.convertTo(contour2f, CvType.CV_32FC2)
        
        // Approximate the contour to a polygon
        val epsilon = 0.04 * Imgproc.arcLength(contour2f, true)
        val approxCurve = MatOfPoint2f()
        Imgproc.approxPolyDP(contour2f, approxcurve, epsilon, true)
        
        // Get bounding rect of approximated polygon
        val approxPoints = approxCurve.toArray()
        if (approxPoints.isNotEmpty()) {
            var minX = Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var maxX = Double.MIN_VALUE
            var maxY = Double.MIN_VALUE
            
            for (point in approxPoints) {
                minX = Math.min(minX, point.x)
                minY = Math.min(minY, point.y)
                maxX = Math.max(maxX, point.x)
                maxY = Math.max(maxY, point.y)
            }
            
            val refinedRect = Rect(
                minX.toInt(),
                minY.toInt(),
                (maxX - minX).toInt(),
                (maxY - minY).toInt()
            )
            
            // Ensure refined rect is within original bounds
            val finalRect = Rect(
                Math.max(originalRect.x, refinedRect.x),
                Math.max(originalRect.y, refinedRect.y),
                Math.min(originalRect.width, refinedRect.width),
                Math.min(originalRect.height, refinedRect.height)
            )
            
            contour2f.release()
            approxCurve.release()
            
            return finalRect
        }
        
        contour2f.release()
        return originalRect
    }

    /**
     * Calculate confidence score for a detected card region
     */
    private fun calculateConfidence(contour: MatOfPoint, rect: Rect): Float {
        var confidence = 0.0f
        
        // Factor 1: Area match with expected card size
        val expectedArea = rect.width * rect.height
        val contourArea = Imgproc.contourArea(contour).toFloat()
        val areaRatio = contourArea / expectedArea
        confidence += Math.min(areaRatio, 1.0f) * 0.3f
        
        // Factor 2: Shape regularity (how close to rectangle)
        val perimeter = Imgproc.arcLength(contour, true).toFloat()
        val solidity = 4 * Math.PI * (contourArea / (perimeter * perimeter))
        confidence += Math.min(solidity, 1.0f) * 0.3f
        
        // Factor 3: Aspect ratio match
        val aspectRatio = rect.width.toFloat() / rect.height
        val expectedAspectRatio = 0.71f
        val ratioMatch = 1.0f - Math.abs(aspectRatio - expectedAspectRatio) / expectedAspectRatio
        confidence += Math.max(0f, ratioMatch) * 0.2f
        
        // Factor 4: Contour smoothness
        val approxCurve = MatOfPoint2f()
        val contour2f = MatOfPoint2f()
        contour.convertTo(contour2f, CvType.CV_32FC2)
        Imgproc.approxPolyDP(contour2f, approxCurve, 0.02 * Imgproc.arcLength(contour2f, true), true)
        val vertexCount = approxCurve.rows()
        approxCurve.release()
        contour2f.release()
        
        // Rectangular shapes have 4 vertices
        val shapeFactor = if (vertexCount == 4) 1.0f else 0.5f
        confidence += shapeFactor * 0.2f
        
        return confidence
    }

    /**
     * Remove overlapping card regions, keeping the one with highest confidence
     */
    private fun removeOverlappingRegions(regions: List<CardRegion>): List<CardRegion> {
        val kept = mutableListOf<CardRegion>()
        val sortedRegions = regions.sortedByDescending { it.confidence }
        
        for (region in sortedRegions) {
            var overlaps = false
            
            for (keptRegion in kept) {
                if (calculateIoU(region, keptRegion) > 0.3f) {
                    overlaps = true
                    break
                }
            }
            
            if (!overlaps) {
                kept.add(region)
            }
        }
        
        return kept
    }

    /**
     * Calculate Intersection over Union (IoU) between two regions
     */
    private fun calculateIoU(a: CardRegion, b: CardRegion): Float {
        val intersectionX1 = Math.max(a.x, b.x)
        val intersectionY1 = Math.max(a.y, b.y)
        val intersectionX2 = Math.min(a.x + a.width, b.x + b.width)
        val intersectionY2 = Math.min(a.y + a.height, b.y + b.height)
        
        if (intersectionX1 >= intersectionX2 || intersectionY1 >= intersectionY2) {
            return 0f
        }
        
        val intersectionArea = (intersectionX2 - intersectionX1) * (intersectionY2 - intersectionY1)
        val unionArea = a.width * a.height + b.width * b.height - intersectionArea
        
        return intersectionArea.toFloat() / unionArea.toFloat()
    }

    /**
     * Release OpenCV matrices to prevent memory leaks
     */
    private fun releaseMats() {
        hsvMat.release()
        grayMat.release()
        threshMat.release()
        hierarchy.release()
        contoursList.clear()
    }

    /**
     * Card region data class
     */
    data class CardRegion(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val confidence: Float
    ) {
        fun toRect(): Rect = Rect(x, y, width, height)
        
        fun center(): Point = Point(x + width / 2.0, y + height / 2.0)
        
        fun contains(point: Point): Boolean {
            return point.x >= x && point.x < x + width &&
                   point.y >= y && point.y < y + height
        }
    }
}
