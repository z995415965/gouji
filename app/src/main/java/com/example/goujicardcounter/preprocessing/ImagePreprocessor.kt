// ImagePreprocessor.kt: Image preprocessing for OCR - grayscale, binarization, noise reduction
package com.example.goujicardcounter.preprocessing

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * OpenCV-based image preprocessing pipeline for card recognition
 * Handles grayscale conversion, binarization, denoising, and perspective correction
 */
class ImagePreprocessor {

    companion object {
        private const val TAG = "ImagePreprocessor"
        private const val MIN_CARD_WIDTH = 40      // Minimum card width in pixels
        private const val MIN_CARD_HEIGHT = 60     // Minimum card height in pixels
        private const val MAX_CARD_WIDTH = 200     // Maximum card width in pixels
        private const val MAX_CARD_HEIGHT = 300    // Maximum card height in pixels
    }

    private val lock = ReentrantLock()
    private val grayMat = Mat()
    private val blurredMat = Mat()
    private val binaryMat = Mat()
    private val contoursList = ArrayList<MatOfPoint>()
    private val tempMat = Mat()

    /**
     * Complete preprocessing pipeline
     */
    fun preprocess(bitmap: Bitmap): Bitmap {
        lock.withLock {
            try {
                // Convert Bitmap to OpenCV Mat
                bitmapToMat(bitmap)
                
                // Step 1: Grayscale conversion
                grayscale()
                
                // Step 2: Gaussian blur for noise reduction
                blur()
                
                // Step 3: Adaptive binarization
                binarize()
                
                // Step 4: Morphological operations to connect broken edges
                morphologicalOps()
                
                // Step 5: Perspective correction if needed
                val correctedMat = Mat()
                detectAndCorrectPerspective(binaryMat, correctedMat)
                
                // Convert back to Bitmap
                return matToBitmap(correctedMat)
            } finally {
                releaseMats()
            }
        }
    }

    /**
     * Convert Android Bitmap to OpenCV Mat
     */
    private fun bitmapToMat(bitmap: Bitmap) {
        val inputMat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, inputMat)
        
        // Convert to grayscale
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
        
