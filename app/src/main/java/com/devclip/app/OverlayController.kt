package com.devclip.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Everything the app asks of the parts that run without it.
 *
 * This was a React Native module — a class whose methods existed to be called
 * across a bridge, each taking a Promise. There is no bridge now, so they are
 * ordinary functions and the Promises are gone.
 *
 * The service is still reached by Intent rather than by a binding. It
 * outlives the app, is started by BootReceiver when no app exists, and has to
 * keep running when the app is gone; an Intent says "do this" without either
 * side holding the other alive.
 */
object OverlayController {

    private fun prefs(context: Context) =
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    private fun send(context: Context, action: String, configure: (Intent) -> Unit = {}) {
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

    // ---- Overlay permission ----

    fun isOverlayGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun requestOverlay(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ---- Text capture ----

    /**
     * Whether the user has switched DevClip on in Accessibility settings.
     *
     * This is the question the old settings screen asked, and it is not the
     * question that matters. See [isCaptureWorking].
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager ?: return false
        val target = "${context.packageName}/${ClipboardAccessibilityService::class.java.name}"
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                "${it.resolveInfo.serviceInfo.packageName}/${it.resolveInfo.serviceInfo.name}" == target
            }
    }

    /**
     * Whether text capture is actually working right now.
     *
     * The difference between this and [isAccessibilityEnabled] is the single
     * most expensive thing this app has got wrong. The switch being on and
     * the service being bound are separate facts, and on Samsung they come
     * apart routinely — the phone unbinds services belonging to apps it
     * decides are idle, and reinstalling over an existing build does it too.
     * The switch keeps reading as on the entire time.
     *
     * An app that reports the switch tells the user everything is fine while
     * nothing is listening, which is indistinguishable from the feature being
     * broken and is why it took so long to find. The running instance is the
     * honest answer: if it is null, there is no capture, whatever Settings
     * says.
     */
    fun isCaptureWorking(): Boolean = ClipboardAccessibilityService.instance != null

    fun requestAccessibility(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- Notifications ----

    fun isNotificationGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    // ---- Battery ----

    /**
     * Whether Android may put DevClip to sleep.
     *
     * Not cosmetic: sleeping the app unbinds the accessibility service, and
     * [isCaptureWorking] starts returning false with nothing having visibly
     * changed.
     */
    fun isBatteryOptimised(context: Context): Boolean {
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return !manager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Raises Android's own "stop optimising this app" dialog.
     *
     * An app cannot grant itself this; asking is the most it can do. Where
     * the dialog is unavailable, the app-details screen is the honest
     * fallback rather than a button that silently does nothing.
     */
    fun requestIgnoreBatteryOptimisations(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }

    /**
     * Opens DevClip's app-details screen.
     *
     * Samsung's own "deep sleeping apps" list is reachable through no public
     * API — it cannot even be read — so for the manufacturer most likely to
     * break capture, this plus a plain instruction is all an app can offer.
     */
    fun openAppSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            android.util.Log.w("DevClip", "Could not open the app settings screen", e)
        }
    }

    // ---- The bubble ----

    fun isBubbleRunning(context: Context): Boolean =
        prefs(context).getBoolean(Prefs.KEY_BUBBLE_RUNNING, false)

    /**
     * Starts the bubble, or reports that it cannot.
     *
     * Returns false when the overlay permission is missing. The service
     * cannot place a window without it, and starting anyway is what used to
     * crash the app: the window add threw, the service died, and DevClip
     * closed with no explanation. Asking first means the caller can send the
     * user somewhere useful instead.
     */
    fun startBubble(context: Context): Boolean {
        if (!isOverlayGranted(context)) return false
        prefs(context).edit().putBoolean(Prefs.KEY_BUBBLE_RUNNING, true).apply()
        // WAKE rather than a bare start: a plain start on a service already
        // up reaches onStartCommand with nothing to act on, so pressing Start
        // while the bubble was merely hidden used to do nothing visible.
        send(context, OverlayService.ACTION_WAKE)
        return true
    }

    fun stopBubble(context: Context) {
        prefs(context).edit().putBoolean(Prefs.KEY_BUBBLE_RUNNING, false).apply()
        context.stopService(Intent(context, OverlayService::class.java))
    }




    // ---- Settings the service reads ----

    private fun applyAppearance(context: Context) {
        if (isBubbleRunning(context)) send(context, OverlayService.ACTION_APPLY_SETTINGS)
    }

    fun setBubbleSize(context: Context, sizeDp: Int) {
        val clamped = sizeDp.coerceIn(Prefs.MIN_BUBBLE_SIZE_DP, Prefs.MAX_BUBBLE_SIZE_DP)
        prefs(context).edit().putInt(Prefs.KEY_BUBBLE_SIZE_DP, clamped).apply()
        if (isBubbleRunning(context)) {
            send(context, OverlayService.ACTION_SET_BUBBLE_SIZE) {
                it.putExtra(OverlayService.EXTRA_SIZE_DP, clamped)
            }
        }
    }

    fun setPopupAlpha(context: Context, alpha: Int) {
        prefs(context).edit()
            .putInt(Prefs.KEY_POPUP_ALPHA, alpha.coerceIn(Prefs.MIN_ALPHA, 100)).apply()
        applyAppearance(context)
    }

    fun setTuckDelay(context: Context, seconds: Int) {
        prefs(context).edit()
            .putInt(Prefs.KEY_TUCK_DELAY_SEC, seconds.coerceIn(0, Prefs.MAX_TUCK_DELAY_SEC))
            .apply()
        applyAppearance(context)
    }

    /**
     * Light, dark or system, for the app and the floating windows alike.
     *
     * Written here rather than by the settings screen because the service
     * has to be told. It draws windows of its own from the same palette, and
     * it reads that palette when a window is built — so a theme changed
     * while the bubble is running reached the app and left the floating list
     * on the old one until the next reboot.
     */
    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(Prefs.KEY_THEME_MODE, mode).apply()
        // The app's own screens, which read the palette during composition and
        // would otherwise not notice until something else redrew them.
        DevClipEvents.emitThemeChanged()
        applyAppearance(context)
    }

    fun themeMode(context: Context): String =
        prefs(context).getString(Prefs.KEY_THEME_MODE, Prefs.THEME_SYSTEM) ?: Prefs.THEME_SYSTEM

    fun setCloseOnOutsideTouch(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(Prefs.KEY_CLOSE_ON_OUTSIDE_TOUCH, enabled).apply()
    }

    fun setConfirmBeforePaste(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(Prefs.KEY_CONFIRM_BEFORE_PASTE, enabled).apply()
    }

    fun setMaxClips(context: Context, max: Int) {
        prefs(context).edit().putInt(Prefs.KEY_MAX_CLIPS, max).apply()
    }

    fun setAutoStartOnBoot(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(Prefs.KEY_AUTO_START_ON_BOOT, enabled).apply()
    }

    // ---- Reading them back, for the screens that show them ----

    fun bubbleSize(context: Context): Int =
        prefs(context).getInt(Prefs.KEY_BUBBLE_SIZE_DP, Prefs.DEFAULT_BUBBLE_SIZE_DP)

    fun popupAlpha(context: Context): Int =
        prefs(context).getInt(Prefs.KEY_POPUP_ALPHA, Prefs.DEFAULT_ALPHA)
            .coerceIn(Prefs.MIN_ALPHA, 100)

    fun tuckDelay(context: Context): Int =
        prefs(context).getInt(Prefs.KEY_TUCK_DELAY_SEC, Prefs.DEFAULT_TUCK_DELAY_SEC)
            .coerceIn(0, Prefs.MAX_TUCK_DELAY_SEC)

    fun closeOnOutsideTouch(context: Context): Boolean =
        prefs(context).getBoolean(Prefs.KEY_CLOSE_ON_OUTSIDE_TOUCH, true)

    fun confirmBeforePaste(context: Context): Boolean =
        prefs(context).getBoolean(Prefs.KEY_CONFIRM_BEFORE_PASTE, true)

    fun maxClips(context: Context): Int =
        prefs(context).getInt(Prefs.KEY_MAX_CLIPS, Prefs.DEFAULT_MAX_CLIPS)

    fun autoStartOnBoot(context: Context): Boolean =
        prefs(context).getBoolean(Prefs.KEY_AUTO_START_ON_BOOT, true)

    // ---- Paste ----

    /**
     * Puts a clip back into whatever field the user was last in.
     *
     * False means the text is on the clipboard but nothing took it — there
     * was no focused editable field, or it refuses a paste. The caller has to
     * say so; silence here reads as the tap not registering.
     */
    fun paste(text: String): Boolean =
        ClipboardAccessibilityService.instance?.pasteIntoFocusedField(text) ?: false
}
