package com.devclip.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.facebook.react.bridge.*

class OverlayModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    init {
        // The only place a ReactApplicationContext is handed to us. Everything
        // that emits — the foreground service, the accessibility service —
        // lives outside React and has no other way to reach it.
        DevClipEvents.reactContext = reactContext
    }

    override fun getName() = "DevClipOverlay"

    override fun invalidate() {
        if (DevClipEvents.reactContext === reactApplicationContext) {
            DevClipEvents.reactContext = null
        }
        super.invalidate()
    }

    /**
     * NativeEventEmitter warns on iOS when a module emits without these, and
     * the warning is noisy enough that people add listeners to silence it
     * rather than to use them. There is nothing to count here: emits come from
     * native components that run whether or not JS is listening.
     */
    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Double) = Unit

    private fun prefs() =
        reactApplicationContext.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    @ReactMethod
    fun requestOverlayPermission(promise: Promise) {
        val context = reactApplicationContext
        val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(context)
        if (canDraw) {
            promise.resolve(true)
            return
        }
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        promise.resolve(false)
    }

    @ReactMethod
    fun isOverlayPermissionGranted(promise: Promise) {
        val context = reactApplicationContext
        val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(context)
        promise.resolve(canDraw)
    }

    @ReactMethod
    fun requestAccessibilityPermission(promise: Promise) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        reactApplicationContext.startActivity(intent)
        promise.resolve(false)
    }

    @ReactMethod
    fun isAccessibilityServiceEnabled(promise: Promise) {
        val context = reactApplicationContext
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val target = "${context.packageName}/${ClipboardAccessibilityService::class.java.name}"
        val isEnabled = enabledServices.any {
            "${it.resolveInfo.serviceInfo.packageName}/${it.resolveInfo.serviceInfo.name}" == target
        }
        promise.resolve(isEnabled)
    }

    /**
     * Starts the service, and shows the bubble if it is already running but
     * hidden.
     *
     * Sent as ACTION_WAKE rather than a bare start: a plain start on a service
     * that is already up reaches onStartCommand with nothing to act on, so
     * pressing Start while the bubble was hidden did nothing visible. wake()
     * is a no-op when the bubble is already showing.
     */
    @ReactMethod
    fun startBubble() {
        prefs().edit().putBoolean(Prefs.KEY_BUBBLE_RUNNING, true).apply()
        sendToService(OverlayService.ACTION_WAKE)
    }

    @ReactMethod
    fun stopBubble() {
        val context = reactApplicationContext
        prefs().edit().putBoolean(Prefs.KEY_BUBBLE_RUNNING, false).apply()
        context.stopService(Intent(context, OverlayService::class.java))
    }

    private fun sendToService(action: String, configure: (Intent) -> Unit = {}) {
        val context = reactApplicationContext
        val intent = Intent(context, OverlayService::class.java).apply {
            this.action = action
            configure(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /** Closes the floating list. The bubble stays put. */
    @ReactMethod
    fun hidePopup() {
        sendToService(OverlayService.ACTION_HIDE_POPUP)
    }

    /** Opens the full-screen activity and closes the overlay. */
    @ReactMethod
    fun openFullApp() {
        sendToService(OverlayService.ACTION_OPEN_FULL)
    }

    /**
     * Resizes the bubble in place.
     *
     * This used to restart the service, which was survivable for three fixed
     * choices and would be a flicker on every pixel of a slider drag.
     */
    @ReactMethod
    fun setBubbleSize(sizeDp: Double) {
        val clamped = sizeDp.toInt()
            .coerceIn(Prefs.MIN_BUBBLE_SIZE_DP, Prefs.MAX_BUBBLE_SIZE_DP)
        prefs().edit().putInt(Prefs.KEY_BUBBLE_SIZE_DP, clamped).apply()
        if (prefs().getBoolean(Prefs.KEY_BUBBLE_RUNNING, false)) {
            sendToService(OverlayService.ACTION_SET_BUBBLE_SIZE) {
                it.putExtra(OverlayService.EXTRA_SIZE_DP, clamped)
            }
        }
    }

    /** Hides the bubble. The service keeps running; the notification brings it back. */
    @ReactMethod
    fun restBubble() {
        sendToService(OverlayService.ACTION_REST)
    }

    /** Brings a hidden bubble back, at the position the user left it. */
    @ReactMethod
    fun wakeBubble() {
        sendToService(OverlayService.ACTION_WAKE)
    }

    @ReactMethod
    fun setMaxClips(max: Double) {
        prefs().edit().putInt(Prefs.KEY_MAX_CLIPS, max.toInt()).apply()
    }

    @ReactMethod
    fun setAutoStartOnBoot(enabled: Boolean) {
        prefs().edit().putBoolean(Prefs.KEY_AUTO_START_ON_BOOT, enabled).apply()
    }

    @ReactMethod
    fun isBubbleRunning(promise: Promise) {
        promise.resolve(prefs().getBoolean(Prefs.KEY_BUBBLE_RUNNING, false))
    }

    /** How see-through the bubble is, as a percentage. */
    @ReactMethod
    fun setBubbleAlpha(alpha: Double) {
        prefs().edit()
            .putInt(Prefs.KEY_BUBBLE_ALPHA, alpha.toInt().coerceIn(Prefs.MIN_ALPHA, 100))
            .apply()
        applyAppearance()
    }

    /**
     * Whether the chosen transparency applies only when the bubble is idle.
     *
     * Off, the bubble simply sits at that level. On, it rests there and
     * returns to solid the moment it is touched — which is what makes a very
     * faint bubble usable, because you can see it as you reach for it.
     */
    @ReactMethod
    fun setBubbleIdleFade(enabled: Boolean) {
        prefs().edit().putBoolean(Prefs.KEY_BUBBLE_IDLE_FADE, enabled).apply()
        applyAppearance()
    }

    /** How see-through the floating list is, as a percentage. */
    @ReactMethod
    fun setPopupAlpha(alpha: Double) {
        prefs().edit()
            .putInt(Prefs.KEY_POPUP_ALPHA, alpha.toInt().coerceIn(Prefs.MIN_ALPHA, 100))
            .apply()
        applyAppearance()
    }

    /** Seconds of stillness before the bubble tucks itself away. 0 never does. */
    @ReactMethod
    fun setTuckDelay(seconds: Double) {
        prefs().edit()
            .putInt(Prefs.KEY_TUCK_DELAY_SEC, seconds.toInt().coerceIn(0, Prefs.MAX_TUCK_DELAY_SEC))
            .apply()
        applyAppearance()
    }

    /**
     * Mirrors tap-to-arm into native.
     *
     * The floating list is native now, so the setting that governs whether a
     * tap pastes immediately has to be readable without a React context —
     * exactly like the clip limit, and for the same reason.
     */
    @ReactMethod
    fun setConfirmBeforePaste(enabled: Boolean) {
        prefs().edit().putBoolean(Prefs.KEY_CONFIRM_BEFORE_PASTE, enabled).apply()
    }

    private fun applyAppearance() {
        if (prefs().getBoolean(Prefs.KEY_BUBBLE_RUNNING, false)) {
            sendToService(OverlayService.ACTION_APPLY_SETTINGS)
        }
    }

    /**
     * Whether Android is allowed to put DevClip to sleep.
     *
     * This is not cosmetic. Capture depends on an accessibility service
     * staying bound, and a phone that decides DevClip is idle will quietly
     * unbind it — the permission still reads as granted in Settings while
     * nothing is listening, which is the single hardest failure in this app to
     * recognise from the outside.
     */
    @ReactMethod
    fun isBatteryOptimised(promise: Promise) {
        val context = reactApplicationContext
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (manager == null) {
            promise.resolve(false)
            return
        }
        promise.resolve(!manager.isIgnoringBatteryOptimizations(context.packageName))
    }

    /**
     * Asks Android to stop optimising DevClip.
     *
     * An app cannot grant itself this; the most it can do is raise the system
     * dialog that asks. Where even that is unavailable, the app-details screen
     * is the honest fallback — one tap away from the setting, rather than a
     * button that silently does nothing.
     */
    @ReactMethod
    fun requestIgnoreBatteryOptimisations(promise: Promise) {
        val context = reactApplicationContext
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            openBatterySettings()
            promise.resolve(false)
        }
    }

    /**
     * Opens the place the user has to go by hand.
     *
     * Samsung's own "deep sleeping apps" list is not reachable through any
     * public API — it cannot even be read — so for the manufacturer most
     * likely to break capture, taking the user to the right screen and saying
     * what to do there is the whole of what an app can offer.
     */
    @ReactMethod
    fun openBatterySettings() {
        val context = reactApplicationContext
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.w("DevClip", "Could not open the battery settings screen", e)
        }
    }

    @ReactMethod
    fun pasteIntoFocusedField(text: String, promise: Promise) {
        val service = ClipboardAccessibilityService.instance
        if (service == null) {
            promise.resolve(false)
            return
        }
        promise.resolve(service.pasteIntoFocusedField(text))
    }
}

