package com.devclip.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * What happens when the bubble is tapped with text selected.
 *
 * Kept out of OverlayService, which is about windows. This is about a clip.
 */
object Capture {

    /**
     * Beyond roughly this many characters, the handoff between apps stops
     * being reliable.
     *
     * Everything between two apps crosses a ~1MB Binder transaction buffer
     * that is shared across the whole process, so the real ceiling is lower
     * than 1MB and moves with whatever else is in flight. DevClip's own
     * database has no such limit — this is purely about what Android will
     * carry — so a clip this size is saved and the user is told the number,
     * rather than being quietly handed a fraction of what they highlighted.
     *
     * For scale: a very long web article is around 50,000 characters.
     */
    private const val LARGE_CLIP_CHARS = 100_000

    /** How much of the clip goes in the confirmation message. */
    private const val PREVIEW_CHARS = 42

    sealed class Outcome {
        /**
         * Saved. [message] confirms *what* was saved — the first few words —
         * because reading a selection is not perfect across every app, and
         * seeing the right words is how the user knows it worked.
         */
        data class Saved(val preview: String, val message: String) : Outcome()

        /** Identical to the clip already at the top. Almost always a double tap. */
        object Duplicate : Outcome()

        /** The selection was in a password field. Deliberately not saved. */
        object Password : Outcome()

        /** Nothing selected. The tap means "open the list" instead. */
        object NoSelection : Outcome()

        /** Something went wrong and the user should be told so. */
        object Failed : Outcome()
    }

    /**
     * Reads the selection, saves it, and then — and only then — puts it on the
     * system clipboard.
     *
     * The order is deliberate. The clipboard write is the step that can fail
     * on a large clip, because it has to cross to another process; the
     * database write does not. Saving first means a clip too big for Android
     * to carry is still in DevClip.
     */
    fun attempt(context: Context): Outcome {
        val selection = SelectionCapture.capture(ClipboardAccessibilityService.instance)
        val text = when (selection) {
            is SelectionCapture.Result.Text -> selection.value
            is SelectionCapture.Result.Password -> return Outcome.Password
            is SelectionCapture.Result.None -> return Outcome.NoSelection
        }

        val helper = DevClipDatabaseHelper(context.applicationContext)
        val saved = try {
            val result = helper.insertClip(text)
            if (result == DevClipDatabaseHelper.InsertResult.SAVED) {
                // The user's limit has to hold when the app is shut, which is
                // when almost all capture happens.
                helper.trimToMax(maxClips(context))
            }
            result
        } finally {
            try {
                helper.close()
            } catch (e: Exception) {
                // Nothing useful to do; the write already happened.
            }
        }

        when (saved) {
            DevClipDatabaseHelper.InsertResult.DUPLICATE -> return Outcome.Duplicate
            DevClipDatabaseHelper.InsertResult.FAILED -> return Outcome.Failed
            DevClipDatabaseHelper.InsertResult.SAVED -> Unit
        }

        val onClipboard = writeClipboard(context, text)
        val preview = preview(text)

        DevClipEvents.emitClipsChanged(preview)

        val message = when {
            !onClipboard -> context.getString(R.string.devclip_capture_saved_no_clipboard, preview)
            text.length >= LARGE_CLIP_CHARS ->
                context.getString(R.string.devclip_capture_saved_large, text.length)
            else -> context.getString(R.string.devclip_capture_saved, preview)
        }

        return Outcome.Saved(preview, message)
    }

    /**
     * Puts the clip on the system clipboard, so the bubble replaces Android's
     * own Copy button rather than sitting beside it.
     *
     * Writing is allowed from the background — it is only *reading* that
     * Android gates on window focus. That asymmetry is the reason capture
     * cannot use the clipboard and this step can.
     */
    private fun writeClipboard(context: Context, text: String): Boolean =
        try {
            val manager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            manager?.setPrimaryClip(ClipData.newPlainText("DevClip", text))
            manager != null
        } catch (e: Exception) {
            // A clip too large for the transaction buffer lands here. The clip
            // is already saved, so this is a message, not a failure.
            android.util.Log.w("DevClip", "Android refused the clipboard write", e)
            false
        }

    private fun maxClips(context: Context): Int =
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
            .getInt(Prefs.KEY_MAX_CLIPS, Prefs.DEFAULT_MAX_CLIPS)

    /** First few words, on one line. */
    fun preview(text: String): String {
        val flat = text.trim().replace(Regex("\\s+"), " ")
        return if (flat.length <= PREVIEW_CHARS) flat
        else flat.substring(0, PREVIEW_CHARS).trimEnd() + "…"
    }
}
