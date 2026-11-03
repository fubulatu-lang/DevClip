package com.devclip.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.devclip.app.ui.DevClipComposeTheme
import com.devclip.app.ui.EditClipSheet
import kotlinx.coroutines.launch

/**
 * The + in the floating list: a new note, written over the app the user is in.
 *
 * Nothing but the sheet. The window is translucent and the activity is kept
 * out of Recents and out of the launcher app's task, so it reads as a sheet
 * rising over the current app rather than DevClip opening — and finishing it
 * puts the user back exactly where they were, not on the clip list.
 *
 * The same sheet the + in the app opens, so a note is written the same way
 * from either place.
 */
class QuickNoteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DevClipComposeTheme {
                EditClipSheet(
                    clip = null,
                    onDismiss = { finish() },
                    onSave = { content, title ->
                        // The activity's scope, not the composition's: the
                        // write has to finish even if the sheet's own
                        // dismissal tears the composition down first.
                        lifecycleScope.launch {
                            val saved = ClipRepository.addNote(this@QuickNoteActivity, content, title)
                            Toast.makeText(
                                this@QuickNoteActivity,
                                if (saved) R.string.note_saved else R.string.note_save_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                            // Kept open on failure, so the text is not lost.
                            if (saved) finish()
                        }
                    }
                )
            }
        }
    }
}
