package com.devclip.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes clips off the main thread.
 *
 * A helper is opened and closed per call rather than held. The database is
 * shared with the capture path, which runs in a foreground service that
 * outlives every screen and writes while the app is closed; a long-lived
 * handle here would be a handle onto a file another process-level component
 * is also writing, kept alive for no reason once the screen is gone.
 *
 * The floating list does the same reads on the main thread deliberately —
 * there it is a bounded read in response to a tap, with a finger waiting.
 * Here there is a screen that can show a list a frame later, so there is no
 * excuse.
 */
object ClipRepository {

    private suspend fun <T> withDb(context: Context, block: (DevClipDatabaseHelper) -> T): T =
        withContext(Dispatchers.IO) {
            val helper = DevClipDatabaseHelper(context.applicationContext)
            try {
                block(helper)
            } finally {
                try { helper.close() } catch (e: Exception) { }
            }
        }

    /**
     * How much of each clip a list reads.
     *
     * Enough to fill three lines of the widest card with room to spare, and
     * nowhere near the megabyte a single clip can be. The floating list reads
     * the same amount for the same reason.
     */
    const val PREVIEW_CHARS = 400

    /** The list, as previews. Paste and edit go back for the whole clip with [get]. */
    suspend fun load(context: Context, query: String): List<DevClipDatabaseHelper.Clip> =
        withDb(context) { db ->
            if (query.isBlank()) db.listClips(Int.MAX_VALUE, PREVIEW_CHARS)
            else db.searchClips(query, PREVIEW_CHARS)
        }

    /** One clip in full, or null if it was deleted while the list was showing. */
    suspend fun get(context: Context, id: Long): DevClipDatabaseHelper.Clip? =
        withDb(context) { it.getClip(id) }

    suspend fun count(context: Context): Int = withDb(context) { it.countClips() }

    suspend fun update(context: Context, id: Long, content: String, title: String?) =
        withDb(context) { it.updateClip(id, content, title) }

    /**
     * Deletes a clip and hands back everything needed to undo it.
     *
     * Read before the delete, in the same call, so the undo restores what was
     * actually removed rather than what the screen last showed — which may
     * have been a preview, or an edit ago.
     */
    suspend fun delete(context: Context, id: Long): DevClipDatabaseHelper.StoredClip? =
        withDb(context) { db ->
            val stored = db.getStoredClip(id)
            db.deleteClip(id)
            stored
        }

    suspend fun restore(context: Context, clip: DevClipDatabaseHelper.StoredClip): Boolean =
        withDb(context) { it.restoreClip(clip) }

    suspend fun clearAll(context: Context) = withDb(context) { it.deleteAllClips() }

    suspend fun trim(context: Context, max: Int) = withDb(context) { it.trimToMax(max) }
}
