// OCRModelManager.kt: Manages PaddleOCR model files and downloads
package com.example.goujicardcounter.ocr.models

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Manages OCR model files: download, extraction, validation, and caching
 */
class OCRModelManager(private val context: Context) {

    companion object {
        private const val TAG = "OCRModelManager"
        private const val MODEL_DIR_NAME = "paddleocr_models"
        private const val MODEL_CACHE_DIR = "model_cache"
        
        // Model file names (PaddleLite format)
        const val DET_MODEL_FILE = "ocrdet_v2.0_cls.nb"
        const val REC_MODEL_FILE = "ocrrec_v2.0_cls.nb"
        const valCLS_MODEL_FILE = "cls_model.nb"
        
        // Model versions for cache invalidation
        const val MODEL_VERSION = "1.0.0"
    }

    private val modelDir: File
    private val cacheDir: File

    init {
        modelDir = File(context.filesDir, MODEL_DIR_NAME)
        cacheDir = File(context.cacheDir, MODEL_CACHE_DIR)
        
        // Ensure directories exist
        modelDir.mkdirs()
        cacheDir.mkdirs()
    }

    /**
     * Check if all required model files are present and valid
     */
    fun isModelsReady(): Boolean {
        return modelDir.exists() && 
               hasModelFile(DET_MODEL_FILE) &&
               hasModelFile(REC_MODEL_FILE) &&
               hasModelFile(CLS_MODEL_FILE) &&
               isModelVersionValid()
    }

    /**
     * Download OCR models from remote server
     * In production, this would download from a CDN or asset bundle
     */
    fun downloadModels(onProgress: ((Float) -> Unit)? = null): Boolean {
        Log.d(TAG, "Starting model download...")
        
        try {
            // Create temporary cache for download
            val tempZip = File(cacheDir, "models_${MODEL_VERSION}.zip")
            
            // Download model zip file
            val downloaded = downloadModelZip(tempZip, onProgress)
            
            if (downloaded) {
                // Extract and validate
                extractModelZip(tempZip)
                
                // Clean up
                tempZip.delete()
                
                Log.d(TAG, "Models downloaded and extracted successfully")
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            return false
        }
    }

    /**
     * Download model zip file from remote server
     */
    private fun downloadModelZip(targetFile: File, onProgress: ((Float) -> Unit)?): Boolean {
        return try {
            // In production, implement actual HTTP download
            // For now, create dummy model files
            createDummyModels()
            
            targetFile.createNewFile()
            onProgress?.invoke(1.0f)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download models", e)
            false
        }
    }

    /**
     * Extract model zip file
     */
    private fun extractModelZip(zipFile: File) {
        if (!zipFile.exists()) return
        
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(modelDir, entry.name)
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract models", e)
        }
    }

    /**
     * Create dummy model files for development/testing
     */
    private fun createDummyModels() {
        val modelFiles = listOf(DET_MODEL_FILE, REC_MODEL_FILE, CLS_MODEL_FILE)
        
        for (fileName in modelFiles) {
            val file = File(modelDir, fileName)
            if (!file.exists()) {
                // Create empty file as placeholder
                file.createNewFile()
                Log.d(TAG, "Created dummy model: $fileName")
            }
        }
    }

    /**
     * Check if a specific model file exists
     */
    private fun hasModelFile(fileName: String): Boolean {
        return File(modelDir, fileName).exists()
    }

    /**
     * Validate model version
     */
    private fun isModelVersionValid(): Boolean {
        val versionFile = File(modelDir, ".version")
        return if (versionFile.exists()) {
            versionFile.readText().trim() == MODEL_VERSION
        } else {
            // Write version file
            versionFile.writeText(MODEL_VERSION)
            true
        }
    }

    /**
     * Get total size of model files
     */
    fun getModelSize(): Long {
        var totalSize = 0L
        for (file in modelDir.listFiles() ?: emptyArray()) {
            totalSize += file.length()
        }
        return totalSize
    }

    /**
     * Clear cached models
     */
    fun clearModels() {
        modelDir.deleteRecursively()
        modelDir.mkdirs()
        Log.d(TAG, "Models cleared")
    }

    /**
     * Get model file path
     */
    fun getModelFilePath(fileName: String): String? {
        val file = File(modelDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Delete model file
     */
    fun deleteModelFile(fileName: String): Boolean {
        val file = File(modelDir, fileName)
        return file.delete()
    }

    /**
     * Recursively delete directory
     */
    private fun File.deleteRecursively() {
        if (isDirectory) {
            listFiles()?.forEach { it.deleteRecursively() }
        }
        delete()
    }
}
