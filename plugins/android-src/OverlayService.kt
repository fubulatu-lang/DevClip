package com.devclip.app

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.animation.PathInterpolator
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlin.math.abs
import kotlin.math.min

/**
 * Foreground service that owns DevClip's floating windows:
 *
 *  1. The bubble — the app icon, docked to the left or right edge, draggable
 *     up and down it, and the thing that captures a selection when tapped.
 *  2. The floating list, drawn in ordinary views by PopupListView.
 *  3. The drag-to-hide target, which exists only while a drag is in progress.
 *
 * Three states, not two:
 *
 *     service stopped          -> nothing
 *     service running, awake   -> bubble visible
 *     service running, resting -> bubble hidden, notification is the way back
 *
 * Resting is not persisted. Hiding the bubble means "get out of my way now",
 * not "I would rather not have a bubble" — so a reboot brings it back, and
 * turning DevClip off is a separate action with its own label.
 *
 * Native owns the list's geometry, because only this side knows where the
 * bubble is and where the system bars are. The list is tethered to the
 * bubble: it hangs below it, left edges aligned, flips above when there is no
 * room below, slides inward near an edge, and travels with the bubble while
 * it is dragged.
 *
 * There used to be a second, expanded shape — a half-height sheet across the
 * bottom, detached from the bubble. It is gone. It duplicated the full app in
 * a worse window, and its geometry branch was the hardest code here to reason
 * about; the tethered list is now sized to be worth opening on its own.
 *
 * Every geometry is clamped to the area left over after the status and
 * navigation bars, so no window is ever placed underneath them.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var bubbleSizePx = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Where the user put the bubble.
     *
     * Two positions, not one. `parkedY` is this, and it survives the keyboard.
     * `params.y` is where the bubble actually is, which the keyboard is
     * allowed to push upward. Folding the two together looks like it works
     * and then loses the user's position the first time a keyboard appears:
     * the bubble slides up, the keyboard goes away, and it stays wherever the
     * keyboard left it. Displacement is never written down anywhere.
     *
     * A drag is the one thing that changes it, keyboard up or not.
     */
    private var parkedY = 0

    /**
     * Which edge the bubble is docked to, and how far down it sits.
     *
     * Stored as an edge and a fraction rather than as pixels, because pixels
     * stop meaning anything the moment the window changes shape — rotation,
     * split-screen, a foldable opening. A fraction lands in the same relative
     * place on any of them.
     *
     * They live in SharedPreferences, not in a JS store, because the service
     * needs them at startup and can be started by BootReceiver long before any
     * React context exists. There is deliberately no setting for either:
     * dragging the bubble is the only way to move it.
     */
    private var edge = Prefs.EDGE_RIGHT
    private var yFraction = Prefs.DEFAULT_Y_FRACTION

    /**
     * Running with the bubble hidden.
     *
     * Not persisted, on purpose. Hiding the bubble means "get out of my way
     * now", not "I would prefer not to have a bubble" — so a reboot brings it
     * back, and turning DevClip off is a separate, clearly-labelled action.
     */
    private var resting = false

    /**
     * Tucked away at the edge as a handle.
     *
     * A third visible state alongside awake and resting, and unlike resting it
     * is not a decision the user made — the bubble did it on a timer. So it
     * undoes itself on a touch rather than needing the notification, and it is
     * never persisted.
     */
    private var tucked = false
    private var handleView: View? = null
    private var handleParams: WindowManager.LayoutParams? = null

    private var dismissTarget: DismissTargetView? = null
    private var dismissParams: WindowManager.LayoutParams? = null
    private var magnetised = false

    private var bubbleAnimator: ValueAnimator? = null
    private var popupContent: PopupListView? = null
    private var popupView: ResizableFrame? = null
    private var popupParams: WindowManager.LayoutParams? = null
    private var popupVisible = false

    /**
     * The size the user last dragged the list to, in pixels.
     *
     * Held here as well as in SharedPreferences because a resize drag updates
     * it on every move event and the preference is written once, on release.
     */
    private var popupWidthPx = 0
    private var popupHeightPx = 0

    /** Window geometry captured when a resize drag begins. */
    private var resizeStartWidth = 0
    private var resizeStartHeight = 0
    private var resizeStartX = 0
    private var resizeStartY = 0

    companion object {
        const val CHANNEL_ID = "devclip_overlay_channel"
        const val NOTIFICATION_ID = 1001
        /**
         * Closes the floating list. The bubble stays where it is.
         *
         * Named for the window it closes, not just "hide": hiding the
         * *bubble* is a separate thing the service is about to grow, and one
         * constant called ACTION_HIDE would be read as whichever the reader
         * had in mind.
         */
        const val ACTION_HIDE_POPUP = "com.devclip.app.ACTION_HIDE_POPUP"
        const val ACTION_OPEN_FULL = "com.devclip.app.ACTION_OPEN_FULL"

        /** Hide the bubble. The service keeps running and keeps its notification. */
        const val ACTION_REST = "com.devclip.app.ACTION_REST"

        /** Bring the bubble back, at the position the user left it. */
        const val ACTION_WAKE = "com.devclip.app.ACTION_WAKE"

        /** Turn DevClip's bubble off entirely. */
        const val ACTION_STOP = "com.devclip.app.ACTION_STOP"

        /** Resize the bubble in place, without tearing it down. */
        const val ACTION_SET_BUBBLE_SIZE = "com.devclip.app.ACTION_SET_BUBBLE_SIZE"
        const val EXTRA_SIZE_DP = "size_dp"

        /**
         * Re-read the appearance settings and apply them.
         *
         * One action rather than one per setting. The values live in
         * SharedPreferences because the service needs them at startup, long
         * before any React context exists — so the writer has already written
         * them by the time this arrives, and every extra action would only be
         * a second copy of a number the service can already see.
         */
        const val ACTION_APPLY_SETTINGS = "com.devclip.app.ACTION_APPLY_SETTINGS"

        /**
         * The floating list is the only floating surface now that the expanded
         * sheet is gone, so it is sized to be worth opening — roughly a third
         * of a phone screen, scrolling past that — rather than to be the
         * smaller of two options.
         */
        private const val LIST_WIDTH_DP = 320
        private const val LIST_HEIGHT_DP = 460

        /**
         * How far the list can be dragged in each direction.
         *
         * The floor is the point below which a clip row stops being readable
         * at all; the ceiling is checked again against the safe area every
         * time the window is placed, so a size dragged on a tablet cannot
         * strand the window off the edge of a phone.
         */
        private const val LIST_MIN_WIDTH_DP = 220
        private const val LIST_MIN_HEIGHT_DP = 160
        private const val LIST_MAX_WIDTH_DP = 560
        private const val LIST_MAX_HEIGHT_DP = 900
        /** Breathing room between the bubble and the window hanging off it. */
        private const val TETHER_GAP_DP = 8
        private const val EDGE_MARGIN_DP = 8

        /**
         * Thickness of the ring that says "a selection is live, tapping will
         * capture it".
         *
         * The icon is always inset by this much, ring or no ring, so the ring
         * appearing does not make the icon jump smaller.
         */
        private const val SELECTION_RING_DP = 3

        /** How long the bubble takes to settle onto its edge. */
        private const val SETTLE_DURATION_MS = 220L

        /**
         * How long after the last touch the bubble drops to its faded level.
         *
         * Fixed rather than configurable. The setting the user wants control
         * over is how see-through it becomes, not the handful of seconds
         * before it gets there, and every extra dial is one more thing to
         * explain in a settings screen.
         */
        private const val IDLE_FADE_DELAY_MS = 3000L

        /** How long the bubble takes to fade, and to slide away or back. */
        private const val FADE_DURATION_MS = 180L

        /**
         * How long after a drag the parked position is written.
         *
         * A few quick drags in a row are one decision, and each write here is
         * a disk write.
         */
        private const val POSITION_WRITE_DELAY_MS = 400L

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

        // The ring is the only advertisement tap-to-capture gets. Events
        // arrive on the accessibility service's thread, so the hop to the main
        // thread is not optional — a window change from any other thread is a
        // crash.
        SelectionCapture.listener = { live ->
            mainHandler.post { showSelectionRing(live) }
        }

        // Arrives on the accessibility service's thread.
        ImeWatcher.listener = {
            mainHandler.post { applyKeyboardAvoidance() }
        }
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
                // Re-derived from the edge and the fraction rather than
                // clamped from the old pixels. That is what storing a fraction
                // buys: a bubble a quarter of the way down stays a quarter of
                // the way down when the phone turns, unfolds, or is put into
                // split screen, instead of being squeezed to whatever fits.
                parkedY = yFromFraction(area)
                params.x = edgeX(area)
                params.y = resolvedY(area)
                try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { }
            }
        }

        // The handle is docked to an edge like the bubble it replaced, so a
        // rotation or an unfold moves it for the same reasons.
        if (tucked) {
            removeHandle()
            addHandle()
        }

        // The target window is sized to the safe area, so it has to be
        // re-measured for the new one.
        hideDismissTarget()
        resizeDismissTarget(area)

        if (popupVisible) {
            applyPopupGeometry()
            updatePopupLayout()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE_POPUP -> hidePopup()
            ACTION_OPEN_FULL -> openFullApp()
            ACTION_REST -> rest()
            ACTION_WAKE -> wake()
            ACTION_STOP -> turnOff()
            ACTION_SET_BUBBLE_SIZE ->
                applyBubbleSize(intent.getIntExtra(EXTRA_SIZE_DP, Prefs.DEFAULT_BUBBLE_SIZE_DP))
            ACTION_APPLY_SETTINGS -> applySettings()
        }
        return START_STICKY
    }

    // ---- Notification ----

    /**
     * The notification is load-bearing now: it is the way back to a hidden
     * bubble, and it was not built for that.
     *
     * IMPORTANCE_LOW, not IMPORTANCE_MIN. MIN gets collapsed into the silent
     * section and its action buttons frequently do not render at all — which
     * would leave the "Show bubble" button, the accessible equivalent of the
     * drag gesture, sometimes simply absent. LOW is still silent and still
     * never a heads-up, but it renders normally with its actions.
     *
     * setOngoing resists a swipe-away, but Android 13 lets the user dismiss a
     * foreground-service notification regardless and that has drifted across
     * releases. So this is a convenience, never the only route: turning the
     * bubble back on from inside DevClip is first-class.
     *
     * "Dismiss" is deliberately absent from the copy. It implies a permanence
     * this gesture does not have. Hide, Show and Turn off say what actually
     * happens.
     */
    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.devclip_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = getString(R.string.devclip_notification_channel_description)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.devclip_notification_title))
            .setContentText(
                getString(
                    if (resting) R.string.devclip_notification_text_resting
                    else R.string.devclip_notification_text_awake
                )
            )
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .setContentIntent(activityIntent())

        if (resting) {
            builder.addAction(
                0,
                getString(R.string.devclip_notification_show),
                serviceIntent(ACTION_WAKE, 2)
            )
        } else {
            // Also the accessible equivalent of drag-to-hide, which would
            // otherwise be a gesture with no non-gesture counterpart.
            builder.addAction(
                0,
                getString(R.string.devclip_notification_hide),
                serviceIntent(ACTION_REST, 1)
            )
        }
        builder.addAction(
            0,
            getString(R.string.devclip_notification_turn_off),
            serviceIntent(ACTION_STOP, 3)
        )

        return builder.build()
    }

    private fun refreshNotification() {
        try {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // Notifications are blocked. The in-app route still works, which
            // is why it is not a fallback.
            android.util.Log.w("DevClip", "Could not update the notification", e)
        }
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, OverlayService::class.java).apply { this.action = action }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            PendingIntent.getForegroundService(this, requestCode, intent, pendingFlags())
        else
            PendingIntent.getService(this, requestCode, intent, pendingFlags())
    }

    private fun activityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(this, 0, intent, pendingFlags())
    }

    private fun notificationsAllowed(): Boolean =
        try {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }

    // ---- Geometry ----

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * coerceIn throws IllegalArgumentException on an inverted range, and a
     * window wider than the space left for it produces exactly that. Every
     * clamp here is against a safe area that a split-screen or freeform window
     * can shrink below the window being placed, so none of them may throw.
     */
    private fun clamp(value: Int, min: Int, max: Int): Int =
        if (max <= min) min else value.coerceIn(min, max)

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

    /**
     * Builds the bubble: the app icon, inset far enough to leave room for a
     * ring around it. Size comes from the window's layout params, not from
     * here, so the bubble can be resized without being rebuilt.
     *
     * The icon comes from the package manager rather than a copied drawable,
     * so it always matches whatever the launcher shows, and is clipped to a
     * circle because an adaptive icon draws itself square when nothing applies
     * the launcher's mask for it.
     */
    private fun buildBubbleView(): FrameLayout {
        val inset = dp(SELECTION_RING_DP)
        // Rasterised once, at the largest size the slider allows, and scaled
        // down by the ImageView. Redrawing the bitmap on every size change
        // would mean redrawing it on every pixel of a slider drag.
        val iconSize = (dp(Prefs.MAX_BUBBLE_SIZE_DP) - inset * 2).coerceAtLeast(1)

        val icon = ImageView(this).apply {
            val drawable = packageManager.getApplicationIcon(packageName)
            val rounded = RoundedBitmapDrawableFactory
                .create(resources, drawableToBitmap(drawable, iconSize))
                .apply { isCircular = true }
            setImageDrawable(rounded)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        return FrameLayout(this).apply {
            elevation = dp(6).toFloat()
            // Constant padding, ring or no ring: the ring is a background, so
            // showing it must not resize the icon inside it.
            setPadding(inset, inset, inset, inset)
            addView(
                icon,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    /**
     * The ring drawn around the bubble while a selection is live.
     *
     * Black and white, always, whatever the theme. It used to be the theme's
     * accent over a dark hairline, which worked until the design system went
     * monochrome and "accent" became near-black in light mode — a black ring
     * on a dark app underneath is no ring at all.
     *
     * The bubble floats over other people's apps and DevClip has no say in
     * what is behind it. A dark stroke outside a white one is the one
     * combination that survives any background: whichever half disappears,
     * the other is at full contrast against it.
     */
    private fun selectionRing(): Drawable {
        val stroke = dp(SELECTION_RING_DP)

        val outline = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(stroke + dp(1), Color.argb(140, 0, 0, 0))
        }
        val ring = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(stroke, Color.WHITE)
        }
        return LayerDrawable(arrayOf<Drawable>(outline, ring))
    }

    /** Advertise capture rather than hiding it behind a gesture nobody tried. */
    private fun showSelectionRing(live: Boolean) {
        val view = bubbleView ?: return
        view.background = if (live) selectionRing() else null
    }

    /**
     * A short flash, so a capture is felt and seen as well as read.
     *
     * Skipped when the user has animations turned off — the haptic and the
     * message still land, which is what actually carries the confirmation.
     */
    private fun flashBubble() {
        val view = bubbleView ?: return
        if (animationsDisabled()) return
        view.animate().cancel()
        view.alpha = 1f
        view.animate().alpha(0.3f).setDuration(90).withEndAction {
            view.animate().alpha(1f).setDuration(150).start()
        }.start()
    }

    private fun animationsDisabled(): Boolean =
        try {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }

    private fun buzz() {
        val view = bubbleView ?: return
        val feedback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
        try {
            view.performHapticFeedback(feedback)
        } catch (e: Exception) {
            // Haptics are off, or the device has no vibrator. Not worth a log.
        }
    }

    // ---- Where the bubble lives ----

    private fun prefs() = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    private fun loadPosition() {
        val stored = prefs()
        edge = if (stored.getString(Prefs.KEY_BUBBLE_EDGE, Prefs.EDGE_RIGHT) == Prefs.EDGE_LEFT)
            Prefs.EDGE_LEFT else Prefs.EDGE_RIGHT
        yFraction = stored.getFloat(Prefs.KEY_BUBBLE_Y_FRACTION, Prefs.DEFAULT_Y_FRACTION)
            .coerceIn(0f, 1f)
    }

    /**
     * Writes the parked position, once the drag has settled.
     *
     * Debounced because a flurry of drags in quick succession is one decision,
     * not several, and each one of these is a disk write.
     */
    private val persistPosition = Runnable {
        prefs().edit()
            .putString(Prefs.KEY_BUBBLE_EDGE, edge)
            .putFloat(Prefs.KEY_BUBBLE_Y_FRACTION, yFraction)
            .apply()
    }

    private fun schedulePersistPosition() {
        mainHandler.removeCallbacks(persistPosition)
        mainHandler.postDelayed(persistPosition, POSITION_WRITE_DELAY_MS)
    }

    /** The bubble is docked, so its x is decided by which edge, not by the drag. */
    private fun edgeX(area: SafeArea): Int =
        if (edge == Prefs.EDGE_LEFT) area.left + dp(EDGE_MARGIN_DP)
        else area.right - bubbleSizePx - dp(EDGE_MARGIN_DP)

    private fun yFromFraction(area: SafeArea): Int {
        val travel = (area.height - bubbleSizePx).coerceAtLeast(0)
        return area.top + (travel * yFraction).toInt()
    }

    private fun fractionFromY(area: SafeArea, y: Int): Float {
        val travel = (area.height - bubbleSizePx).coerceAtLeast(1)
        return ((y - area.top).toFloat() / travel).coerceIn(0f, 1f)
    }

    // ---- Parked vs displaced ----

    /**
     * The lowest the bubble may sit right now.
     *
     * With a keyboard up that is above the keys, not above the navigation bar:
     * TYPE_APPLICATION_OVERLAY draws *over* the IME, so a bubble parked low
     * lands on top of the keys rather than behind them.
     */
    private fun bubbleMaxY(area: SafeArea): Int {
        val floor = area.bottom - bubbleSizePx
        val imeTop = ImeWatcher.imeTopPx
        if (imeTop <= 0) return floor
        return min(floor, imeTop - bubbleSizePx - dp(EDGE_MARGIN_DP))
    }

    /** Where the bubble should be, given where it is parked and the keyboard. */
    private fun resolvedY(area: SafeArea): Int =
        clamp(parkedY, area.top, bubbleMaxY(area))

    /**
     * Moves the bubble out of the keyboard's way, and back again afterwards.
     *
     * The parked position is not touched. When the keyboard goes, the bubble
     * returns to exactly where the user left it — which is the entire reason
     * there are two positions rather than one.
     */
    private fun applyKeyboardAvoidance() {
        val params = bubbleParams ?: return
        val area = safeArea()
        val target = resolvedY(area)
        if (params.y == target) return
        animateBubbleTo(params.x, target)
    }

    private fun animateBubbleTo(targetX: Int, targetY: Int) {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return

        bubbleAnimator?.cancel()

        if (animationsDisabled()) {
            params.x = targetX
            params.y = targetY
            pushBubbleLayout(view, params)
            return
        }

        val fromX = params.x
        val fromY = params.y
        bubbleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SETTLE_DURATION_MS
            // One UI's standard curve, the same one the JS side animates on.
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { animation ->
                val t = animation.animatedValue as Float
                params.x = (fromX + (targetX - fromX) * t).toInt()
                params.y = (fromY + (targetY - fromY) * t).toInt()
                pushBubbleLayout(view, params)
            }
            start()
        }
    }

    /** Moves the bubble, and the list hanging off it, in one step. */
    private fun pushBubbleLayout(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            // The window is gone; the animation will finish harmlessly.
            return
        }
        if (popupVisible) {
            applyPopupGeometry()
            updatePopupLayout()
        }
    }

    // ---- Resizing, live ----

    /**
     * Changes the bubble's size without tearing it down and rebuilding it.
     *
     * The old setter restarted the whole service. That was survivable when the
     * size was three fixed choices; against a slider it would have torn the
     * bubble down and rebuilt it on every pixel of the drag. The icon is
     * rasterised once at the largest size the slider allows and scaled by the
     * ImageView, so a resize is a layout change and nothing more.
     */
    private fun applyBubbleSize(sizeDp: Int) {
        val clamped = sizeDp.coerceIn(Prefs.MIN_BUBBLE_SIZE_DP, Prefs.MAX_BUBBLE_SIZE_DP)
        bubbleSizePx = dp(clamped)

        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        val area = safeArea()

        params.width = bubbleSizePx
        params.height = bubbleSizePx
        parkedY = yFromFraction(area)
        params.x = edgeX(area)
        params.y = resolvedY(area)
        pushBubbleLayout(view, params)
    }

    // ---- Hiding and showing the bubble ----

    /**
     * Hides the bubble. The service stays up, and so does the notification —
     * which is now the way back.
     */
    private fun rest() {
        if (resting) return
        resting = true
        mainHandler.removeCallbacks(fadeToIdle)
        mainHandler.removeCallbacks(tuckAway)
        tucked = false
        removeHandle()
        // Hiding the bubble hides what hangs off it. Leaving the list floating
        // with nothing to be tethered to would be its own bug.
        hidePopup()
        removeBubble()
        refreshNotification()
        DevClipEvents.emitBubbleState(true)

        // A Toast, not a message in a window: the windows were just torn down.
        // The copy has to adapt, because with notifications blocked there is
        // no notification panel to send anyone to.
        toast(
            getString(
                if (notificationsAllowed()) R.string.devclip_bubble_hidden
                else R.string.devclip_bubble_hidden_no_notification
            )
        )
    }

    private fun wake() {
        if (!resting) return
        resting = false
        tucked = false
        removeHandle()
        addBubble()
        refreshNotification()
        DevClipEvents.emitBubbleState(false)
    }

    private fun turnOff() {
        prefs().edit().putBoolean(Prefs.KEY_BUBBLE_RUNNING, false).apply()
        stopSelf()
    }

    private fun removeBubble() {
        bubbleAnimator?.cancel()
        bubbleView?.let { try { windowManager.removeView(it) } catch (e: Exception) { } }
        bubbleView = null
        bubbleParams = null
        removeDismissTarget()
    }

    /**
     * Applies changed appearance settings to windows that already exist.
     *
     * A tucked bubble is brought back first: the settings screen is the one
     * place the user is definitely looking at DevClip rather than at the app
     * underneath it, and leaving the bubble hidden while they drag a slider
     * meant to change how it looks would show them nothing at all.
     */
    private fun applySettings() {
        if (tucked) untuck()
        applyBubbleAlpha(idle = false)
        popupView?.alpha = popupAlpha()
        scheduleIdle()
    }

    // ---- Transparency and tucking away ----

    private val fadeToIdle = Runnable { applyBubbleAlpha(idle = true) }
    private val tuckAway = Runnable { tuck() }

    /** The level the user set, as a fraction. */
    private fun bubbleAlpha(): Float =
        prefs().getInt(Prefs.KEY_BUBBLE_ALPHA, Prefs.DEFAULT_ALPHA)
            .coerceIn(Prefs.MIN_ALPHA, 100) / 100f

    private fun idleFadeEnabled(): Boolean =
        prefs().getBoolean(Prefs.KEY_BUBBLE_IDLE_FADE, false)

    /**
     * Puts the bubble at the right opacity for what is happening.
     *
     * With idle fade off, the chosen level is simply how the bubble looks, all
     * the time. With it on, the level is where the bubble *rests* and a touch
     * brings it back to solid — which is the behaviour that makes a very
     * transparent bubble usable at all, because you can see what you are
     * about to press the moment you press it.
     */
    private fun applyBubbleAlpha(idle: Boolean) {
        val view = bubbleView ?: return
        val target = when {
            !idleFadeEnabled() -> bubbleAlpha()
            idle -> bubbleAlpha()
            else -> 1f
        }
        if (view.alpha == target) return
        if (animationsDisabled()) {
            view.alpha = target
        } else {
            view.animate().alpha(target).setDuration(FADE_DURATION_MS).start()
        }
    }

    /**
     * Called whenever the user touches the bubble.
     *
     * Restores full opacity and restarts both timers, so a bubble in use never
     * fades or tucks itself out from under the finger using it.
     */
    private fun onBubbleInteraction() {
        mainHandler.removeCallbacks(fadeToIdle)
        mainHandler.removeCallbacks(tuckAway)
        applyBubbleAlpha(idle = false)
    }

    /** Restarts the idle timers after an interaction has finished. */
    private fun scheduleIdle() {
        mainHandler.removeCallbacks(fadeToIdle)
        mainHandler.removeCallbacks(tuckAway)
        if (resting || tucked) return
        if (idleFadeEnabled()) mainHandler.postDelayed(fadeToIdle, IDLE_FADE_DELAY_MS)
        val delay = prefs().getInt(Prefs.KEY_TUCK_DELAY_SEC, Prefs.DEFAULT_TUCK_DELAY_SEC)
        if (delay > 0) mainHandler.postDelayed(tuckAway, delay * 1000L)
    }

    /**
     * Replaces the bubble with a slim handle on the screen edge.
     *
     * Not the same thing as resting. Resting is the user saying "get out of my
     * way" and is undone from the notification; this is the bubble getting out
     * of its own way on a timer, and a touch on the handle is enough to undo
     * it. The list is closed first for the same reason resting closes it:
     * something tethered to a bubble that is no longer there is its own bug.
     */
    private fun tuck() {
        if (tucked || resting) return
        if (popupVisible) return
        tucked = true
        hidePopup()
        removeBubble()
        addHandle()
    }

    /** Brings the bubble back, where the handle was. */
    private fun untuck() {
        if (!tucked) return
        tucked = false
        removeHandle()
        addBubble()
        onBubbleInteraction()
        scheduleIdle()
    }

    private fun addHandle() {
        val area = safeArea()
        val onLeft = edge == Prefs.EDGE_LEFT
        val view = EdgeHandleView(this, onLeft).apply {
            contentDescription = getString(R.string.devclip_handle_description)
            isClickable = true
            setOnClickListener { untuck() }
        }

        val width = dp(EdgeHandleView.TOUCH_WIDTH_DP)
        val height = dp(EdgeHandleView.HEIGHT_DP)
        val params = WindowManager.LayoutParams(
            width, height, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (onLeft) area.left else area.right - width
            // Centred on where the bubble was, so the handle appears where the
            // user last left the thing it replaced.
            y = clamp(
                parkedY + bubbleSizePx / 2 - height / 2,
                area.top,
                area.bottom - height
            )
        }

        try {
            windowManager.addView(view, params)
            handleView = view
            handleParams = params
        } catch (e: Exception) {
            // Losing the handle would leave no way back to the bubble at all,
            // so failing to place it undoes the tuck rather than stranding it.
            android.util.Log.w("DevClip", "Could not place the edge handle", e)
            tucked = false
            addBubble()
        }
    }

    private fun removeHandle() {
        handleView?.let { try { windowManager.removeView(it) } catch (e: Exception) { } }
        handleView = null
        handleParams = null
    }

    // ---- The drag-to-hide target ----

    /**
     * Creates the target's window, hidden, *before* the bubble's.
     *
     * Order matters and there is no way round it. Two windows of the same type
     * stack by the order they were added, and an app on Android 8 and up gets
     * exactly one type to work with — so a target window added when the drag
     * starts would be added last and would draw over the bubble, hiding the
     * very thing being dragged behind its own scrim. Adding it first, empty
     * and not touchable, and merely showing its contents when a drag begins,
     * puts it where it belongs underneath.
     */
    private fun ensureDismissTarget() {
        if (dismissTarget != null) return
        val area = safeArea()
        val view = DismissTargetView(this).apply { visibility = View.GONE }
        val params = WindowManager.LayoutParams(
            area.width, area.height, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = area.left
            y = area.top
        }
        try {
            windowManager.addView(view, params)
            dismissTarget = view
            dismissParams = params
        } catch (e: Exception) {
            // No target means no drag-to-hide. The notification's Hide action
            // and the switch in Settings both still work, which is why the
            // gesture is not the only way to do this.
            android.util.Log.w("DevClip", "Could not create the hide target", e)
        }
    }

    private fun showDismissTarget() {
        val view = dismissTarget ?: return
        view.engaged = false
        view.visibility = View.VISIBLE
    }

    private fun hideDismissTarget() {
        magnetised = false
        val view = dismissTarget ?: return
        view.engaged = false
        view.visibility = View.GONE
    }

    private fun removeDismissTarget() {
        dismissTarget?.let { try { windowManager.removeView(it) } catch (e: Exception) { } }
        dismissTarget = null
        dismissParams = null
        magnetised = false
    }

    private fun resizeDismissTarget(area: SafeArea) {
        val view = dismissTarget ?: return
        val params = dismissParams ?: return
        params.width = area.width
        params.height = area.height
        params.x = area.left
        params.y = area.top
        try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { }
    }

    /**
     * Pulls the bubble into the target when the drag gets close.
     *
     * Magnetising moves the bubble *into* the target, so releasing while
     * magnetised is releasing inside it, not merely near it. That is the point
     * of the pull: the commit is visible and felt before the finger lifts,
     * rather than being discovered afterwards.
     *
     * Returns true when the bubble is being held by the target.
     */
    private fun updateMagnet(params: WindowManager.LayoutParams): Boolean {
        val target = dismissTarget ?: return false
        val origin = dismissParams ?: return false

        val centreX = params.x + bubbleSizePx / 2f
        val centreY = params.y + bubbleSizePx / 2f
        val targetX = origin.x + target.circleCenterX
        val targetY = origin.y + target.circleCenterY
        val dx = centreX - targetX
        val dy = centreY - targetY
        val near = kotlin.math.hypot(dx, dy) <= target.magnetRadiusPx

        if (near != magnetised) {
            magnetised = near
            target.engaged = near
            // On entering, not on release: the user should feel the commit
            // while they can still change their mind about it.
            if (near) buzz()
        }

        if (near) {
            params.x = (targetX - bubbleSizePx / 2f).toInt()
            params.y = (targetY - bubbleSizePx / 2f).toInt()
        }
        return near
    }

    private fun addBubble() {
        loadPosition()
        bubbleSizePx = dp(
            prefs().getInt(Prefs.KEY_BUBBLE_SIZE_DP, Prefs.DEFAULT_BUBBLE_SIZE_DP)
                .coerceIn(Prefs.MIN_BUBBLE_SIZE_DP, Prefs.MAX_BUBBLE_SIZE_DP)
        )
        val size = bubbleSizePx

        // Before the bubble's window, so it stacks underneath it.
        ensureDismissTarget()

        val bubble = buildBubbleView()

        val area = safeArea()
        parkedY = yFromFraction(area)
        val params = WindowManager.LayoutParams(
            size, size, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = edgeX(area)
            y = resolvedY(area)
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        var longPressFired = false

        // Android's own tap/drag threshold, in pixels for this display.
        //
        // This used to be the literal 8, which is 8 *pixels* — about 3dp on a
        // typical phone, against the 8dp Android itself allows a finger to
        // wander during a tap. A finger moves 10-20px on an ordinary tap, so
        // every tap was classified as a drag, `moved` was true at ACTION_UP,
        // and the branch that opens the popup never ran. The bubble dragged
        // perfectly and tapping it did nothing whatsoever.
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        // Long press always opens the list, even with text selected. Without
        // it there would be no way to reach the list at all while a selection
        // is live, and holding is the same gesture the rest of Android uses
        // for "the other thing this control does".
        val longPress = Runnable {
            longPressFired = true
            buzz()
            try {
                if (!popupVisible) showPopup()
            } catch (e: Exception) {
                fail(getString(R.string.devclip_error_open_window), e)
            }
        }

        // actionMasked, not action: getAction() packs the pointer index into
        // its high bits, so a second finger landing on the bubble makes the
        // raw value stop matching ACTION_UP.
        bubble.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    bubbleAnimator?.cancel()
                    onBubbleInteraction()
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    longPressFired = false
                    mainHandler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        moved = true
                        mainHandler.removeCallbacks(longPress)
                        // Only once a drag has actually started. Flashing a
                        // dismiss target under every tap would be alarming.
                        showDismissTarget()
                    }
                    if (!moved) return@setOnTouchListener true

                    val a = safeArea()
                    // Free in both axes during the drag — the snap to an edge
                    // happens on release. A bubble that could not leave its
                    // rail could never reach the bottom-centre target.
                    params.x = clamp(initialX + dx, a.left, a.right - bubbleSizePx)
                    // Clamped to the band the user can actually see. Dragging
                    // the bubble under a raised keyboard would put it
                    // somewhere they cannot reach it again.
                    params.y = clamp(initialY + dy, a.top, bubbleMaxY(a))
                    updateMagnet(params)
                    pushBubbleLayout(v, params)
                    true
                }
                // The system took the gesture away (a notification shade pull,
                // another window). Not a tap, and not a drag to finish.
                MotionEvent.ACTION_CANCEL -> {
                    moved = true
                    mainHandler.removeCallbacks(longPress)
                    hideDismissTarget()
                    settle()
                    scheduleIdle()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    mainHandler.removeCallbacks(longPress)
                    if (moved) {
                        val hide = magnetised
                        hideDismissTarget()
                        if (hide) {
                            // Posted, not called: rest() tears down this very
                            // view's window, and doing that from inside its
                            // own touch dispatch is asking for trouble.
                            mainHandler.post { rest() }
                        } else {
                            park(params)
                        }
                    } else if (!longPressFired) {
                        // An exception thrown from here escapes view dispatch and
                        // takes the whole touch listener down with it — the bubble
                        // would still be drawn but would stop responding, with no
                        // visible sign of why. Nothing a tap does is worth that.
                        try {
                            onBubbleTapped()
                        } catch (e: Exception) {
                            // Every failure here used to look identical to a
                            // tap that was never registered. Say something.
                            fail(getString(R.string.devclip_error_tap), e)
                        }
                    }
                    scheduleIdle()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubble, params)
        bubbleView = bubble
        bubbleParams = params
        showSelectionRing(SelectionCapture.hasLiveSelection)
        applyBubbleAlpha(idle = false)
        scheduleIdle()
    }

    /**
     * Where a released drag ends up: docked to the nearer edge, at the height
     * it was dropped.
     *
     * The bubble lives on an edge. Letting it stop anywhere would put it in
     * the middle of whatever the user is reading, and it is the edge-docking
     * that makes the bottom-centre hide target safe to have at all.
     */
    private fun park(params: WindowManager.LayoutParams) {
        val area = safeArea()
        val centre = params.x + bubbleSizePx / 2
        edge = if (centre < area.left + area.width / 2) Prefs.EDGE_LEFT else Prefs.EDGE_RIGHT
        parkedY = params.y
        yFraction = fractionFromY(area, parkedY)
        schedulePersistPosition()
        animateBubbleTo(edgeX(area), resolvedY(area))
    }

    /** Return the bubble to where it belongs after an interrupted drag. */
    private fun settle() {
        val params = bubbleParams ?: return
        val area = safeArea()
        animateBubbleTo(edgeX(area), resolvedY(area))
    }

    /**
     * A tap means "capture", if there is anything to capture.
     *
     * If there is not, it means "show me my clips" — which is what the bubble
     * meant before capture existed, and what a user who taps it with nothing
     * selected is asking for.
     */
    private fun onBubbleTapped() {
        when (val outcome = Capture.attempt(this)) {
            is Capture.Outcome.Saved -> {
                buzz()
                flashBubble()
                showSelectionRing(false)
                toast(outcome.message)
            }
            is Capture.Outcome.Duplicate -> {
                showSelectionRing(false)
                toast(getString(R.string.devclip_capture_duplicate))
            }
            is Capture.Outcome.Password -> {
                showSelectionRing(false)
                toast(getString(R.string.devclip_capture_password))
            }
            is Capture.Outcome.Failed -> {
                showSelectionRing(false)
                toast(getString(R.string.devclip_capture_failed))
            }
            is Capture.Outcome.NoSelection -> {
                if (popupVisible) hidePopup() else showPopup()
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ---- Popup (native content) ----

    /**
     * Builds the popup's view once and keeps it for the life of the service.
     *
     * This used to create a React Native surface, and that is the bug the user
     * saw as "the bubble opens an empty outline". A surface draws nothing
     * without a live React instance behind it, and nothing starts one but an
     * Activity — so after a reboot, or after DevClip was swiped out of
     * Recents, the window was added and stayed blank until the launcher app
     * had been opened once. Starting the host here first was tried and did not
     * fix it: the host starts asynchronously, so the surface was still created
     * against an instance that did not exist yet.
     *
     * Views have none of that lifecycle. They draw when they are added.
     */
    private fun ensurePopupView(): ResizableFrame? {
        popupView?.let { return it }

        val content = PopupListView(this).apply {
            // Posted, not called. Both of these tear down the very window the
            // click is being dispatched inside, and removing a window from
            // within its own touch dispatch is the same trouble the drag-to-
            // hide gesture already avoids this way.
            onOpenFullApp = { mainHandler.post { this@OverlayService.openFullApp() } }
            onClose = { mainHandler.post { this@OverlayService.hidePopup() } }
            onPaste = { text -> this@OverlayService.pasteFromPopup(text) }
        }

        val frame = ResizableFrame(this).apply {
            addView(
                content,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            onResizeStart = { this@OverlayService.beginPopupResize() }
            onResizeEnd = { this@OverlayService.endPopupResize() }
            resizeListener = ResizableFrame.ResizeListener { l, t, r, b, dx, dy ->
                this@OverlayService.applyPopupResize(l, t, r, b, dx, dy)
            }
        }

        frame.alpha = popupAlpha()
        popupContent = content
        popupView = frame
        return frame
    }

    /** The list's transparency, as the user set it. */
    private fun popupAlpha(): Float =
        prefs().getInt(Prefs.KEY_POPUP_ALPHA, Prefs.DEFAULT_ALPHA)
            .coerceIn(Prefs.MIN_ALPHA, 100) / 100f

    /**
     * Reads the clips and hands them to the list.
     *
     * On the main thread, like the capture that writes them: both are the
     * direct consequence of a tap on the bubble, both are bounded reads of a
     * small table, and a background thread here would mean the window
     * appearing before its contents on every single open.
     */
    private fun refreshPopupContent() {
        val content = popupContent ?: return
        val limit = prefs().getInt(Prefs.KEY_MAX_CLIPS, Prefs.DEFAULT_MAX_CLIPS)
            .let { if (it <= 0) PopupListView.MAX_ROWS else min(it, PopupListView.MAX_ROWS) }

        val helper = DevClipDatabaseHelper(applicationContext)
        val clips = try {
            helper.listClips(limit)
        } finally {
            try { helper.close() } catch (e: Exception) { }
        }
        content.render(clips, prefs().getBoolean(Prefs.KEY_CONFIRM_BEFORE_PASTE, true))
    }

    /**
     * Hands a clip back to whatever the user was doing.
     *
     * The clipboard is written either way; the direct paste is the part that
     * can fail, when there is no focused field or it does not accept a paste.
     * Saying so is the difference between "nothing happened" and "it is on
     * your clipboard, paste it yourself".
     */
    private fun pasteFromPopup(text: String) {
        val service = ClipboardAccessibilityService.instance
        val pasted = service?.pasteIntoFocusedField(text) ?: false
        if (!pasted) {
            if (service == null) {
                // Without the accessibility service there is no paste at all,
                // so the clipboard write has to happen here instead.
                try {
                    val manager = getSystemService(Context.CLIPBOARD_SERVICE)
                        as? android.content.ClipboardManager
                    manager?.setPrimaryClip(
                        android.content.ClipData.newPlainText("DevClip", text)
                    )
                } catch (e: Exception) {
                    android.util.Log.w("DevClip", "Could not set the clipboard", e)
                }
            }
            toast(getString(R.string.devclip_paste_copied_only))
        }
    }

    // ---- Popup resizing ----

    private fun beginPopupResize() {
        val params = popupParams ?: return
        resizeStartWidth = params.width
        resizeStartHeight = params.height
        resizeStartX = params.x
        resizeStartY = params.y
        buzz()
    }

    /**
     * Applies a resize drag to the window.
     *
     * Dragging the left or top edge moves the window as well as resizing it,
     * because the opposite edge has to stay where it is — otherwise pulling
     * the left edge would drag the whole list leftward instead of widening it.
     * Both are clamped before either is committed, so a drag that runs past a
     * limit stops growing rather than sliding the window sideways.
     */
    private fun applyPopupResize(
        left: Boolean, top: Boolean, right: Boolean, bottom: Boolean, dx: Int, dy: Int
    ) {
        val params = popupParams ?: return
        val view = popupView ?: return
        val area = safeArea()

        var width = resizeStartWidth
        var height = resizeStartHeight
        var x = resizeStartX
        var y = resizeStartY

        if (right) width = resizeStartWidth + dx
        if (bottom) height = resizeStartHeight + dy
        if (left) width = resizeStartWidth - dx
        if (top) height = resizeStartHeight - dy

        width = clamp(width, dp(LIST_MIN_WIDTH_DP), min(dp(LIST_MAX_WIDTH_DP), area.width))
        height = clamp(height, dp(LIST_MIN_HEIGHT_DP), min(dp(LIST_MAX_HEIGHT_DP), area.height))

        // Re-derived from the clamped size, not from the raw drag, so the
        // fixed edge stays fixed once the moving one has stopped.
        if (left) x = resizeStartX + (resizeStartWidth - width)
        if (top) y = resizeStartY + (resizeStartHeight - height)

        params.width = width
        params.height = height
        params.x = clamp(x, area.left, area.right - width)
        params.y = clamp(y, area.top, area.bottom - height)
        popupWidthPx = width
        popupHeightPx = height

        try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { }
    }

    /** Writes the dragged size once, on release, rather than on every move. */
    private fun endPopupResize() {
        val density = resources.displayMetrics.density
        prefs().edit()
            .putInt(Prefs.KEY_POPUP_WIDTH_DP, (popupWidthPx / density).toInt())
            .putInt(Prefs.KEY_POPUP_HEIGHT_DP, (popupHeightPx / density).toInt())
            .apply()
    }

    /** The stored list size, falling back to the default shape. */
    private fun loadPopupSize() {
        val storedW = prefs().getInt(Prefs.KEY_POPUP_WIDTH_DP, LIST_WIDTH_DP)
        val storedH = prefs().getInt(Prefs.KEY_POPUP_HEIGHT_DP, LIST_HEIGHT_DP)
        popupWidthPx = dp(storedW.coerceIn(LIST_MIN_WIDTH_DP, LIST_MAX_WIDTH_DP))
        popupHeightPx = dp(storedH.coerceIn(LIST_MIN_HEIGHT_DP, LIST_MAX_HEIGHT_DP))
    }

    /** Report a failure the user can see, instead of appearing to do nothing. */
    private fun fail(message: String, e: Exception?) {
        android.util.Log.e("DevClip", message, e)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPopup() {
        val view = ensurePopupView() ?: return
        // Every open, not just the first: a clip captured while the list was
        // closed has to be in it when it opens, and the list is cheap to fill.
        refreshPopupContent()
        view.alpha = popupAlpha()
        if (popupWidthPx == 0 || popupHeightPx == 0) loadPopupSize()

        if (popupVisible) {
            applyPopupGeometry()
            updatePopupLayout()
            return
        }

        val params = WindowManager.LayoutParams(
            popupWidthPx, popupHeightPx, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        popupParams = params
        applyPopupGeometry()

        // addView on a view that is still attached throws, and a removal that
        // failed earlier leaves exactly that. Detaching first makes the add
        // unconditional rather than dependent on the previous cycle.
        if (view.parent != null) {
            try { windowManager.removeView(view) } catch (e: Exception) { }
        }
        try {
            windowManager.addView(view, params)
            popupVisible = true
        } catch (e: Exception) {
            fail(getString(R.string.devclip_error_place_window), e)
            popupVisible = false
        }
    }

    private fun updatePopupLayout() {
        if (!popupVisible) return
        val view = popupView ?: return
        val params = popupParams ?: return
        try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { }
    }

    private fun hidePopup() {
        popupView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { /* already removed */ }
        }
        popupVisible = false
    }


    /**
     * Sizes and positions the floating list, always inside the safe area. It
     * hangs off the bubble and flips above it when there is no room below.
     */
    private fun applyPopupGeometry() {
        val params = popupParams ?: return
        val area = safeArea()

        if (popupWidthPx == 0 || popupHeightPx == 0) loadPopupSize()
        // Clamped to what is on screen now, every time. A size dragged on an
        // unfolded foldable, or before a rotation, must not strand the window
        // wider than the display it is being placed on.
        val width = min(popupWidthPx, area.width - dp(EDGE_MARGIN_DP) * 2)
            .coerceAtLeast(1)
        val height = min(popupHeightPx, area.height - dp(EDGE_MARGIN_DP) * 2)
            .coerceAtLeast(1)
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
        params.x = clamp(x, area.left, area.right - width)
        params.y = clamp(y, area.top, area.bottom - height)
    }

    private fun openFullApp() {
        hidePopup()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        SelectionCapture.listener = null
        ImeWatcher.listener = null
        // Anything the debounce still owes gets written now rather than lost.
        mainHandler.removeCallbacks(persistPosition)
        persistPosition.run()
        mainHandler.removeCallbacksAndMessages(null)
        removeHandle()
        removeBubble()
        popupView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        popupContent?.release()
        popupContent = null
        popupView = null
    }
}
