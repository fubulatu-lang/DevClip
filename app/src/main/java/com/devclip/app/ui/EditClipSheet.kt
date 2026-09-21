package com.devclip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devclip.app.DevClipDatabaseHelper
import com.devclip.app.R

/**
 * Editing one clip.
 *
 * The text field is bounded, and that is the whole point of the shape it has.
 * In the version this replaces the field had a minimum height and no maximum,
 * so a long clip made it as tall as its own text; tapping near the bottom put
 * the caret where the keyboard was about to appear and nothing moved it,
 * because the scrolling parent tracked the field as one object and had no
 * idea where the caret was inside it.
 *
 * With a ceiling the field scrolls itself and the platform keeps the caret in
 * view, the way it does in every other text box on the phone. The ceiling
 * comes from the live window height, which the keyboard has already shrunk by
 * the time it matters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClipSheet(
    clip: DevClipDatabaseHelper.Clip,
    onDismiss: () -> Unit,
    onSave: (content: String, title: String?) -> Unit,
    onDelete: () -> Unit
) {
    val colors = Tokens.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(clip.id) { mutableStateOf(clip.title.orEmpty()) }
    var content by remember(clip.id) { mutableStateOf(clip.content) }

    val windowHeight = LocalConfiguration.current.screenHeightDp.dp
    val contentCeiling = (windowHeight * 0.35f).coerceAtLeast(120.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = colors.scrim,
        shape = RoundedCornerShape(topStart = Radius.container, topEnd = Radius.container)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.keyline)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.edit_title_label),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSoft,
                modifier = Modifier.padding(bottom = Space.sm)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.edit_title_placeholder)) },
                shape = RoundedCornerShape(Radius.md),
                colors = fieldColors()
            )

            Text(
                text = stringResource(R.string.edit_content_label),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSoft,
                modifier = Modifier.padding(top = Space.lg, bottom = Space.sm)
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = contentCeiling),
                shape = RoundedCornerShape(Radius.md),
                colors = fieldColors()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.xl, bottom = Space.keyline),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.heightIn(min = MinTouchTarget)
                ) {
                    Text(
                        text = stringResource(R.string.edit_delete),
                        color = colors.danger,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

                Button(
                    onClick = { onSave(content, title.trim().ifBlank { null }) },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                    // One UI buttons are pills. A 4dp radius reads as Material.
                    shape = RoundedCornerShape(Radius.pill),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    )
                ) {
                    Text(
                        text = stringResource(R.string.edit_save),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Tokens.colors.surfaceSunken,
    unfocusedContainerColor = Tokens.colors.surfaceSunken,
    focusedBorderColor = Tokens.colors.accent,
    unfocusedBorderColor = Tokens.colors.border,
    focusedTextColor = Tokens.colors.ink,
    unfocusedTextColor = Tokens.colors.ink,
    cursorColor = Tokens.colors.accent
)
