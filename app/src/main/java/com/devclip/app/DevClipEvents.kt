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
 *  - the theme changes, and every screen has to repaint in it.
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
     * A clip was captured. Carries nothing: the database is where the clip
     * is, and the only thing a listener does is read it again.
     *
     * A buffer with drop-oldest rather than a suspend: this is emitted from
     * the accessibility service's thread and from the foreground service, and
     * neither can afford to wait on a collector. If the buffer ever fills,
     * the newest prompt is the one worth keeping — they all mean the same
     * thing, which is "go and read the table".
     */
    private val _clipsChanged = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val clipsChanged: SharedFlow<Unit> = _clipsChanged.asSharedFlow()

    /**
     * Bumped whenever the theme preference changes.
     *
     * The Compose theme used to read the palette straight out of
     * SharedPreferences during composition. Nothing about that read is
     * observable, so changing the theme repainted nothing: the new colours
     * appeared only when something else happened to recompose the whole tree
     * — leaving the screen, rotating, reopening the app. It looked like a
     * very slow theme switch and was actually no theme switch at all.
     *
     * A counter rather than the mode itself, because the answer also depends
     * on the system's dark mode when the mode is "system", and this only has
     * to say "ask again".
     */
    private val _themeRevision = MutableStateFlow(0)
    val themeRevision: StateFlow<Int> = _themeRevision.asStateFlow()

    fun emitThemeChanged() {
        _themeRevision.value++
    }

    fun emitClipsChanged() {
        _clipsChanged.tryEmit(Unit)
    }
}
