package com.devclip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.devclip.app.OverlayController
import com.devclip.app.Prefs
import com.devclip.app.R

/**
 * Settings, reorganised.
 *
 * The screen this replaces had five sections, one of which was a junk drawer:
 * "Capture" held eleven controls covering four unrelated jobs — starting and
 * stopping the service, hiding the bubble, the bubble's size and opacity, the
 * *list's* opacity, resizing, auto-start and confirm-before-paste. Almost
 * none of it was capture, which was filed under Permissions. Meanwhile
 * "Appearance" held exactly one thing while every appearance control sat in
 * Capture.
 *
 * Five sections now, each named for one idea: what is working, the bubble,
 * the list, the history, and how it looks.
 *
 * Status is the one that earns its place. The old screen answered "is the
 * permission switch on", which is not the question — the switch and the
 * service being bound are separate facts that come apart routinely on this
 * phone. Answering the wrong one is how the app spent weeks telling its user
 * everything was fine while nothing was listening.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val colors = Tokens.colors

    BackHandler(onBack = onBack)

    // Two reasons to re-read. `revision` covers this screen's own writes;
    // `tick` covers everything it does not own — a permission switch in
    // Android's settings, a service the system binds, a battery restriction.
    // None of those notify anybody, and most can only be changed by leaving,
    // so coming back is the only signal there is.
    //
    // Without the tick, granting a permission and returning left Status
    // showing what it had read before the user left: the section built to
    // stop this app lying about whether capture works, lying about it.
    var revision by remember { mutableStateOf(0) }
    val tick by rememberResumeTick()
    @Suppress("UNUSED_EXPRESSION") revision
    @Suppress("UNUSED_EXPRESSION") tick

    val header = rememberOneUiHeaderState()
    val layout = rememberWindowLayout()

    // Deleting every clip is the one thing on this screen that cannot be
    // taken back, and it sat one tap from Import. A dialog is right here and
    // nowhere else in the app: this is a decision, not news.
    var confirmingClear by remember { mutableStateOf(false) }

    // Edge to edge, so the bars are this screen's to account for. The colour
    // goes under them; only the content is inset.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .nestedScroll(header.nestedScrollConnection)
        ) {
        OneUiHeader(
            title = stringResource(R.string.settings_title),
            state = header,
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(MinTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = colors.ink
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = layout.margin,
                end = layout.margin,
                bottom = Space.keyline
            ),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            // ---- Status ----
            item { SectionHeader(stringResource(R.string.settings_status)) }
            item {
                val working = OverlayController.isCaptureWorking()
                val switchedOn = OverlayController.isAccessibilityEnabled(context)
                StatusRow(
                    label = stringResource(R.string.status_capture),
                    health = if (working) Health.Ok else Health.Problem,
                    // The case worth naming. Switched on but not running is
                    // not "off" — it is the failure that looks like success,
                    // and the user needs telling which one they have.
                    detail = when {
                        working -> stringResource(R.string.status_working)
                        switchedOn -> stringResource(R.string.status_granted_not_running)
                        else -> stringResource(R.string.status_off)
                    },
                    onClick = { OverlayController.requestAccessibility(context) }
                )
            }
            item {
                // The permission, separate from the running state below it.
                // Nothing in the app requested this, so on a fresh install
                // the bubble could never appear — and starting it anyway
                // crashed DevClip rather than saying why.
                val granted = OverlayController.isOverlayGranted(context)
                StatusRow(
                    label = stringResource(R.string.status_overlay),
                    health = if (granted) Health.Ok else Health.Problem,
                    detail = stringResource(
                        if (granted) R.string.status_granted else R.string.status_not_granted
                    ),
                    onClick = { OverlayController.requestOverlay(context) }
                )
            }
            item {
                val running = OverlayController.isBubbleRunning(context)
                val canStart = OverlayController.isOverlayGranted(context)
                StatusRow(
                    label = stringResource(R.string.status_bubble),
                    // Stopped because the user stopped it is a choice, not a
                    // fault. Caution is for something that is wrong; a bubble
                    // switched off on purpose gets the neutral mark.
                    health = when {
                        running -> Health.Ok
                        !canStart -> Health.Problem
                        else -> Health.Idle
                    },
                    detail = stringResource(
                        when {
                            running -> R.string.status_on
                            !canStart -> R.string.status_blocked_no_overlay
                            else -> R.string.status_off
                        }
                    ),
                    onClick = {
                        when {
                            running -> OverlayController.stopBubble(context)
                            // Send them to the permission rather than to a
                            // service that cannot place a window.
                            !canStart -> OverlayController.requestOverlay(context)
                            else -> OverlayController.startBubble(context)
                        }
                        revision++
                    }
                )
            }
            item {
                val granted = OverlayController.isNotificationGranted(context)
                StatusRow(
                    label = stringResource(R.string.status_notifications),
                    health = if (granted) Health.Ok else Health.Problem,
                    detail = stringResource(
                        if (granted) R.string.status_granted else R.string.status_not_granted
                    ),
                    // Android offers this dialog once and never again, so
                    // after a refusal the app-details screen is the only
                    // route left.
                    onClick = { OverlayController.openAppSettings(context) }
                )
            }
            item {
                val optimised = OverlayController.isBatteryOptimised(context)
                StatusRow(
                    label = stringResource(R.string.status_battery),
                    health = if (optimised) Health.Problem else Health.Ok,
                    detail = stringResource(
                        if (optimised) R.string.status_restricted else R.string.status_unrestricted
                    ),
                    onClick = { OverlayController.requestIgnoreBatteryOptimisations(context) }
                )
            }
            item {
                Note(stringResource(R.string.status_samsung_note))
            }

            // ---- Bubble ----
            item { SectionHeader(stringResource(R.string.settings_bubble)) }
            item {
                SliderRow(
                    label = stringResource(R.string.bubble_size),
                    value = OverlayController.bubbleSize(context).toFloat(),
                    range = Prefs.MIN_BUBBLE_SIZE_DP.toFloat()..Prefs.MAX_BUBBLE_SIZE_DP.toFloat(),
                    display = { "${it.toInt()}dp" },
                    onChange = { OverlayController.setBubbleSize(context, it.toInt()); revision++ }
                )
            }
            item {
                SliderRow(
                    label = stringResource(R.string.bubble_tuck),
                    value = OverlayController.tuckDelay(context).toFloat(),
                    range = 0f..Prefs.MAX_TUCK_DELAY_SEC.toFloat(),
                    display = { if (it.toInt() == 0) stringResourceNever() else "${it.toInt()}s" },
                    onChange = { OverlayController.setTuckDelay(context, it.toInt()); revision++ }
                )
            }
            item { Note(stringResource(R.string.bubble_tuck_note)) }
            item {
                SwitchRow(
                    label = stringResource(R.string.bubble_autostart),
                    checked = OverlayController.autoStartOnBoot(context),
                    onChange = { OverlayController.setAutoStartOnBoot(context, it); revision++ }
                )
            }

            // ---- Clip list ----
            item { SectionHeader(stringResource(R.string.settings_list)) }
            item {
                SliderRow(
                    label = stringResource(R.string.list_opacity),
                    value = OverlayController.popupAlpha(context).toFloat(),
                    range = Prefs.MIN_ALPHA.toFloat()..100f,
                    display = { "${it.toInt()}%" },
                    onChange = { OverlayController.setPopupAlpha(context, it.toInt()); revision++ }
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.list_confirm_paste),
                    checked = OverlayController.confirmBeforePaste(context),
                    onChange = { OverlayController.setConfirmBeforePaste(context, it); revision++ }
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.list_close_outside),
                    checked = OverlayController.closeOnOutsideTouch(context),
                    onChange = {
                        OverlayController.setCloseOnOutsideTouch(context, it); revision++
                    }
                )
            }
            item { Note(stringResource(R.string.list_close_outside_note)) }
            item { Note(stringResource(R.string.list_resize_note)) }

            // ---- History ----
            item { SectionHeader(stringResource(R.string.settings_history)) }
            item {
                ChipRow(
                    label = stringResource(R.string.history_keep),
                    options = KEEP_OPTIONS,
                    selected = OverlayController.maxClips(context),
                    display = { if (it == 0) stringResource(R.string.no_limit) else it.toString() },
                    onSelect = { OverlayController.setMaxClips(context, it); revision++ }
                )
            }
            item {
                ActionRow(
                    label = stringResource(R.string.history_export),
                    onClick = onExport
                )
            }
            item {
                ActionRow(
                    label = stringResource(R.string.history_import),
                    onClick = onImport
                )
            }
            item {
                ActionRow(
                    label = stringResource(R.string.history_clear),
                    destructive = true,
                    onClick = { confirmingClear = true }
                )
            }

            // ---- Appearance ----
            item { SectionHeader(stringResource(R.string.settings_appearance)) }
            item {
                val mode = OverlayController.themeMode(context)
                ChipRow(
                    label = stringResource(R.string.theme),
                    options = listOf(Prefs.THEME_SYSTEM, Prefs.THEME_LIGHT, Prefs.THEME_DARK),
                    selected = mode,
                    display = {
                        stringResource(
                            when (it) {
                                Prefs.THEME_LIGHT -> R.string.theme_light
                                Prefs.THEME_DARK -> R.string.theme_dark
                                else -> R.string.theme_system
                            }
                        )
                    },
                    onSelect = { OverlayController.setThemeMode(context, it); revision++ }
                )
            }

            // ---- Which build this is ----
            //
            // The APK on the release page carries its version in its file
            // name; this is the other half of that pair. Without it there is
            // no way to tell, from the phone, whether the thing you just
            // installed is the thing you just downloaded.
            item { Note(appVersion(context)) }
        }
        }
    }

    if (confirmingClear) {
        ConfirmClearDialog(
            onConfirm = {
                confirmingClear = false
                onClearAll()
            },
            onDismiss = { confirmingClear = false }
        )
    }
}

/**
 * "Delete all clips?", asked once.
 *
 * The verbs are the answer — "Delete all" and "Cancel", never "OK" — so the
 * button says what pressing it does. Delete is the filled one because it is
 * the action the user came here for; the danger colour on its label says
 * what kind of action that is, and the words say it too.
 */
