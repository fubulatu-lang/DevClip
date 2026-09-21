package com.devclip.app.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devclip.app.OverlayController
import com.devclip.app.R

/**
 * Setup: the three permissions DevClip cannot work without, and a way past
 * them.
 *
 * It exists because the app shipped without it and a fresh install could not
 * be made to work at all. Two of these permissions can only be granted by
 * leaving for a system screen, and nothing in the app offered to take the
 * user there — so the bubble could never start, and tapping Start crashed
 * DevClip rather than explaining why.
 *
 * A wall the user can walk past. Holding somebody on a screen until they have
 * visited two system settings pages would be holding them hostage to screens
 * DevClip does not control, and the app can say plainly what it cannot do
 * instead. Skip is always there.
 *
 * Each card re-reads its own state on resume, because granting any of these
 * means leaving and coming back, and that return is the only notice anything
 * changed.
 */
@Composable
fun SetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val colors = Tokens.colors
    val tick by rememberResumeTick()

    // Read through `tick` so returning from a system settings screen re-reads
    // rather than showing what was true before the user left.
    @Suppress("UNUSED_EXPRESSION") tick

    val captureOn = OverlayController.isAccessibilityEnabled(context)
    val overlayOn = OverlayController.isOverlayGranted(context)
    val notificationsOn = OverlayController.isNotificationGranted(context)

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The resume tick re-reads it; nothing to do with the result here. */ }

    val allDone = captureOn && overlayOn && notificationsOn

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Space.keyline,
                end = Space.keyline,
                top = Space.keyline
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item {
                Text(
                    text = stringResource(R.string.setup_heading),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.ink,
                    modifier = Modifier.padding(bottom = Space.sm)
                )
            }
            item {
                Text(
                    text = stringResource(R.string.setup_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSoft,
                    modifier = Modifier.padding(bottom = Space.lg)
                )
            }

            item {
                PermissionCard(
                    title = stringResource(R.string.setup_capture),
                    body = stringResource(R.string.setup_capture_body),
                    done = captureOn,
                    onEnable = { OverlayController.requestAccessibility(context) }
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.setup_bubble),
                    body = stringResource(R.string.setup_bubble_body),
                    done = overlayOn,
                    onEnable = { OverlayController.requestOverlay(context) }
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.setup_notifications),
                    body = stringResource(R.string.setup_notifications_body),
                    done = notificationsOn,
                    onEnable = {
                        // The only one of the three that is an ordinary
                        // runtime permission, so the only one that can be
                        // asked for without leaving the app.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                )
            }
        }

        // The interaction area: One UI puts what the user touches within
        // reach of a thumb, not at the top where the eye lands first.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Space.keyline,
                    end = Space.keyline,
                    bottom = Space.keyline,
                    top = Space.md
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                )
            ) {
                Text(
                    text = stringResource(
                        if (allDone) R.string.setup_continue else R.string.setup_skip
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (!allDone) {
                Text(
                    text = stringResource(R.string.setup_skip_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(top = Space.sm)
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    done: Boolean,
    onEnable: () -> Unit
) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Space.md)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSoft,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (done) {
            // Granted states say so in words as well as position. One UI does
            // not allow colour or placement alone to carry meaning.
            Text(
                text = stringResource(R.string.setup_done),
                style = MaterialTheme.typography.labelLarge,
                color = colors.success
            )
        } else {
            TextButton(
                onClick = onEnable,
                modifier = Modifier.heightIn(min = MinTouchTarget)
            ) {
                Text(
                    text = stringResource(R.string.setup_enable),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent
                )
            }
        }
    }
}
