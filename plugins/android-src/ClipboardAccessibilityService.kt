package com.devclip.app

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * DevClip's accessibility service. It does two things, and neither of them is
 * reading the clipboard.
 *
 * **Reading the selection.** Selection-changed events are fed to
 * [SelectionCapture], which keeps the most recent one as a fallback for the
 * live tree walk it does when the bubble is tapped. See SelectionCapture for
 * why capture works this way rather than through the clipboard.
 *
 * **Pasting.** [pasteIntoFocusedField] puts a saved clip into whichever text
 * field the user was last typing in, in whatever app that is. DevClip's own
 * windows are non-focusable, so "the focused field" is correctly the one
 * underneath them.
 *
 * There used to be an `OnPrimaryClipChangedListener` here, on the belief that
 * accessibility services can read the clipboard in the background. They
 * cannot. Since Android 10, `ClipboardService.clipboardAccessAllowed` returns
 * false for a read unless the caller holds the signature-only
 * READ_CLIPBOARD_IN_BACKGROUND permission, has window focus, or is the default
 * IME — and an accessibility service is granted none of those. The listener
 * fired and got null every time, which is the quietest possible way for a
 * feature to not exist. It is gone.
 *
 * The class keeps its name because renaming it changes the service's component
 * name in the manifest, and Android would treat that as a different service —
 * the user would have to walk back into Settings and turn accessibility on
 * again for no benefit.
 */
class ClipboardAccessibilityService : AccessibilityService() {

    private var clipboardManager: ClipboardManager? = null

    companion object {
        // Lets OverlayService and OverlayModule reach the running instance.
        // Null whenever the user has not enabled the service (or the OS has
        // killed it), which is exactly when there is no capture and no paste.
        @Volatile
        var instance: ClipboardAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ->
                    SelectionCapture.onSelectionEvent(event)
            }
        } catch (e: Exception) {
            // Never crash the accessibility service. Losing one event costs a
            // fallback; crashing costs the user capture, paste, and a trip
            // back through the Settings screen to re-enable it.
            android.util.Log.w("DevClip", "Accessibility event handling failed", e)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        SelectionCapture.forget()
    }

    /**
     * Puts [text] on the system clipboard.
     *
     * Writing is allowed from the background; only reading is gated on focus.
     * That asymmetry is why capture cannot use the clipboard and paste can.
     */
    fun setClipboard(text: String): Boolean =
        try {
            clipboardManager?.setPrimaryClip(ClipData.newPlainText("DevClip", text)) != null
        } catch (e: Exception) {
            android.util.Log.w("DevClip", "Could not set the clipboard", e)
            false
        }

    /**
     * Sets the system clipboard to [text], then attempts to paste it directly
     * into whatever editable field currently has input focus in the
     * foreground app.
     *
     * Returns true if a real paste actually happened, false if there was no
     * focused editable field or it doesn't support paste — in which case the
     * text is still on the clipboard and the caller should fall back to
     * telling the user to paste manually.
     */
    fun pasteIntoFocusedField(text: String): Boolean {
        try {
            setClipboard(text)

            val node = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            val canPaste =
                node.isEditable && node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }
            if (!canPaste) return false

            return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } catch (e: Exception) {
            return false
        }
    }
}
