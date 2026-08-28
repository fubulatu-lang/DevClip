package com.devclip.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler

/**
 * Foreground service that owns two overlay windows:
 *  1. A small draggable, tappable circular bubble (always visible once started).
 *  2. A ReactRootView-based popup window (hidden until the bubble is tapped),
 *     rendering the "DevClipPopup" JS component registered in index.ts.
 *
 * Both windows use TYPE_APPLICATION_OVERLAY, which requires the user to have
 * granted the "display over other apps" permission first
 * (see OverlayModule.requestOverlayPermission).
 */
class OverlayService : Service(), DefaultHardwareBackBtnHandler {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var popupRootView: ReactRootView? = null
    private var popupParams: WindowManager.LayoutParams? = null
    private var popupVisible = false

    companion object {
        const val CHANNEL_ID = "devclip_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_RESIZE = "com.devclip.app.ACTION_RESIZE"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"

        private val overlayType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESIZE) {
            val width = intent.getIntExtra(EXTRA_WIDTH, -1)
            val height = intent.getIntExtra(EXTRA_HEIGHT, -1)
            resizePopup(width, height)
        }
        return START_STICKY
    }

    // ---- Notification (required for any foreground service) ----

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "DevClip Overlay", NotificationManager.IMPORTANCE_MIN
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DevClip is running")
            .setContentText("Tap the bubble to view your clipboard history.")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    // ---- Bubble ----

    private fun addBubble() {
        val bubble = View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#3D4CF0"))
            }
        }

        val prefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val sizeDp = when (prefs.getString(Prefs.KEY_BUBBLE_SIZE, "medium")) {
            "small" -> 44
            "large" -> 72
            else -> 56
        }
        val size = (sizeDp * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size, size, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        bubble.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePopup()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubble, params)
        bubbleView = bubble
    }

    // ---- Popup (React Native content) ----

    private fun togglePopup() {
        if (popupVisible) hidePopup() else showPopup()
    }

    private fun showPopup() {
        if (popupRootView == null) {
            val rootView = ReactRootView(this)
            val reactInstanceManager = (application as MainApplication)
                .reactNativeHost.reactInstanceManager
            rootView.startReactApplication(reactInstanceManager, "DevClipPopup", null)
            popupRootView = rootView
        }

        val density = resources.displayMetrics.density
        val width = (300 * density).toInt()
        val height = (400 * density).toInt()

        val params = WindowManager.LayoutParams(
            width, height, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(popupRootView, params)
        popupParams = params
        popupVisible = true
    }

    private fun hidePopup() {
        popupRootView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { /* already removed */ }
        }
        popupVisible = false
    }

    private fun resizePopup(width: Int, height: Int) {
        val view = popupRootView ?: return
        val params = popupParams ?: return
        val density = resources.displayMetrics.density

        if (width == -1 || height == -1) {
            // "full" state: hide overlay popup, launch the real full-screen Activity instead.
            hidePopup()
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            return
        }

        params.width = (width * density).toInt()
        params.height = (height * density).toInt()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) { /* view not attached yet */ }
    }

    override fun invokeDefaultOnBackPressed() {
        hidePopup()
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        popupRootView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            it.unmountReactApplication()
        }
    }
}
