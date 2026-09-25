package com.devclip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay

/**
 * A message that says what just happened, then goes away.
 *
 * News, not a decision. It exists for the case where a paste half-worked —
 * the clip reached the clipboard but no field took it — and the user would
 * otherwise see nothing at all and conclude the tap did not register. A
 * dialog there would interrupt the thing it is reporting on.
 *
 * A live region, so TalkBack reads it out. Without that the message was only
 * news to people who could see it, and for everyone else the half-worked
 * paste was exactly the silence this exists to prevent.
 *
 * It can carry one action — Undo, after a delete — and stays longer when it
 * does, because an action nobody has time to reach is not an action.
 */
@Composable
fun Snack(
    text: String,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = Tokens.colors
    val hasAction = actionLabel != null && onAction != null

    LaunchedEffect(text, hasAction) {
        delay(if (hasAction) SNACK_WITH_ACTION_MS else SNACK_DURATION_MS)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Space.keyline)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Inverted against the page, so it reads as something laid over the
        // screen rather than another row on it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.ink, RoundedCornerShape(Radius.md))
                .heightIn(min = MinTouchTarget)
                .padding(
                    start = Space.lg,
                    end = if (hasAction) Space.xs else Space.lg,
                    top = Space.xs,
                    bottom = Space.xs
                )
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.bg,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = Space.sm)
            )
            if (hasAction) {
                TextButton(
                    onClick = {
                        onAction?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.heightIn(min = MinTouchTarget)
                ) {
                    Text(
                        text = actionLabel.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.bg
                    )
                }
            }
        }
    }
}

/** Long enough to read a sentence, short enough not to sit in the way. */
private const val SNACK_DURATION_MS = 4000L

/** Long enough to read the sentence and then decide to reach for Undo. */
private const val SNACK_WITH_ACTION_MS = 8000L
