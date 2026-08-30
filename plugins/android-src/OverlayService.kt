package com.devclip.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.facebook.react.ReactRootView
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import kotlin.math.abs
import kotlin.math.min

/**
 * Foreground service that owns two overlay windows:
 *  1. A draggable, tappable bubble showing the app icon (always visible).
 *  2. A ReactRootView window rendering "DevClipPopup", in one of two shapes.
 *
 * The popup has two geometries and native owns both, because only this side
 * knows where the bubble is and where the system bars are:
 *
 *  - MINI is tethered to the bubble. It hangs below it, left edges aligned,
 *    flips above when there is no room below, and slides inward near an
 *    edge. Dragging the bubble carries it along.
 *  - EXPANDED detaches and becomes a half-height sheet across the bottom.
 *
 * Every geometry is clamped to the area left over after the status and
 * navigation bars, so neither window is ever placed underneath them.
 */
class OverlayService : Service(), DefaultHardwareBackBtnHandler {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var bubbleSizePx = 0
    private var popupRootView: ReactRootView? = null
    private var popupParams: WindowManager.LayoutParams? = null
    private var popupVisible = false
    private var mode = MODE_MINI

    companion object {
        const val CHANNEL_ID = "devclip_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SET_MODE = "com.devclip.app.ACTION_SET_MODE"
        const val ACTION_HIDE = "com.devclip.app.ACTION_HIDE"
        const val ACTION_OPEN_FULL = "com.devclip.app.ACTION_OPEN_FULL"
        const val EXTRA_MODE = "mode"

        const val MODE_MINI = "mini"
        const val MODE_EXPANDED = "expanded"

        /** Mini is sized to show two clips; the list scrolls past that. */
        private const val MINI_WIDTH_DP = 300
        private const val MINI_HEIGHT_DP = 344
        /** Breathing room between the bubble and the window hanging off it. */
        private const val TETHER_GAP_DP = 8
        private const val EDGE_MARGIN_DP = 8

        private val overlayType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
    }

    /** The screen rectangle that is not covered by the status/navigation bars. */
    private data class SafeArea(val left: Int, val top: Int, val width: Int, val height: Int) {
        val right get() = left + width
        val bottom get() = top + height
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        addBubble()
    }

    /**
     * Rotation, unfolding, and entering multi-window all move the system bars
     * and change the usable rectangle. Both windows are positioned from that
     * rectangle, so both have to be re-placed when it changes — otherwise a
     * window that was correctly inset before the change is left overlapping a
     * system bar, or off the edge of the new configuration entirely.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val area = safeArea()

        bubbleView?.let { view ->
            bubbleParams?.let { params ->
                params.x = params.x.coerceIn(area.left, area.right - bubbleSizePx)
                params.y = params.y.coerceIn(area.top, area.bottom - bubbleSizePx)
                try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { }
            }
        }

        if (popupVisible) {
            applyPopupGeometry()
            popupRootView?.let { view ->
                popupParams?.let { params ->
                    try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_MODE -> setMode(intent.getStringExtra(EXTRA_MODE) ?: MODE_MINI)
            ACTION_HIDE -> hidePopup()
            ACTION_OPEN_FULL -> openFullApp()
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

    // ---- Geometry ----

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun safeArea(): SafeArea {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            return SafeArea(
                left = insets.left,
                top = insets.top,
                width = metrics.bounds.width() - insets.left - insets.right,
                height = metrics.bounds.height() - insets.top - insets.bottom
            )
        }
        val dm = resources.displayMetrics
        return SafeArea(0, 0, dm.widthPixels, dm.heightPixels)
    }

    // ---- Bubble ----

    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun addBubble() {
        val prefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val sizeDp = when (prefs.getString(Prefs.KEY_BUBBLE_SIZE, "medium")) {
            "small" -> 44
            "large" -> 72
            else -> 56
        }
        val size = dp(sizeDp)
        bubbleSizePx = size

        // The bubble wears the app icon. Taken from the package manager rather
        // than a copied drawable, so it always matches whatever the launcher
        // shows, and clipped to a circle because an adaptive icon draws itself
        // square when nothing applies the launcher's mask for it.
        val bubble = ImageView(this).apply {
            val icon = packageManager.getApplicationIcon(packageName)
            val rounded = RoundedBitmapDrawableFactory
                .create(resources, drawableToBitmap(icon, size))
                .apply { isCircular = true }
            setImageDrawable(rounded)
            elevation = dp(6).toFloat()
        }

        val area = safeArea()
        val params = WindowManager.LayoutParams(
            size, size, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = area.left + dp(EDGE_MARGIN_DP)
            y = area.top + dp(96)
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
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    val a = safeArea()
                    params.x = (initialX + dx).coerceIn(a.left, a.right - size)
                    params.y = (initialY + dy).coerceIn(a.top, a.bottom - size)
                    windowManager.updateViewLayout(v, params)
                    // The mini window is tethered: it travels with the bubble.
                    if (popupVisible && mode == MODE_MINI) applyPopupGeometry()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        // A tap always opens mini, never the shape it was left in.
                        if (popupVisible) hidePopup() else {
                            mode = MODE_MINI
                            showPopup()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubble, params)
        bubbleView = bubble
        bubbleParams = params
    }

    // ---- Popup (React Native content) ----

    private fun showPopup() {
        if (popupRootView == null) {
            val rootView = ReactRootView(this)
            val reactInstanceManager = (application as MainApplication)
                .reactNativeHost.reactInstanceManager
            rootView.startReactApplication(reactInstanceManager, "DevClipPopup", null)
            popupRootView = rootView
        }

        val params = WindowManager.LayoutParams(
            dp(MINI_WIDTH_DP), dp(MINI_HEIGHT_DP), overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        popupParams = params
        applyPopupGeometry()
        windowManager.addView(popupRootView, params)
        popupVisible = true
    }

    private fun hidePopup() {
        popupRootView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { /* already removed */ }
        }
        popupVisible = false
    }

    private fun setMode(next: String) {
        mode = next
        if (!popupVisible) { showPopup(); return }
        applyPopupGeometry()
        val view = popupRootView ?: return
        val params = popupParams ?: return
        try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { }
    }

    /**
     * Sizes and positions the popup for the current mode, always inside the
     * safe area. Mini hangs off the bubble and flips above it when there is
     * no room below; expanded ignores the bubble and takes the bottom half.
     */
    private fun applyPopupGeometry() {
        val params = popupParams ?: return
        val area = safeArea()

        if (mode == MODE_EXPANDED) {
            params.width = area.width
            params.height = area.height / 2
            params.x = area.left
            params.y = area.bottom - params.height
        } else {
            val width = min(dp(MINI_WIDTH_DP), area.width - dp(EDGE_MARGIN_DP) * 2)
            val height = min(dp(MINI_HEIGHT_DP), area.height - dp(EDGE_MARGIN_DP) * 2)
            val bubble = bubbleParams
            val gap = dp(TETHER_GAP_DP)

            val x = bubble?.x ?: area.left
            var y = (bubble?.y ?: area.top) + bubbleSizePx + gap

            // No room below: hang it above the bubble instead.
            if (y + height > area.bottom) {
                y = (bubble?.y ?: area.top) - height - gap
            }

            params.width = width
            params.height = height
            params.x = x.coerceIn(area.left, area.right - width)
            params.y = y.coerceIn(area.top, area.bottom - height)
        }
    }

    private fun openFullApp() {
        hidePopup()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
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
