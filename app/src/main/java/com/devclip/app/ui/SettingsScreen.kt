package com.devclip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

    // Read once per composition rather than held in state: these are
    // SharedPreferences and system facts, and the screen is re-entered rather
    // than lived in. `revision` forces a re-read after anything writes.
    var revision by remember { mutableStateOf(0) }
    @Suppress("UNUSED_EXPRESSION") revision

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg,
                    titleContentColor = colors.ink
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Space.keyline,
                end = Space.keyline,
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
                    ok = working,
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
                val running = OverlayController.isBubbleRunning(context)
                StatusRow(
                    label = stringResource(R.string.status_bubble),
                    ok = running,
                    detail = stringResource(if (running) R.string.status_on else R.string.status_off),
                    onClick = {
                        if (running) OverlayController.stopBubble(context)
                        else OverlayController.startBubble(context)
                        revision++
                    }
                )
            }
            item {
                val optimised = OverlayController.isBatteryOptimised(context)
                StatusRow(
                    label = stringResource(R.string.status_battery),
                    ok = !optimised,
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
                    label = stringResource(R.string.bubble_opacity),
                    value = OverlayController.bubbleAlpha(context).toFloat(),
                    range = Prefs.MIN_ALPHA.toFloat()..100f,
                    display = { "${it.toInt()}%" },
                    onChange = { OverlayController.setBubbleAlpha(context, it.toInt()); revision++ }
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.bubble_fade_idle),
                    checked = OverlayController.bubbleIdleFade(context),
                    onChange = { OverlayController.setBubbleIdleFade(context, it); revision++ }
                )
            }
            item { Note(stringResource(R.string.bubble_fade_idle_note)) }
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
                    onClick = onClearAll
                )
            }

            // ---- Appearance ----
            item { SectionHeader(stringResource(R.string.settings_appearance)) }
            item {
                val mode = themeMode(context)
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
                    onSelect = { setThemeMode(context, it); revision++ }
                )
            }
        }
    }
}

/** The clip limits offered. 0 means no limit. */
private val KEEP_OPTIONS = listOf(100, 500, 1000, 0)

private fun themeMode(context: android.content.Context): String =
    context.getSharedPreferences(Prefs.NAME, android.content.Context.MODE_PRIVATE)
        .getString(Prefs.KEY_THEME_MODE, Prefs.THEME_SYSTEM) ?: Prefs.THEME_SYSTEM

private fun setThemeMode(context: android.content.Context, mode: String) {
    context.getSharedPreferences(Prefs.NAME, android.content.Context.MODE_PRIVATE)
        .edit().putString(Prefs.KEY_THEME_MODE, mode).apply()
}

@Composable
private fun stringResourceNever() = stringResource(R.string.never)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Tokens.colors.ink,
        modifier = Modifier.padding(top = Space.xl, bottom = Space.sm)
    )
}

/**
 * A fact and what to do about it.
 *
 * The dot is never the only signal — the words beside it say the same thing,
 * because One UI does not allow colour alone to carry meaning and because a
 * green dot means nothing to someone who cannot see it as green.
 */
@Composable
private fun StatusRow(label: String, ok: Boolean, detail: String, onClick: () -> Unit) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .heightIn(min = MinTouchTarget)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (ok) colors.success else colors.warning, CircleShape)
        )
        Column(modifier = Modifier.padding(start = Space.md).weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
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
            onCheckedChange = onChange,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            Text(display(live), style = MaterialTheme.typography.bodySmall, color = colors.inkSoft)
        }
        Slider(
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
        Row(
            modifier = Modifier.padding(top = Space.md),
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
                        .clickable { onSelect(option) }
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
            .clickable(onClick = onClick)
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
