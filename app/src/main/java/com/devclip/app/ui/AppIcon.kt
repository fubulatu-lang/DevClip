package com.devclip.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * DevClip's own launcher icon, as the phone draws it.
 *
 * Asked of the package manager rather than loaded with painterResource. The
 * launcher icon is an adaptive icon, which painterResource cannot draw at
 * all — it throws on anything that is not a vector or a bitmap — and going
 * through the system also gets the shape the user's launcher masks it to,
 * so the mark in the header is the one on their home screen.
 *
 * Decorative: the title beside it already names the screen, and
 * a screen reader announcing "DevClip icon" before every visit adds nothing.
 */
@Composable
fun AppIcon(size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Rasterised once at the largest size the header grows it to, then
    // scaled down, rather than redrawn on every frame of the pull.
    val px = with(LocalDensity.current) { AppIconMaxSize.roundToPx() }
    val bitmap: ImageBitmap? = remember(px) {
        try {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap(px, px)
                .asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    bitmap ?: return
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
}

/** The icon's size with the header collapsed, and fully open. */
val AppIconMinSize = 28.dp
val AppIconMaxSize = 44.dp
