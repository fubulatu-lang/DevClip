package com.devclip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * A counter that increments every time the screen comes back to the front.
 *
 * Read it in a composable and whatever surrounds that read is recomposed on
 * resume. It exists because most of what these screens show is not theirs to
 * observe: a permission switch in Android's settings, a service the system
 * binds, a battery restriction. None of it notifies anybody, and two of the
 * three can only be changed by leaving the app entirely.
 *
 * Settings read those once during composition and never again, so granting a
 * permission and coming back left the screen showing what it had read before
 * the user left — the Status section that exists to stop this app lying about
 * whether capture works, lying about whether capture works.
 *
 * Returning is the only reliable signal that something might have changed.
 */
@Composable
fun rememberResumeTick(): State<Int> {
    val tick = remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick.intValue++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    return tick
}
