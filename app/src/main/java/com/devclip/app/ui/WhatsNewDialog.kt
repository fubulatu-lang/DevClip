package com.devclip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.devclip.app.Changelog
import com.devclip.app.R

/**
 * "What's new": every release, newest first.
 *
 * The same dialog serves the automatic popup after an update and the version
 * link at the bottom of Settings. It scrolls; the history is long by design,
 * and the newest entry is at the top.
 *
 * The newest version's chip is filled in ink and every older one is a quiet
 * grey, so which entry is new is answered before anything is read — in
 * monochrome, by weight rather than by colour, which is the only way this
 * design system has to say it.
 */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val colors = Tokens.colors
    val entries = Changelog.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(Radius.container),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Glyphs.History,
                    contentDescription = null,
                    tint = colors.ink
                )
                Spacer(Modifier.width(Space.md))
                Text(
                    text = stringResource(R.string.whats_new_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() }
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEachIndexed { index, entry ->
                    val newest = index == 0
                    val versionLabel = stringResource(R.string.whats_new_version, entry.version)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = Space.sm)
                            // One heading per release, read as "Version
                            // 1.4.0: <title>", so a screen reader can jump
                            // from release to release.
                            .clearAndSetSemantics {
                                heading()
                                contentDescription = "$versionLabel: ${entry.title}"
                            }
                    ) {
                        Text(
                            text = "v${entry.version}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (newest) colors.onAccent else colors.inkSoft,
                            modifier = Modifier
                                .background(
                                    if (newest) colors.accent else colors.surfaceSunken,
                                    RoundedCornerShape(Radius.pill)
                                )
                                .padding(horizontal = Space.sm, vertical = Space.xs)
                        )
                        Spacer(Modifier.width(Space.sm))
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        entry.items.forEach { item ->
                            Row {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.inkSoft,
                                    modifier = Modifier.padding(end = Space.sm)
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.inkSoft
                                )
                            }
                        }
                    }
                    if (index < entries.lastIndex) {
                        HorizontalDivider(
                            color = colors.divider,
                            modifier = Modifier.padding(vertical = Space.lg)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = MinTouchTarget)
            ) {
                Text(
                    text = stringResource(R.string.whats_new_close),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ink
                )
            }
        }
    )
}

/**
 * The version, at the foot of Settings, as the way into [WhatsNewDialog].
 *
 * A quiet closing line about the app itself rather than a setting: small,
 * centred, underlined so it reads as something to tap. The whole row is the
 * target, at One UI's minimum height.
 */
@Composable
fun VersionLink(label: String, onClick: () -> Unit) {
    val colors = Tokens.colors
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .padding(top = Space.lg)
    ) {
        Icon(
            imageVector = Glyphs.History,
            contentDescription = null,
            tint = colors.inkSoft,
            modifier = Modifier.size(IconSm)
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
            ),
            color = colors.inkSoft
        )
    }
}
