// ScreenCaptureService.kt: Foreground service for screen recording
package com.example.goujicardcounter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask

/**
 * Foreground service that captures screen content
 * Uses MediaProjection API to get screen frames
 * Runs continuously while card counting is active
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "card_counter_channel"

        var instance: ScreenCaptureService? = null
    }

    private val binder = LocalBinder()
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var captureTimer: Timer? = null
    private var isRunning = false

    // Callback to notify activity of captured frames
    var onFrameCaptured: ((android.graphics.Bitmap) -> Unit)? = null

    /** Binder for binding to this service */
    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Service created")
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        isRunning = true
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopForegroundService()
        instance = null
        Log.d(TAG, "Service destroyed")
    }

    /** Create notification channel for foreground service */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "够级记牌器",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台运行以捕获屏幕内容"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /** Start foreground service with notification */
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("够级记牌器")
            .setContentText("正在捕获屏幕内容...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Stop foreground service */
    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Initialize screen capture with MediaProjection */
    fun initializeCapture(resultCode: Int, data: Intent) {
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        
        if (mediaProjection != null) {
            Log.d(TAG, "Screen capture initialized")
            startCaptureLoop()
        } else {
            Log.e(TAG, "Failed to initialize screen capture")
        }
    }

    /** Start periodic screen capture loop */
    private fun startCaptureLoop() {
        captureTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (isRunning && mediaProjection != null) {
                        captureFrame()
                    }
                }
            }, 0, 1000)  // Capture every 1 second
        }
    }

    /** Capture a single frame from screen */
    private fun captureFrame() {
        try {
            // In production, this would use ImageReader to get screen frames
            // For now, just log
            Log.d(TAG, "Capturing frame...")
            
            // TODO: Implement actual frame capture using VirtualDisplay
            // val display = mediaProjection?.createVirtualDisplay(
            //     "CardCounter", screenWidth, screenHeight, dpi,
            //     DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            //     imageReader?.surface, null, null
            // )
            
        } catch (e: Exception) {
            Log.e(TAG, "Frame capture failed", e)
        }
    }

    /** Stop screen capture */
    fun stopCapture() {
        captureTimer?.cancel()
        mediaProjection?.stop()
        mediaProjection = null
    }

    /** Check if service is running */
    fun isServiceRunning(): Boolean = isRunning
}