@Composable
private fun ConfirmClearDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = Tokens.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(Radius.container),
        title = {
            Text(
                text = stringResource(R.string.history_clear_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
        },
        text = {
            Text(
                text = stringResource(R.string.history_clear_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSoft
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = MinTouchTarget),
                shape = RoundedCornerShape(Radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceSunken,
                    contentColor = colors.danger
                )
            ) {
                Text(
                    text = stringResource(R.string.history_clear_confirm),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = MinTouchTarget)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ink
                )
            }
        }
    )
}

/**
 * "DevClip 1.1.0 (2)" — the name, the version, and the code Android compares.
 *
 * Read from the installed package rather than from BuildConfig, so it is
 * what the phone believes it is running, not what the source said at the
 * moment it was compiled.
 */
private fun appVersion(context: android.content.Context): String = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION") info.versionCode.toLong()
    }
    "DevClip ${info.versionName} ($code)"
} catch (e: Exception) {
    "DevClip"
}

/** The clip limits offered. 0 means no limit. */
private val KEEP_OPTIONS = listOf(100, 500, 1000, 0)

@Composable
private fun stringResourceNever() = stringResource(R.string.never)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Tokens.colors.ink,
        // A heading to TalkBack as well as to the eye, so the rotor can jump
        // section to section instead of row by row down a long screen.
        modifier = Modifier
            .padding(top = Space.xl, bottom = Space.sm)
            .semantics { heading() }
    )
}

