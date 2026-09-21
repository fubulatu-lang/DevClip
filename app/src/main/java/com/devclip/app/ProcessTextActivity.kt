package com.devclip.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * "DevClip" in Android's text-selection menu.
 *
 * The bubble reads selections out of the accessibility tree, and that is the
 * one thing about this app that cannot be made to work everywhere. A
 * selection inside a WebView — Chrome, and every app that renders its text in
 * one — is simply not in the tree in any form the walk can find. No amount of
 * widening the search reaches it, because there is nothing there to find.
 *
 * ACTION_PROCESS_TEXT is Android's own answer to that. Any app can add an
 * entry to the Copy / Share / … menu, and the system hands it the selected
 * text directly, having read it with knowledge DevClip does not have. It
 * works in every app that shows that menu, including the ones the tree walk
 * cannot see into.
 *
 * No UI of its own. It appears, saves, says so, and is gone — the menu item
 * *is* the interface, and anything else would put a screen between the user
 * and a thing they have already asked for. Translucent and
 * excludeFromRecents in the manifest for the same reason.
 */
class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // One extra carries the text, whether or not it can be edited.
        // EXTRA_PROCESS_TEXT_READONLY is a *boolean* saying which it is, not
        // a second copy of the text — reading it as one returns null forever
        // and looks like a working fallback. DevClip does not care either
        // way: it never sends a replacement back, so read-only and editable
        // selections are handled identically.
        val text = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()
            ?.trim()

        val message = when {
            text.isNullOrEmpty() -> getString(R.string.devclip_capture_failed)
            else -> when (val outcome = Capture.save(this, text)) {
                is Capture.Outcome.Saved -> outcome.message
                is Capture.Outcome.Duplicate -> getString(R.string.devclip_capture_duplicate)
                is Capture.Outcome.Password -> getString(R.string.devclip_capture_password)
                is Capture.Outcome.NoSelection -> getString(R.string.devclip_capture_failed)
                is Capture.Outcome.Failed -> getString(R.string.devclip_capture_failed)
            }
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // Nothing is handed back. Returning EXTRA_PROCESS_TEXT would replace
        // what the user highlighted with whatever was returned, and this
        // saves a copy — it does not edit anything.
        setResult(RESULT_OK)
        finish()
    }
}
