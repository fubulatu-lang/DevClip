package com.devclip.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import com.facebook.react.bridge.*

class OverlayModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "DevClipOverlay"

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
        // The user has to flip the switch manually; the app should re-check
        // with isOverlayPermissionGranted (or just retry startBubble) once
        // they come back.
        promise.resolve(false)
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
        val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
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
        context.stopService(Intent(context, OverlayService::class.java))
    }

    @ReactMethod
    fun resizePopupWindow(width: Int, height: Int) {
        val context = reactApplicationContext
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_RESIZE
            putExtra(OverlayService.EXTRA_WIDTH, width)
            putExtra(OverlayService.EXTRA_HEIGHT, height)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
