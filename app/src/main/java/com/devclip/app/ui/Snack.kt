package com.devclip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * A message that says what just happened, then goes away.
 *
 * News, not a decision. It exists for the case where a paste half-worked —
 * the clip reached the clipboard but no field took it — and the user would
 * otherwise see nothing at all and conclude the tap did not register. A
 * dialog there would interrupt the thing it is reporting on.
 */
@Composable
fun Snack(text: String, onDismiss: () -> Unit) {
    val colors = Tokens.colors

    LaunchedEffect(text) {
        delay(SNACK_DURATION_MS)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Space.keyline)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            // Inverted against the page, so it reads as something laid over
            // the screen rather than another row on it.
            color = colors.bg,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.ink, RoundedCornerShape(Radius.md))
                .padding(horizontal = Space.lg, vertical = Space.md)
        )
    }
}

/** Long enough to read a sentence, short enough not to sit in the way. */
private const val SNACK_DURATION_MS = 4000L
