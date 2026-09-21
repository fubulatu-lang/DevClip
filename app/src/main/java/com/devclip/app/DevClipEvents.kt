package com.devclip.app

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the rest of the app listens to when something happens outside it.
 *
 * Two things need it:
 *
 *  - the bubble captures a clip, and any list on screen has to show it;
 *  - the bubble is hidden, tucked away or brought back, and the app has to
 *    say which.
 *
 * This used to be a bridge into JavaScript, and every emit was allowed to be
 * a no-op because there was frequently no React instance to receive it — the
 * service outlives the app's UI, and after a reboot it starts before any UI
 * has ever existed. That is still true of the *listener*, not of the channel:
 * emitting from a service with no screen open is normal, and nothing is
 * collecting.
 *
 * So events remain a prompt to look, never the source of truth. A screen
 * reads the database when it appears, and an event only tells it to read
 * again. Dropping one costs a list that is briefly stale; treating one as
 * data would cost a list that is permanently wrong.
 */
object DevClipEvents {

    /**
     * A clip was captured. Carries the preview, for anything that wants to
     * say what was saved.
     *
     * A buffer with drop-oldest rather than a suspend: this is emitted from
     * the accessibility service's thread and from the foreground service, and
     * neither can afford to wait on a collector. If the buffer ever fills,
     * the newest prompt is the one worth keeping — they all mean the same
     * thing, which is "go and read the table".
     */
    private val _clipsChanged = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val clipsChanged: SharedFlow<String> = _clipsChanged.asSharedFlow()

    /**
     * Whether the bubble is currently hidden.
     *
     * State, not an event: a screen that opens needs the answer now, not the
     * next time it changes.
     */
    private val _bubbleResting = MutableStateFlow(false)
    val bubbleResting: StateFlow<Boolean> = _bubbleResting.asStateFlow()

    fun emitClipsChanged(preview: String) {
        _clipsChanged.tryEmit(preview)
    }

    fun emitBubbleState(resting: Boolean) {
        _bubbleResting.value = resting
    }
}
