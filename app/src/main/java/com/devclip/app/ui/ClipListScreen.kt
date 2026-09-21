package com.devclip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devclip.app.ClipRepository
import com.devclip.app.DevClipDatabaseHelper
import com.devclip.app.DevClipEvents
import com.devclip.app.OverlayController
import com.devclip.app.R
import kotlinx.coroutines.launch

/**
 * The clip list: the whole history, searchable, and the way into editing.
 *
 * Laid out the way One UI asks. The title is large and lives in the viewing
 * area, collapsing into the bar as the list scrolls; what the user touches
 * sits low, within reach of a thumb. The old screen put its one action in a
 * bar at the bottom already — this keeps that and stops the title being a
 * fixed 17sp line that wasted the top of the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipListScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = Tokens.colors

    var query by remember { mutableStateOf("") }
    var clips by remember { mutableStateOf<List<DevClipDatabaseHelper.Clip>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DevClipDatabaseHelper.Clip?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        clips = ClipRepository.load(context, query)
        loaded = true
    }

    // Re-reads whenever the query changes, and once on arrival.
    LaunchedEffect(query) { reload() }

    // A capture while this screen is open has to appear in it. The event is
    // only a prompt to look — the database is what is true.
    LaunchedEffect(Unit) {
        DevClipEvents.clipsChanged.collect { reload() }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.bg,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.clips_title)) },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
                    ) {
                        StrokeGlyph(
                            glyph = Glyph.Settings,
                            tint = colors.ink,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = colors.bg,
                    scrolledContainerColor = colors.bg,
                    titleContentColor = colors.ink,
                    actionIconContentColor = colors.ink
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.keyline, vertical = Space.sm)
            )

            if (clips.isEmpty()) {
                EmptyState(
                    loaded = loaded,
                    searching = query.isNotBlank(),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = Space.keyline,
                        end = Space.keyline,
                        top = Space.sm,
                        bottom = Space.keyline
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    items(clips, key = { it.id }) { clip ->
                        ClipRow(
                            clip = clip,
                            position = clips.indexOf(clip) + 1,
                            onPaste = {
                                val pasted = OverlayController.paste(clip.content)
                                message = context.getString(
                                    if (pasted) R.string.paste_done else R.string.devclip_paste_copied_only
                                )
                            },
                            onEdit = { editing = clip }
                        )
                    }
                }
            }
        }
    }

    editing?.let { clip ->
        EditClipSheet(
            clip = clip,
            onDismiss = { editing = null },
            onSave = { content, title ->
                scope.launch {
                    ClipRepository.update(context, clip.id, content, title)
                    editing = null
                    reload()
                }
            },
            onDelete = {
                scope.launch {
                    ClipRepository.delete(context, clip.id)
                    editing = null
                    reload()
                }
            }
        )
    }

    message?.let {
        Snack(text = it, onDismiss = { message = null })
    }
}

/**
 * One clip.
 *
 * Tap pastes, hold edits — the same pair the floating list uses for the same
 * reason: pasting is what this is for, and editing is the occasional thing
 * that should not be one mis-tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipRow(
    clip: DevClipDatabaseHelper.Clip,
    position: Int,
    onPaste: () -> Unit,
    onEdit: () -> Unit
) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .combinedClickable(onClick = onPaste, onLongClick = onEdit)
            .padding(Space.lg)
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append(position).append(". ")
                    clip.title?.takeIf { it.isNotBlank() }?.let { append(it).append(": ") }
                    append(clip.content)
                }
            },
        verticalAlignment = Alignment.Top
    ) {
        // The row number. Positional, not an identity: it renumbers as clips
        // arrive and are deleted, which is what makes "the third one down"
        // useful.
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(colors.surfaceSunken, RoundedCornerShape(Radius.sm)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = position.toString(),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = colors.inkSoft
            )
        }

        Column(modifier = Modifier.padding(start = Space.md)) {
            clip.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = clip.content,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = colors.inkSoft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState(loaded: Boolean, searching: Boolean, modifier: Modifier = Modifier) {
    val colors = Tokens.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = when {
                !loaded -> stringResource(R.string.clips_loading)
                searching -> stringResource(R.string.clips_no_matches)
                else -> stringResource(R.string.clips_empty)
            },
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = colors.inkFaint,
            modifier = Modifier.padding(horizontal = Space.keyline)
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Tokens.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = MinTouchTarget),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        singleLine = true,
        shape = RoundedCornerShape(Radius.pill),
        keyboardOptions = KeyboardOptions.Default,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
            cursorColor = colors.accent
        )
    )
}