/** How a status row reads: working, wrong, or off because the user chose it. */
private enum class Health { Ok, Problem, Idle }

/**
 * A fact and what to do about it.
 *
 * The dot is never the only signal — the words beside it say the same thing,
 * because One UI does not allow colour alone to carry meaning and because a
 * green dot means nothing to someone who cannot see it as green. The three
 * states differ in shape as well: filled for working, filled in caution for
 * a problem, and a hollow ring for off-by-choice, which is not a colour at
 * all and so cannot be mistaken for either.
 */
@Composable
private fun StatusRow(label: String, health: Health, detail: String, onClick: () -> Unit) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = MinTouchTarget)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dot = Modifier.size(StatusDot)
        Box(
            modifier = when (health) {
                Health.Ok -> dot.background(colors.success, CircleShape)
                Health.Problem -> dot.background(colors.warning, CircleShape)
                Health.Idle -> dot.border(StatusRing, colors.inkFaint, CircleShape)
            }
        )
        Column(modifier = Modifier.padding(start = Space.md).weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
        }
    }
}

/**
 * A labelled switch, as one control.
 *
 * The whole row toggles, and TalkBack hears "Start after reboot, on,
 * switch" as one thing. The switch used to be its own target with the label
 * a separate piece of text beside it, so the screen reader announced "On,
 * switch" with no word for what was on.
 */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = MinTouchTarget)
            .padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            // Null: the row owns the toggle. A second handler here would be a
            // second focus stop saying the same thing without the label.
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.inkFaint,
                uncheckedTrackColor = colors.surfaceSunken
            )
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: @Composable (Float) -> String,
    onChange: (Float) -> Unit
) {
    val colors = Tokens.colors
    var live by remember(value) { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .padding(horizontal = Space.lg, vertical = Space.md)
    ) {
        val shown = display(live)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            Text(shown, style = MaterialTheme.typography.bodySmall, color = colors.inkSoft)
        }
        Slider(
            // Named, and read out in the units on screen. Unlabelled, TalkBack
            // said "56 percent, slider" for a bubble size in dp.
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = shown
            },
            value = live,
            onValueChange = { live = it },
            // Written on release, not on every pixel of the drag: each change
            // is a disk write and an Intent to the service.
            onValueChangeFinished = { onChange(live) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceSunken
            )
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Tokens.colors.inkFaint,
        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm)
    )
}

/** One choice from a short, fixed set. */
@Composable
private fun <T> ChipRow(
    label: String,
    options: List<T>,
    selected: T,
    display: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    val colors = Tokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .padding(horizontal = Space.lg, vertical = Space.md)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
        // A group of radio buttons, to TalkBack: "Dark, selected, 3 of 3".
        // The fill was the only thing marking the choice, which a screen
        // reader cannot see.
        Row(
            modifier = Modifier
                .padding(top = Space.md)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            options.forEach { option ->
                val active = option == selected
                Box(
                    modifier = Modifier
                        .background(
                            if (active) colors.accent else colors.surfaceSunken,
                            // Pills, as One UI has them.
                            RoundedCornerShape(Radius.pill)
                        )
                        .selectable(
                            selected = active,
                            role = Role.RadioButton,
                            onClick = { onSelect(option) }
                        )
                        .heightIn(min = MinTouchTarget)
                        .padding(horizontal = Space.lg, vertical = Space.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = display(option),
                        style = MaterialTheme.typography.bodyMedium,
                        // Selected differs in fill as well as colour: One UI
                        // does not allow colour alone to carry state.
                        color = if (active) colors.onAccent else colors.inkSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = MinTouchTarget)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) colors.danger else colors.ink
        )
    }
}
