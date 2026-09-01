package com.devclip.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.facebook.react.bridge.*

class OverlayModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "DevClipOverlay"

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

    @ReactMethod
    fun startBubble() {
        val context = reactApplicationContext
        prefs().edit().putBoolean(Prefs.KEY_BUBBLE_RUNNING, true).apply()
        val intent = Intent(context, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
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

    @ReactMethod
    fun setBubbleSize(size: String) {
        prefs().edit().putString(Prefs.KEY_BUBBLE_SIZE, size).apply()
        // If the bubble is currently showing, restart it so the new size takes effect immediately.
        if (prefs().getBoolean(Prefs.KEY_BUBBLE_RUNNING, false)) {
            startBubble()
        }
    }

    @ReactMethod
    fun setAutoStartOnBoot(enabled: Boolean) {
        prefs().edit().putBoolean(Prefs.KEY_AUTO_START_ON_BOOT, enabled).apply()
    }

    @ReactMethod
    fun isBubbleRunning(promise: Promise) {
        promise.resolve(prefs().getBoolean(Prefs.KEY_BUBBLE_RUNNING, false))
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

/** Shared preferences keys used to pass simple settings from JS into native
 *  components (OverlayService, BootReceiver) that can run independently of
 *  the JS thread. */
object Prefs {
    const val NAME = "devclip_prefs"
    const val KEY_BUBBLE_RUNNING = "bubble_running"
    const val KEY_BUBBLE_SIZE = "bubble_size" // "small" | "medium" | "large"
    const val KEY_AUTO_START_ON_BOOT = "auto_start_on_boot"
}
