package com.devclip.app.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
 * Setup: everything DevClip needs switched on, in the order it has to happen,
 * and a way past all of it.
 *
 * It exists because the app shipped without it and a fresh install could not
 * be made to work at all. Most of these can only be granted by leaving for a
 * system screen, and nothing in the app offered to take the user there — so
 * the bubble could never start, and tapping Start crashed DevClip rather than
 * explaining why.
 *
 * The steps are numbered because they are not independent. An APK installed
 * from outside the Play Store has its accessibility switch greyed out until
 * restricted settings are allowed, so step two is impossible until step one
 * is done — and the phone explains none of this. It says "App was denied
 * access" and leaves the user on a dimmed switch. A list of equal-looking
 * cards is a list you can start at the wrong end.
 *
 * A wall the user can walk past all the same. Holding somebody on a screen
 * until they have visited four system settings pages would be holding them
 * hostage to screens DevClip does not control, and the app can say plainly
 * what it cannot do instead. Skip is always there.
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
    val batteryFree = !OverlayController.isBatteryOptimised(context)

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The resume tick re-reads it; nothing to do with the result here. */ }

    val allDone = captureOn && overlayOn && notificationsOn && batteryFree

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
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
                    // First, and its own step, because it is what makes the
                    // next one possible. Android cannot be asked whether
                    // restricted settings have been allowed — there is no API
                    // for it — so this reads as done once text capture is on,
                    // which is the only proof available that the block lifted.
                    StepCard(
                        number = 1,
                        title = stringResource(R.string.setup_restricted),
                        body = stringResource(R.string.setup_restricted_body),
                        done = captureOn,
                        actionLabel = stringResource(R.string.setup_open),
                        onEnable = { OverlayController.openAppSettings(context) }
                    )
                }
                item {
                    StepCard(
                        number = 2,
                        title = stringResource(R.string.setup_capture),
                        body = stringResource(R.string.setup_capture_body),
                        done = captureOn,
                        onEnable = { OverlayController.requestAccessibility(context) }
                    )
                }
                item {
                    StepCard(
                        number = 3,
                        title = stringResource(R.string.setup_bubble),
                        body = stringResource(R.string.setup_bubble_body),
                        done = overlayOn,
                        onEnable = { OverlayController.requestOverlay(context) }
                    )
                }
                item {
                    StepCard(
                        number = 4,
                        title = stringResource(R.string.setup_notifications),
                        body = stringResource(R.string.setup_notifications_body),
                        done = notificationsOn,
                        onEnable = {
                            // The only one of these that is an ordinary
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
                item {
                    // Last, and not optional however much it looks it. A
                    // sleeping app is unbound from its accessibility service,
                    // which is capture silently stopping with the switch
                    // still reading as on — the exact failure this app spent
                    // weeks not understanding.
                    StepCard(
                        number = 5,
                        title = stringResource(R.string.setup_battery),
                        body = stringResource(R.string.setup_battery_body),
                        done = batteryFree,
                        onEnable = {
                            OverlayController.requestIgnoreBatteryOptimisations(context)
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
}

/**
 * One step, numbered.
 *
 * The number is part of the instruction, not decoration: these have to be
 * done in this order and the phone will not say so.
 */
@Composable
private fun StepCard(
    number: Int,
    title: String,
    body: String,
    done: Boolean,
    onEnable: () -> Unit,
    actionLabel: String? = null
) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (done) colors.success else colors.surfaceSunken,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (done) colors.onAccent else colors.inkSoft
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Space.md, end = Space.md)
        ) {
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
                    text = actionLabel ?: stringResource(R.string.setup_enable),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent
                )
            }
        }
    }
}