/**
 * Settings passed from JS into native components that can run with no JS
 * thread at all — OverlayService, BootReceiver, the accessibility service.
 *
 * SharedPreferences rather than a store read over the bridge, because these
 * are needed at moments when there is no bridge: the service can be started
 * by BootReceiver long before any React context exists.
 */
object Prefs {
    const val NAME = "devclip_prefs"
    const val KEY_BUBBLE_RUNNING = "bubble_running"
    const val KEY_AUTO_START_ON_BOOT = "auto_start_on_boot"

    /**
     * Bubble diameter in dp.
     *
     * A number, not one of three names, because the size is a slider now. The
     * floor is Android's comfortable touch target: below 48dp a bubble gets
     * missed, and it gets missed most over a keyboard, which is exactly where
     * it matters. The ceiling is 1.5x that — past it the bubble stops being a
     * bubble and starts being an obstruction.
     *
     * A new key rather than a reused one: the old value was a String, and
     * reading a String key as an Int throws.
     */
    const val KEY_BUBBLE_SIZE_DP = "bubble_size_dp"
    const val MIN_BUBBLE_SIZE_DP = 48
    const val MAX_BUBBLE_SIZE_DP = 72
    const val DEFAULT_BUBBLE_SIZE_DP = 56

    /**
     * Where the bubble is docked, as an edge and a fraction of the way down.
     *
     * Never pixels. A pixel position is meaningless the moment the window
     * changes shape — rotation, split-screen, a foldable opening — and this
     * has to survive all three plus a reboot.
     *
     * There is deliberately no setting for either. Dragging the bubble is the
     * only way to move it, which is why these are read by the service and
     * never written by it from JS.
     */
    const val KEY_BUBBLE_EDGE = "bubble_edge"
    const val KEY_BUBBLE_Y_FRACTION = "bubble_y_fraction"
    const val EDGE_LEFT = "left"
    const val EDGE_RIGHT = "right"
    const val DEFAULT_Y_FRACTION = 0.28f

