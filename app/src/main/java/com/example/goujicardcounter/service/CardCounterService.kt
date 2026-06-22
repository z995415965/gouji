// CardCounterService.kt: Foreground service for continuous card counting
package com.example.goujicardcounter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.goujicardcounter.logic.GoujiGameLogic
import com.example.goujicardcounter.recognition.CardRecognizer
import java.util.Timer
import java.util.TimerTask

/**
 * Foreground service that continuously counts cards
 * Runs in background while game is active
 * Uses OCR to recognize played cards from screen
 */
class CardCounterService : Service() {

    companion object {
        private const val TAG = "CardCounterService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "card_counter_service"
        
        var instance: CardCounterService? = null
    }

    private val binder = LocalBinder()
    private lateinit var cardRecognizer: CardRecognizer
    private var countingTimer: Timer? = null
    private var isCounting = false
    private var deckCount = 6
    private var cardCounts = mutableMapOf<String, Int>()

    // Callback to notify UI of changes
    var onUpdateCardCounts: ((Map<String, Int>) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): CardCounterService = this@CardCounterService
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        cardRecognizer = CardRecognizer()
        cardCounts = GoujiGameLogic.initCardCounts(deckCount)
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForegroundService()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        countingTimer?.cancel()
        instance = null
        Log.d(TAG, "Service destroyed")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "够级记牌服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台记牌服务运行中"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("够级记牌器")
            .setContentText("正在后台记牌...")
            .setSmallIcon(android.R.drawable.ic_menu_map_mode)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Start counting cards */
    fun startCounting() {
        if (isCounting) return
        
        isCounting = true
        cardRecognizer.initialize(applicationContext)
        
        countingTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (isCounting) {
                        processScreenCapture()
                    }
                }
            }, 0, 2000)  // Every 2 seconds
        }
        
        Log.d(TAG, "Card counting started")
    }

    /** Stop counting cards */
    fun stopCounting() {
        isCounting = false
        countingTimer?.cancel()
        Log.d(TAG, "Card counting stopped")
    }

    /** Process captured screen frame */
    private fun processScreenCapture() {
        try {
            // In production, this would receive frames from ScreenCaptureService
            // For now, simulate card recognition
            
            // TODO: Receive bitmap from ScreenCaptureService
            // val bitmap = screenCaptureService?.getLastFrame()
            // val cards = cardRecognizer.recognizeCards(bitmap)
            
            // Simulate recognizing a card
            simulateCardRecognition()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing screen capture", e)
        }
    }

    /** Simulate card recognition (replace with actual OCR) */
    private fun simulateCardRecognition() {
        // This is a placeholder - in production, actual OCR would run here
        // For testing, we can simulate recognizing cards being played
    }

    /** Reset all card counts */
    fun resetCounts() {
        cardCounts = GoujiGameLogic.resetCardCounts(deckCount)
        cardRecognizer.reset()
        GoujiGameLogic.clearHistory()
        onUpdateCardCounts?.invoke(cardCounts)
        Log.d(TAG, "Card counts reset")
    }

    /** Update deck count */
    fun updateDeckCount(newDeckCount: Int) {
        deckCount = newDeckCount
        cardCounts = GoujiGameLogic.initCardCounts(deckCount)
        onUpdateCardCounts?.invoke(cardCounts)
    }

    /** Get current card counts */
    fun getCurrentCounts(): Map<String, Int> = cardCounts

    /** Record a recognized card */
    fun recordRecognizedCard(cardValue: String, count: Int = 1) {
        val play = GoujiGameLogic.PlayedCard(
            player = GoujiGameLogic.PlayerPosition.SOUTH,
            cardValue = cardValue,
            cardCount = count,
            timestamp = System.currentTimeMillis()
        )
        
        if (GoujiGameLogic.recordPlay(cardCounts, play)) {
            onUpdateCardCounts?.invoke(cardCounts)
            Log.d(TAG, "Recorded card: $cardValue x$count")
        }
    }
}
