package com.devclip.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Where the on-screen keyboard starts, so the bubble can get out of its way.
 *
 * There is no other route to this. An overlay window with FLAG_NOT_FOCUSABLE
 * receives no IME insets — that is the same flag that keeps the bubble from
 * stealing focus, and it is load-bearing for both capture and paste — so the
 * bubble cannot measure the keyboard itself. The accessibility service can:
 * its window list includes the IME window and its bounds on screen. That
 * service is already mandatory for capture, so this costs nothing extra, and
 * when it is off the user has no capture either and the bubble simply does not
 * dodge.
 *
 * Z-order is not the problem here. TYPE_APPLICATION_OVERLAY draws above the
 * IME, which is exactly why a bubble parked low ends up sitting on top of the
 * keys.
 */
object ImeWatcher {

    /**
     * Top edge of the keyboard in screen pixels, or 0 when there is no
     * keyboard up.
     */
    @Volatile
    var imeTopPx: Int = 0
        private set

    /** Called on the accessibility service's thread when the value changes. */
    @Volatile
    var listener: ((Int) -> Unit)? = null

    fun refresh(service: AccessibilityService) {
        val top = try {
            measure(service)
        } catch (e: Exception) {
            // The window list can be pulled out from under a read. A stale
            // value for one frame is better than a dead accessibility service.
            return
        }
        if (top == imeTopPx) return
        imeTopPx = top
        listener?.invoke(top)
    }

    fun forget() {
        if (imeTopPx == 0) return
        imeTopPx = 0
        listener?.invoke(0)
    }

    private fun measure(service: AccessibilityService): Int {
        val windows = service.windows ?: return 0
        for (window in windows) {
            if (window == null) continue
            if (window.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue
            val bounds = Rect()
            window.getBoundsInScreen(bounds)
            // A keyboard that reports an empty rectangle is one on its way in
            // or out. Treating it as "no keyboard" is the safe reading: the
            // bubble stays parked rather than jumping to a nonsense position.
            if (bounds.height() <= 0) continue
            return bounds.top
        }
        return 0
    }
}
