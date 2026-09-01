package com.devclip.app

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap

/**
 * The one channel from native into JS.
 *
 * Three separate things need it and they all needed it at once, so it is one
 * mechanism rather than three:
 *
 *  - the bubble captures a clip and the lists have to show it;
 *  - the bubble is hidden or brought back and the app has to say which;
 *  - the floating list is mounted once and, before this existed, read the
 *    database once and never again. Native could have been saving clips
 *    perfectly and the list would still have looked empty forever.
 *
 * [reactContext] is set by [OverlayModule] when the React instance comes up.
 * Everything that emits runs outside React — a foreground service, an
 * accessibility service — and any of it can run when there is no React
 * instance at all: after a reboot, or once Android has trimmed the app's
 * process and left only the service. So every emit is allowed to be a no-op,
 * and JS re-reads the database when it comes back rather than assuming it saw
 * every event. Events are a prompt to refresh, never the source of truth.
 */
object DevClipEvents {

    /** A clip was written to the database by native. Payload: `{ preview }`. */
    const val CLIPS_CHANGED = "DevClipClipsChanged"

    /** The bubble was hidden or brought back. Payload: `{ resting }`. */
    const val BUBBLE_STATE = "DevClipBubbleState"

    @Volatile
    var reactContext: ReactApplicationContext? = null

    fun emit(name: String, params: WritableMap?) {
        val context = reactContext ?: return
        try {
            if (!context.hasActiveReactInstance()) return
            context.emitDeviceEvent(name, params)
        } catch (e: Exception) {
            // The instance can be torn down between the check and the call.
            // A dropped event costs a stale list until the next refresh; a
            // crash here would take the service down with it.
            android.util.Log.w("DevClip", "Could not deliver $name to JS", e)
        }
    }

    fun emitClipsChanged(preview: String) {
        val params = Arguments.createMap().apply { putString("preview", preview) }
        emit(CLIPS_CHANGED, params)
    }

    fun emitBubbleState(resting: Boolean) {
        val params = Arguments.createMap().apply { putBoolean("resting", resting) }
        emit(BUBBLE_STATE, params)
    }
}