    /**
     * The user's clip limit, mirrored here so capture can enforce it.
     *
     * Trimming used to happen only while the app was open. Capture happens
     * with the app closed — that is the point of it — so the limit was not a
     * limit until DevClip was next opened.
     */
    const val KEY_MAX_CLIPS = "max_clips"
    const val DEFAULT_MAX_CLIPS = 500

    /**
     * Tap-to-arm, mirrored for the floating list.
     *
     * The list is native now and opens with no React context behind it, so a
     * setting it obeys has to live where it can read it.
     */
    const val KEY_CONFIRM_BEFORE_PASTE = "confirm_before_paste"

    /**
     * How opaque the bubble and the list are, as percentages.
     *
     * The floor is not zero. A window at zero opacity is invisible but still
     * takes touches, which is indistinguishable from a phone that has started
     * ignoring part of the screen — so the slider stops well before the point
     * where the user could lose the bubble entirely.
     */
    const val KEY_BUBBLE_ALPHA = "bubble_alpha"
    const val KEY_POPUP_ALPHA = "popup_alpha"
    const val MIN_ALPHA = 20
    const val DEFAULT_ALPHA = 100

    /** Whether the chosen transparency applies only while the bubble is idle. */
    const val KEY_BUBBLE_IDLE_FADE = "bubble_idle_fade"

    /**
     * Seconds of stillness before the bubble becomes an edge handle. 0 is off.
     *
     * Off by default: a bubble that disappears on its own is a surprise the
     * first time it happens, and it should be something the user turned on.
     */
    const val KEY_TUCK_DELAY_SEC = "tuck_delay_sec"
    const val DEFAULT_TUCK_DELAY_SEC = 0
    const val MAX_TUCK_DELAY_SEC = 120

    /**
     * The size the user last dragged the floating list to, in dp.
     *
     * Stored in dp, never pixels, for the same reason the bubble's position
     * is: a pixel size stops meaning anything the moment the window changes
     * shape, and this has to survive a rotation and a reboot.
     */
    const val KEY_POPUP_WIDTH_DP = "popup_width_dp"
    const val KEY_POPUP_HEIGHT_DP = "popup_height_dp"
}
