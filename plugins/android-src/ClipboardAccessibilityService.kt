package com.devclip.app

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent

/**
 * Runs as a system Accessibility Service. Android grants clipboard read
 * access to accessibility services even when no app is in the foreground,
 * which is what makes true "capture anything copied anywhere" possible on
 * Android 10+. The user must turn this on manually once, from
 * Settings > Accessibility (OverlayModule.requestAccessibilityPermission
 * opens that screen for them).
 *
 * We deliberately do almost nothing with AccessibilityEvents themselves —
 * we only need this service class to exist and be enabled so the OS treats
 * us as an accessibility client, which unlocks the clipboard listener.
 */
class ClipboardAccessibilityService : AccessibilityService() {

    private lateinit var clipboardManager: ClipboardManager
    private var dbHelper: DevClipDatabaseHelper? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        try {
            val clip = clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
            if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
            val text = clip.getItemAt(0).coerceToText(this)?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                dbHelper?.insertClip(text)
            }
        } catch (e: Exception) {
            // Never crash the accessibility service — a bad clipboard read
            // should not take down system-wide clipboard capture.
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        dbHelper = DevClipDatabaseHelper(applicationContext)
        clipboardManager.addPrimaryClipChangedListener(clipListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: we only use this service to obtain clipboard access, not to
        // read screen content.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        dbHelper?.close()
    }
}
