// PermissionManager.kt: Manages all runtime permissions
package com.example.goujicardcounter.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Manages all permissions required by the app:
 * - SYSTEM_ALERT_WINDOW (floating window)
 * - Media Projection (screen recording)
 * - FOREGROUND_SERVICE
 * - POST_NOTIFICATIONS (Android 13+)
 * - IGNORE_BATTERY_OPTIMIZATIONS (background keepalive)
 */
class PermissionManager(private val activity: Activity) {

    companion object {
        const val REQUEST_FLOATING_WINDOW = 1001
        const val REQUEST_SCREEN_CAPTURE = 1002
        const val REQUEST_NOTIFICATION = 1003
        const val REQUEST_BATTERY_OPTIMIZATION = 1004
    }

    /** Check if floating window permission is granted */
    fun hasFloatingWindowPermission(): Boolean {
        return Settings.canDrawOverlays(activity)
    }

    /** Request floating window permission */
    fun requestFloatingWindowPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivityForResult(intent, REQUEST_FLOATING_WINDOW)
    }

    /** Check if screen capture permission is granted */
    fun hasScreenCapturePermission(): Boolean {
        // This is set via MediaProjection API, tracked separately
        return false
    }

    /** Check if notification permission is granted (Android 13+) */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Request notification permission */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION
            )
        }
    }

    /** Check if battery optimization is ignored */
    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = activity.getSystemService(Activity.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptifications(activity.packageName)
    }

    /** Request to ignore battery optimization */
    fun requestBatteryOptimizationIgnore() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION)
    }

    /** Check all required permissions */
    fun getAllPermissionStatus(): Map<String, Boolean> {
        return mapOf(
            "floating_window" to hasFloatingWindowPermission(),
            "notification" to hasNotificationPermission(),
            "battery_optimization" to isBatteryOptimizationIgnored()
        )
    }

    /** Handle permission request results */
    fun handlePermissionResult(requestCode: Int, resultCode: Int): Boolean {
        return when (requestCode) {
            REQUEST_FLOATING_WINDOW -> {
                hasFloatingWindowPermission()
            }
            REQUEST_NOTIFICATION -> {
                hasNotificationPermission()
            }
            REQUEST_BATTERY_OPTIMIZATION -> {
                isBatteryOptimizationIgnored()
            }
            else -> false
        }
    }
}