        inputMat.release()
    }

    /**
     * Convert OpenCV Mat back to Android Bitmap
     */
    private fun matToBitmap(mat: Mat): Bitmap {
        val rgbaMat = Mat()
        Imgproc.cvtColor(mat, rgbaMat, Imgproc.COLOR_GRAY2RGBA)
        
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgbaMat, bitmap)
        
        rgbaMat.release()
        return bitmap
    }

    /**
     * Step 1: Convert to grayscale
     */
    private fun grayscale() {
        // Already converted in bitmapToMat, but can add additional processing here
        grayMat.copyTo(blurredMat)
    }

    /**
     * Step 2: Gaussian blur to reduce noise
     */
    private fun blur() {
        Imgproc.GaussianBlur(
            blurredMat, 
            blurredMat, 
            Size(5.0, 5.0), 
            0
        )
    }

    /**
     * Step 3: Adaptive binarization for card text
     * Uses adaptive threshold which handles varying lighting conditions
     */
    private fun binarize() {
        Imgproc.adaptiveThreshold(
            blurredMat,
            binaryMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            blockSize = 11,
            C = 2
        )
    }

    /**
     * Step 4: Morphological operations
     * Dilate to connect broken edges, then erode to restore original size
     */
    private fun morphologicalOps() {
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(3.0, 3.0)
        )
        
        // Dilate
        Imgproc.dilate(binaryMat, tempMat, kernel)
        
        // Erode
        Imgproc.erode(tempMat, binaryMat, kernel)
        
        kernel.release()
        tempMat.release()
    }

    /**
     * Step 5: Detect and correct perspective distortion
     * Useful when screen is not perfectly aligned
     */
    private fun detectAndCorrectPerspective(source: Mat, dest: Mat) {
        source.copyTo(dest)
        
        // Find contours
        Imgproc.findContours(
            source,
            contoursList,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        // Find largest contour (likely the card area)
        var maxArea = 0.0
        var maxContour: MatOfPoint? = null
        
        for (contour in contoursList) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                maxArea = area
                maxContour = contour
            }
        }
        
        if (maxContour != null && maxArea > 10000) {
            // Approximate contour to polygon
            val polygon = MatOfPoint2f()
            maxContour!!.convertTo(polygon, CvType.CV_32FC2)
            
            val approxCurve = MatOfPoint2f()
            Imgproc.approxPolyDP(polygon, approxCurve, 0.02 * Imgproc.arcLength(polygon, true), true)
            
            // If we have 4 corners, perform perspective transform
            if (approxCurve.total() == 4) {
                val corners = arrayOfNulls<Point>(4)
                val pts = approxCurve.toArray()
                for (i in pts.indices) {
                    corners[i] = pts[i]
                }
                
                // Sort corners: top-left, top-right, bottom-right, bottom-left
                val sortedCorners = sortCorners(corners)
                
                // Calculate destination dimensions
                val width1 = Math.hypot(sortedCorners[1]!!.x - sortedCorners[0]!!.x, 
                                       sortedCorners[1]!!.y - sortedCorners[0]!!.y)
                val width2 = Math.hypot(sortedCorners[3]!!.x - sortedCorners[2]!!.x, 
                                       sortedCorners[3]!!.y - sortedCorners[2]!!.y)
                val height1 = Math.hypot(sortedCorners[2]!!.x - sortedCorners[1]!!.x, 
                                        sortedCorners[2]!!.y - sortedCorners[1]!!.y)
                val height2 = Math.hypot(sortedCorners[3]!!.x - sortedCorners[0]!!.x, 
                                        sortedCorners[3]!!.y - sortedCorners[0]!!.y)
                
                val maxWidth = max(width1, width2).toInt()
                val maxHeight = max(height1, height2).toInt()
                
                // Create destination points
                val dstPoints = floatArrayOf(
                    0f, 0f,                          // top-left
                    maxWidth.toFloat(), 0f,          // top-right
                    maxWidth.toFloat(), maxHeight.toFloat(), // bottom-right
                    0f, maxHeight.toFloat()          // bottom-left
                )
                
                // Perform perspective transform
                val srcMatrix = MatOfPoint2f(*sortedCorners.map { it!! }.toTypedArray())
                val dstMatrix = MatOfPoint2f(*dstPoints.chunked(2).map { Point(it[0].toDouble(), it[1].toDouble()) }.toTypedArray())
                
                val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMatrix, dstMatrix)
                Imgproc.warpPerspective(source, dest, perspectiveTransform, 
                                      Size(maxWidth.toDouble(), maxHeight.toDouble()))
                
                perspectiveTransform.release()
                srcMatrix.release()
                dstMatrix.release()
            }
            
            polygon.release()
            approxCurve.release()
            maxContour!!.release()
        }
        
        contoursList.clear()
    }

    /**
     * Sort corners in order: top-left, top-right, bottom-right, bottom-left
     */
    private fun sortCorners(corners: Array<Point?>): Array<Point?> {
        // Find top-left and bottom-right by sum of coordinates
        var topLeft = corners[0]
        var bottomRight = corners[0]
        var topRight = corners[0]
        var bottomLeft = corners[0]
        
        for (corner in corners) {
            if (corner == null) continue
            
            val sum = corner.x + corner.y
            if (sum < topLeft!!.x + topLeft.y) topLeft = corner
            if (sum > bottomRight!!.x + bottomRight.y) bottomRight = corner
            
            val diff = corner.y - corner.x
            if (diff < topRight!!.y - topRight.x) topRight = corner
            if (diff > bottomLeft!!.y - bottomLeft.x) bottomLeft = corner
        }
        
        return arrayOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    /**
     * Extract card regions from preprocessed image
     * Returns list of bounding rectangles for each detected card
     */
    fun extractCardRegions(source: Mat): MutableList<Rect> {
        val regions = mutableListOf<Rect>()
        
        Imgproc.findContours(
            source,
            contoursList,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        for (contour in contoursList) {
            val rect = Imgproc.boundingRect(contour)
            
            // Filter by size to avoid noise
            if (rect.width >= MIN_CARD_WIDTH && rect.width <= MAX_CARD_WIDTH &&
                rect.height >= MIN_CARD_HEIGHT && rect.height <= MAX_CARD_HEIGHT) {
                regions.add(rect)
            }
        }
        
        // Sort by position (left to right, top to bottom)
        regions.sortBy { it.x + it.y * source.width() }
        
        return regions
    }

    /**
     * Release OpenCV matrices to prevent memory leaks
     */
    private fun releaseMats() {
        grayMat.release()
        blurredMat.release()
        binaryMat.release()
        tempMat.release()
        contoursList.clear()
    }

    /**
     * Simple threshold binarization (alternative to adaptive)
     */
    fun simpleBinarize(source: Mat, dest: Mat, threshold: Int = 127) {
        Imgproc.threshold(source, dest, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
    }

    /**
     * Apply median blur for salt-and-pepper noise removal
     */
    fun medianBlur(source: Mat, dest: Mat, ksize: Int = 5) {
        Imgproc.medianBlur(source, dest, ksize)
    }
}
