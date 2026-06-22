// BootReceiver.kt: Auto-start service on device boot
package com.example.goujicardcounter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.goujicardcounter.service.CardCounterService
import com.example.goujicardcounter.service.ScreenCaptureService

/**
 * Receives BOOT_COMPLETED broadcast and starts services
 * Requires user to enable auto-start in settings
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Boot completed, starting services")
                
                // Start card counter service
                val cardCounterIntent = Intent(context, CardCounterService::class.java)
                context.startService(cardCounterIntent)
                
                // Start screen capture service
                val screenCaptureIntent = Intent(context, ScreenCaptureService::class.java)
                context.startService(screenCaptureIntent)
            }
        }
    }
}
