// FloatingWindowManager.kt: Manages floating window UI
package com.example.goujicardcounter.ui.floating

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.goujicardcounter.R

/**
 * Floating window manager for card counter display
 * Shows draggable ball icon that expands to card counting panel
 */
class FloatingWindowManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWindowManager"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = false
    private var isVisible = false

    // Window parameters
    private val params = WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
    }

    /** Initialize and show floating window */
    fun show(initialX: Int = 0, initialY: Int = 0) {
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // Inflate floating view
            floatingView = LayoutInflater.from(context).inflate(R.layout.view_floating_card_counter, null).apply {
                // Setup gesture detector for dragging
                setupGestureDetector(this)
                
                // Setup expand/collapse
                findViewById<ImageView>(R.id.ivFloatingBall)?.setOnClickListener {
                    toggleExpand()
                }
            }

            // Set initial position
            params.x = initialX
            params.y = initialY

            // Add to window manager
            windowManager?.addView(floatingView, params)
            isVisible = true
            
            Log.d(TAG, "Floating window shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating window", e)
        }
    }

    /** Hide floating window */
    fun hide() {
        try {
            floatingView?.let { view ->
                windowManager?.removeView(view)
            }
            floatingView = null
            isVisible = false
            Log.d(TAG, "Floating window hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide floating window", e)
        }
    }

    /** Toggle expanded/collapsed state */
    private fun toggleExpand() {
        isExpanded = !isExpanded
        floatingView?.let { view ->
            val panel = view.findViewById<View>(R.id.cardCountPanel)
            panel.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            // Animate expansion
            view.animate()
                .alpha(if (isExpanded) 0.9f else 1.0f)
                .setDuration(200)
                .start()
        }
    }

    /** Setup gesture detector for dragging */
    private fun setupGestureDetector(view: View) {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            var startX = 0
            var startY = 0

            override fun onDown(e: MotionEvent): Boolean {
                startX = e.rawX.toInt()
                startY = e.rawY.toInt()
                return true
            }

            override fun onMove(e: MotionEvent): Boolean {
                if (floatingView != null) {
                    val x = params.x + (e.rawX.toInt() - startX)
                    val y = params.y + (e.rawY.toInt() - startY)
                    
                    // Clamp to screen bounds
                    val maxX = windowManager?.defaultDisplay?.width ?: 0
                    val maxY = windowManager?.defaultDisplay?.height ?: 0
                    params.x = Math.max(0, Math.min(x, maxX - view.width))
                    params.y = Math.max(0, Math.min(y, maxY - view.height))
                    
                    windowManager?.updateViewLayout(view, params)
                }
                return true
            }
        })

        view.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    /** Update card count display */
    fun updateCardDisplay(cardCounts: Map<String, Int>) {
        floatingView?.findViewById<TextView>(R.id.tvCardCounts)?.text = 
            cardCounts.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }

    /** Check if floating window is visible */
    fun isVisible(): Boolean = isVisible

    /** Destroy floating window */
    fun destroy() {
        hide()
    }
}
