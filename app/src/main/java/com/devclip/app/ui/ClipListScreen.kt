package com.devclip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
 * Laid out the way One UI asks. The title sits in the viewing area and opens
 * when the screen is pulled down, bringing search and the overflow menu with
 * it — on a phone this tall, the top corners are not somewhere a thumb goes.
 *
 * Search is an icon rather than a permanent field. A field pinned under the
 * title cost a row of list on every screen to serve the one time in twenty
 * the user is looking for something rather than reading what is there.
 */
@Composable
fun ClipListScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = Tokens.colors

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var clips by remember { mutableStateOf<List<DevClipDatabaseHelper.Clip>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DevClipDatabaseHelper.Clip?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val header = rememberOneUiHeaderState()

    // While searching, back means "leave search". Without this it means
    // "leave DevClip", which is a long way to fall out of a text field.
    BackHandler(enabled = searching) {
        searching = false
        query = ""
    }

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

    // The window draws edge to edge, so every screen owes the system bars
    // their room. The background is painted underneath them all the same:
    // insetting the colour as well as the content would leave two grey bands
    // on a screen whose whole point is one flat surface.
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
            if (searching) {
                SearchBar(
                    value = query,
                    onValueChange = { query = it },
                    onClose = {
                        searching = false
                        query = ""
                    }
                )
            } else {
                OneUiHeader(
                    title = stringResource(R.string.clips_title),
                    state = header,
                    actionsWidth = actionsWidthFor(2),
                    actions = {
                        HeaderIcon(
                            icon = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_open)
                        ) { searching = true }

                        Box {
                            HeaderIcon(
                                icon = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            ) { menuOpen = true }

                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                containerColor = colors.surface
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.settings_title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = colors.ink
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                )
            }

            if (clips.isEmpty()) {
                EmptyState(
                    loaded = loaded,
                    searching = query.isNotBlank(),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Space.keyline,
                        end = Space.keyline,
                        top = Space.sm,
                        bottom = Space.keyline
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    itemsIndexed(clips, key = { _, clip -> clip.id }) { index, clip ->
                        ClipRow(
                            clip = clip,
                            position = index + 1,
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
 *
 * combinedClickable is the only way to have both from one modifier, and it
 * is still a foundation API behind an opt-in.
 */
@OptIn(ExperimentalFoundationApi::class)
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
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSoft
            )
        }

        Column(modifier = Modifier.padding(start = Space.md)) {
            clip.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = clip.content,
                style = MaterialTheme.typography.bodyMedium,
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
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkFaint,
            modifier = Modifier.padding(horizontal = Space.keyline)
        )
    }
}

/**
 * Search, in place of the app bar rather than underneath it.
 *
 * One UI replaces the bar instead of stacking a field below it, so the list
 * keeps its full height and there is never a title above a search box the
 * title has nothing to do with. Back leaves search and clears the query,
 * which is the only reading of "back" that returns the list to what it was.
 */
@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = Tokens.colors
    val focus = remember { FocusRequester() }

    // Search opened by a tap on an icon should be ready to type into. Anything
    // else makes the user reach for the field they just asked for.
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(OneUiHeaderDefaults.CollapsedHeight)
            .padding(horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose, modifier = Modifier.size(MinTouchTarget)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.search_close),
                tint = colors.ink
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = Space.xs, end = Space.sm)
                .heightIn(min = MinTouchTarget)
                .focusRequester(focus),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(Radius.pill),
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(MinTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.search_clear),
                            tint = colors.inkSoft
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
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
}
