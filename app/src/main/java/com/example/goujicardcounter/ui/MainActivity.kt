// MainActivity.kt: Main activity with card counter controls
package com.example.goujicardcounter.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.goujicardcounter.R
import com.example.goujicardcounter.logic.GoujiGameLogic
import com.example.goujicardcounter.permission.PermissionManager
import com.example.goujicardcounter.recognition.CardRecognizer
import com.example.goujicardcounter.service.ScreenCaptureService
import com.example.goujicardcounter.ui.floating.FloatingWindowManager
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var cardRecognizer: CardRecognizer
    private lateinit var floatingWindowManager: FloatingWindowManager
    
    private var screenCaptureService: ScreenCaptureService? = null
    private var isServiceBound = false
    private var isCountingActive = false
    private var deckCount = 6  // Default 6-deck

    // UI Components
    private lateinit var tvCardCounts: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button
    private lateinit var spinnerDeckCount: Spinner

    // Card counts map
    private var cardCounts = mutableMapOf<String, Int>()

    // Recognition timer
    private var recognitionTimer: Timer? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScreenCaptureService.LocalBinder
            screenCaptureService = binder.getService()
            isServiceBound = true
            Toast.makeText(this@MainActivity, "录屏服务已连接", Toast.LENGTH_SHORT).show()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            screenCaptureService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize components
        permissionManager = PermissionManager(this)
        cardRecognizer = CardRecognizer()
        floatingWindowManager = FloatingWindowManager(this)

        // Setup UI
        setupViews()
        
        // Check permissions
        checkPermissions()
        
        // Initialize card counts
        resetCardCounts()
    }

    private fun setupViews() {
        tvCardCounts = findViewById(R.id.tvCardCounts)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        spinnerDeckCount = findViewById(R.id.spinnerDeckCount)

        // Deck count spinner
        spinnerDeckCount.setSelection(if (deckCount == 5) 0 else 1)
        spinnerDeckCount.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                deckCount = if (position == 0) 5 else 6
                resetCardCounts()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Start/Stop button
        btnStartStop.setOnClickListener {
            if (isCountingActive) {
                stopCounting()
            } else {
                startCounting()
            }
        }

        // Reset button
        btnReset.setOnClickListener {
            showResetConfirmation()
        }

        // Menu buttons
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnTutorial).setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }
    }

    private fun checkPermissions() {
        if (!permissionManager.hasFloatingWindowPermission()) {
            AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("请点击确定跳转到设置页面开启悬浮窗权限")
                .setPositiveButton("去设置") { _, _ ->
                    permissionManager.requestFloatingWindowPermission()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        if (!permissionManager.hasNotificationPermission()) {
            permissionManager.requestNotificationPermission()
        }

        if (!permissionManager.isBatteryOptimizationIgnored()) {
            AlertDialog.Builder(this)
                .setTitle("建议关闭电池优化")
                .setMessage("关闭电池优化可以保证记牌器在后台稳定运行")
                .setPositiveButton("去设置") { _, _ ->
                    permissionManager.requestBatteryOptimizationIgnore()
                }
                .setNegativeButton("暂不", null)
                .show()
        }
    }

    private fun resetCardCounts() {
        cardCounts = GoujiGameLogic.resetCardCounts(deckCount)
        updateCardDisplay()
    }

    private fun updateCardDisplay() {
        val displayText = GoujiGameLogic.CARD_VALUES.joinToString("\n") { card ->
            val remaining = cardCounts[card] ?: 0
            val total = GoujiGameLogic.getTotalCards(deckCount, card)
            "$card: $remaining/$total"
        }
        tvCardCounts.text = displayText
        floatingWindowManager.updateCardDisplay(cardCounts)
    }

    private fun startCounting() {
        if (!permissionManager.hasFloatingWindowPermission()) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }

        // Bind to screen capture service
        val intent = Intent(this, ScreenCaptureService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)

        // Initialize OCR
        cardRecognizer.initialize(this)

        // Show floating window
        floatingWindowManager.show()

        // Start recognition loop
        startRecognitionLoop()

        isCountingActive = true
        btnStartStop.text = "停止记牌"
        Toast.makeText(this, "开始记牌", Toast.LENGTH_SHORT).show()
    }

    private fun stopCounting() {
        // Stop recognition loop
        recognitionTimer?.cancel()
        recognitionTimer = null

        // Unbind service
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }

        // Hide floating window
        floatingWindowManager.hide()

        isCountingActive = false
        btnStartStop.text = "开始记牌"
        Toast.makeText(this, "停止记牌", Toast.LENGTH_SHORT).show()
    }

    private fun startRecognitionLoop() {
        recognitionTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    // In production, this would capture screen and run OCR
                    // For now, simulate recognition
                    simulateCardRecognition()
                }
            }, 0, 2000)  // Every 2 seconds
        }
    }

    private fun simulateCardRecognition() {
        // Placeholder for actual OCR recognition
        // This would process captured screen frames
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("重置牌局")
            .setMessage("确定要清空当前所有记牌记录吗？")
            .setPositiveButton("确定") { _, _ ->
                resetCardCounts()
                cardRecognizer.reset()
                GoujiGameLogic.clearHistory()
                Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (isCountingActive) {
            // Re-check permissions
            checkPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCounting()
        floatingWindowManager.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionManager.REQUEST_FLOATING_WINDOW -> {
                if (!permissionManager.hasFloatingWindowPermission()) {
                    Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show()
                }
            }
            PermissionManager.REQUEST_NOTIFICATION -> {
                if (!permissionManager.hasNotificationPermission()) {
                    Toast.makeText(this, "通知权限被拒绝", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PermissionManager.REQUEST_FLOATING_WINDOW -> {
                if (permissionManager.handlePermissionResult(requestCode, resultCode)) {
                    Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
                }
            }
            PermissionManager.REQUEST_NOTIFICATION -> {
                if (permissionManager.handlePermissionResult(requestCode, resultCode)) {
                    Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
                }
            }
            PermissionManager.REQUEST_BATTERY_OPTIMIZATION -> {
                if (permissionManager.handlePermissionResult(requestCode, resultCode)) {
                    Toast.makeText(this, "电池优化已忽略", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_tutorial -> {
                startActivity(Intent(this, TutorialActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
