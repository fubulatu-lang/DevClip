package com.devclip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.devclip.app.Capture
import com.devclip.app.ClipRepository
import com.devclip.app.DevClipDatabaseHelper
import com.devclip.app.DevClipEvents
import com.devclip.app.OverlayController
import com.devclip.app.R
import kotlinx.coroutines.delay
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
    // What the sheet is showing: a clip opened from the list, a new note,
    // or nothing. One state, so the two can never be open at once.
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var showingChangelog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<SnackMessage?>(null) }

    val header = rememberOneUiHeaderState()
    val layout = rememberWindowLayout()

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

    // Re-reads whenever the query changes, and once on arrival. A typed
    // query waits for a pause first: every keystroke was a full-table LIKE,
    // and the results of "d", "de" and "dev" are not worth reading on the
    // way to "devclip". Relaunching the effect cancels the wait.
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(SEARCH_SETTLE_MS)
        reload()
    }

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
                    leading = { f ->
                        AppIcon(size = AppIconMinSize + (AppIconMaxSize - AppIconMinSize) * f)
                    },
                    actionsWidth = actionsWidthFor(4),
                    actions = {
                        HeaderIcon(
                            icon = Glyphs.Add,
                            contentDescription = stringResource(R.string.note_add)
                        ) { sheet = Sheet.New }

                        // Brings the bubble up from wherever it is: off,
                        // hidden by the drag-to-hide target, or tucked at the
                        // edge. Without the overlay permission there is no
                        // bubble to bring, so the button goes to the switch
                        // that allows one instead of failing silently.
                        HeaderIcon(
                            icon = Glyphs.Bubble,
                            contentDescription = stringResource(R.string.bubble_show)
                        ) {
                            message = if (OverlayController.startBubble(context)) {
                                SnackMessage(context.getString(R.string.bubble_shown))
                            } else {
                                OverlayController.requestOverlay(context)
                                SnackMessage(context.getString(R.string.bubble_needs_overlay))
                            }
                        }

                        HeaderIcon(
                            icon = Glyphs.Search,
                            contentDescription = stringResource(R.string.search_open)
                        ) { searching = true }

                        Box {
                            HeaderIcon(
                                icon = Glyphs.More,
                                contentDescription = stringResource(R.string.more_options)
                            ) { menuOpen = true }

                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                containerColor = colors.surface
                            ) {
                                MenuItem(
                                    icon = Glyphs.History,
                                    label = stringResource(R.string.whats_new_title)
                                ) {
                                    menuOpen = false
                                    showingChangelog = true
                                }
                                MenuItem(
                                    icon = Glyphs.Settings,
                                    label = stringResource(R.string.settings_title)
                                ) {
                                    menuOpen = false
                                    onOpenSettings()
                                }
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
                // Two columns once the window is wide enough to give each a
                // readable measure, rather than one card stretched across a
                // tablet. The margin follows One UI's adaptive rule; below
                // 589dp both are exactly what they always were.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(layout.columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = layout.margin,
                        end = layout.margin,
                        top = Space.sm,
                        bottom = Space.keyline
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    itemsIndexed(clips, key = { _, clip -> clip.id }) { index, clip ->
                        ClipRow(
                            clip = clip,
                            position = index + 1,
                            onOpen = {
                                scope.launch {
                                    // The row holds a preview. What opens is
                                    // the whole clip, read again by id.
                                    val full = ClipRepository.get(context, clip.id)
                                    if (full == null) {
                                        message = SnackMessage(context.getString(R.string.clip_gone))
                                        reload()
                                    } else {
                                        sheet = Sheet.Open(full)
                                    }
                                }
                            },
                            onCopy = {
                                scope.launch {
                                    val full = ClipRepository.get(context, clip.id)
                                    if (full == null) {
                                        message = SnackMessage(context.getString(R.string.clip_gone))
                                        reload()
                                    } else {
                                        copyMessage(context, full.content)?.let { message = it }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    when (val current = sheet) {
        null -> Unit
        Sheet.New -> EditClipSheet(
            clip = null,
            onDismiss = { sheet = null },
            onSave = { content, title ->
                scope.launch {
                    val saved = ClipRepository.addNote(context, content, title)
                    // Kept open on failure: closing it would throw away
                    // what was just typed, with nothing to show for it.
                    if (saved) sheet = null
                    message = SnackMessage(
                        context.getString(if (saved) R.string.note_saved else R.string.note_save_failed)
                    )
                    reload()
                }
            }
        )
        is Sheet.Open -> {
            val clip = current.clip
            EditClipSheet(
                clip = clip,
                onDismiss = { sheet = null },
                onCopy = { content ->
                    copyMessage(context, content)?.let { message = it }
                },
                onSave = { content, title ->
                    scope.launch {
                        ClipRepository.update(context, clip.id, content, title)
                        sheet = null
                        reload()
                    }
                },
                onDelete = {
                    scope.launch {
                        val removed = ClipRepository.delete(context, clip.id)
                        sheet = null
                        reload()
                        // Delete is immediate and Undo is how it is taken back:
                        // news with a way out, not a question asked beforehand.
                        // "Never lose a clip" has to include the one deleted by a
                        // thumb that meant Save.
                        message = SnackMessage(
                            text = context.getString(R.string.clip_deleted),
                            actionLabel = removed?.let { context.getString(R.string.undo) },
                            onAction = removed?.let { stored ->
                                {
                                    scope.launch {
                                        val back = ClipRepository.restore(context, stored)
                                        reload()
                                        if (!back) {
                                            message = SnackMessage(
                                                context.getString(R.string.clip_restore_failed)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    if (showingChangelog) {
        WhatsNewDialog(onDismiss = { showingChangelog = false })
    }

    // Keyed on the message itself, not its words: two deletes in a row say
    // the same thing, and without the key the second would inherit the
    // first one's timer — and the Undo would vanish early.
    message?.let {
        key(it) {
            Snack(
                text = it.text,
                actionLabel = it.actionLabel,
                onAction = it.onAction,
                onDismiss = { message = null }
            )
        }
    }
}

/** What the snack says, and the one thing it offers to do about it, if any. */
data class SnackMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * What to say after a copy, or null to say nothing.
 *
 * From Android 13 the system shows its own confirmation whenever anything
 * lands on the clipboard, so a snack saying "Copied" as well is the same news
 * twice, stacked. Below 13 there is no such thing and the snack is the only
 * sign it worked. A failure is always reported: the system says nothing then.
 */
private fun copyMessage(context: android.content.Context, text: String): SnackMessage? {
    val ok = Capture.writeClipboard(context, text)
    return when {
        !ok -> SnackMessage(context.getString(R.string.copy_failed))
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> null
        else -> SnackMessage(context.getString(R.string.copied))
    }
}

/** What the bottom sheet is open on. */
private sealed interface Sheet {
    data object New : Sheet
    data class Open(val clip: DevClipDatabaseHelper.Clip) : Sheet
}

/** An overflow menu entry: a symbol and a label, at a full touch target. */
@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = Tokens.colors
    DropdownMenuItem(
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = colors.ink) },
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink
            )
        },
        onClick = onClick,
        modifier = Modifier.heightIn(min = MinTouchTarget)
    )
}

/** How long typing has to pause before a search runs. */
private const val SEARCH_SETTLE_MS = 150L

/**
 * One clip.
 *
 * Tap opens it. This screen is where clips are read, kept and changed; it
 * used to paste on a tap, which from the launcher app had nowhere useful to
 * paste into and made reading a long clip a matter of luck. The floating
 * list is still tap-to-paste, because over another app that is the point.
 *
 * The button at the end copies, which is the one thing worth doing to a clip
 * without opening it. It is its own control, outside the row's description,
 * so a screen reader reaches it separately and says which clip it copies.
 */
@Composable
private fun ClipRow(
    clip: DevClipDatabaseHelper.Clip,
    position: Int,
    onOpen: () -> Unit,
    onCopy: () -> Unit
) {
    val colors = Tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Radius.md))
            .clip(RoundedCornerShape(Radius.md))
            .clickable(
                onClickLabel = stringResource(R.string.clip_open_action),
                onClick = onOpen
            )
            .padding(start = Space.lg, top = Space.md, bottom = Space.md, end = Space.xs),
        verticalAlignment = Alignment.Top
    ) {
        // Read as one sentence rather than a number, then a title, then a
        // body. The copy button stays outside this so it remains its own control.
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Space.xs)
                .clearAndSetSemantics {
                    contentDescription = buildString {
                        append(position).append(". ")
                        clip.title?.takeIf { it.isNotBlank() }?.let { append(it).append(": ") }
                        append(clip.content)
                    }
                },
            verticalAlignment = Alignment.Top
        ) {
            // The row number. Positional, not an identity: it renumbers as
            // clips arrive and are deleted, which is what makes "the third one
            // down" useful. A minimum rather than a size, so at large text
            // "100" widens the badge instead of spilling out of it.
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = BadgeMin, minHeight = BadgeMin)
                    .background(colors.surfaceSunken, RoundedCornerShape(Radius.sm))
                    .padding(horizontal = Space.xs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSoft,
                    textAlign = TextAlign.Center
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

        IconButton(onClick = onCopy, modifier = Modifier.size(MinTouchTarget)) {
            Icon(
                imageVector = Glyphs.Copy,
                contentDescription = stringResource(R.string.clip_copy_description, position),
                tint = colors.inkSoft,
                modifier = Modifier.size(IconSm)
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
                imageVector = Glyphs.Back,
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
                            imageVector = Glyphs.Close,
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
