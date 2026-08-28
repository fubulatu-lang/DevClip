package com.devclip.app

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Runs as a system Accessibility Service. Android grants clipboard read
 * access to accessibility services even when no app is in the foreground,
 * which is what makes true "capture anything copied anywhere" possible on
 * Android 10+. The user must turn this on manually once, from
 * Settings > Accessibility (OverlayModule.requestAccessibilityPermission
 * opens that screen for them).
 *
 * This service also performs the reverse operation: pasting a saved clip
 * directly into whichever text field the user was last typing in, in
 * whatever app that is — see pasteIntoFocusedField below. That capability
 * is why this service requests canRetrieveWindowContent +
 * flagRetrieveInteractiveWindows in accessibility_service_config.xml,
 * rather than only the minimal clipboard-listener permissions it needed
 * before.
 */
class ClipboardAccessibilityService : AccessibilityService() {

    private lateinit var clipboardManager: ClipboardManager
    private var dbHelper: DevClipDatabaseHelper? = null

    companion object {
        // Lets OverlayModule reach the running service instance to trigger a
        // paste. Null whenever the user hasn't enabled the accessibility
        // service (or the OS has killed it).
        var instance: ClipboardAccessibilityService? = null
    }

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
        instance = this
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        dbHelper = DevClipDatabaseHelper(applicationContext)
        clipboardManager.addPrimaryClipChangedListener(clipListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: we don't react to individual events. pasteIntoFocusedField
        // below queries the currently-focused node on demand instead of
        // tracking focus changes continuously.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        dbHelper?.close()
    }

    /**
     * Sets the system clipboard to [text], then attempts to paste it directly
     * into whatever editable field currently has input focus in the
     * foreground app (our own overlay windows are non-focusable, so this
     * correctly finds the field the user was last using underneath them).
     *
     * Returns true if a real paste actually happened, false if there was no
     * focused editable field or it doesn't support paste — in which case the
     * text is still on the clipboard and the caller should fall back to
     * telling the user to paste manually.
     */
    fun pasteIntoFocusedField(text: String): Boolean {
        try {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("DevClip", text))

            val node = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            val canPaste = node.isEditable && node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }
            if (!canPaste) return false

            return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } catch (e: Exception) {
            return false
        }
    }
}
