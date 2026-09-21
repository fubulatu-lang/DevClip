package com.devclip.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Export and import, as JSON, through the Storage Access Framework.
 *
 * The user picks the file each time rather than DevClip holding a folder
 * permission. A backup is a snapshot taken deliberately, not a place the app
 * syncs to, and a persisted grant would be a standing claim on somebody's
 * storage for something that happens twice a year.
 *
 * org.json rather than a serialisation library: this is one flat array of
 * three fields, and the platform already ships a parser for it.
 */
object Backup {

    /** Bumped only when the shape changes in a way an old reader would misread. */
    private const val VERSION = 1

    data class ImportResult(val added: Int, val skipped: Int)

    suspend fun export(context: Context, target: Uri): Int = withContext(Dispatchers.IO) {
        val helper = DevClipDatabaseHelper(context.applicationContext)
        val clips = try {
            helper.listClips(Int.MAX_VALUE)
        } finally {
            try { helper.close() } catch (e: Exception) { }
        }

        val array = JSONArray()
        clips.forEach { clip ->
            array.put(
                JSONObject().apply {
                    put("title", clip.title ?: JSONObject.NULL)
                    put("content", clip.content)
                }
            )
        }
        val payload = JSONObject().apply {
            put("app", "DevClip")
            put("version", VERSION)
            put("clips", array)
        }

        context.contentResolver.openOutputStream(target)?.use { out ->
            out.write(payload.toString(2).toByteArray())
        } ?: throw IllegalStateException("Could not open that file for writing")

        clips.size
    }

    /**
     * Merges, and skips anything whose text is already stored.
     *
     * Merge rather than replace, because replacing makes restoring an old
     * backup a destructive act that looks like a safe one — newer clips would
     * vanish with no undo. Matching on text alone means importing the same
     * file twice does nothing the second time.
     */
    suspend fun import(context: Context, source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(source)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Could not open that file")

        val clips = parse(raw)
        val helper = DevClipDatabaseHelper(context.applicationContext)
        try {
            var added = 0
            var skipped = 0
            // insertClip only guards against the clip at the top, which is the
            // double-tap case. A whole file needs the set.
            val existing = helper.listClips(Int.MAX_VALUE).map { it.content }.toMutableSet()
            clips.forEach { (title, content) ->
                if (!existing.add(content)) {
                    skipped++
                } else {
                    helper.insertImported(title, content)
                    added++
                }
            }
            ImportResult(added, skipped)
        } finally {
            try { helper.close() } catch (e: Exception) { }
        }
    }

    /**
     * Reads a backup, and refuses anything it cannot vouch for.
     *
     * A file picker hands back whatever was chosen, which will sometimes not
     * be a DevClip backup. Failing loudly is the point: the alternative is
     * importing a hundred rows of nothing into a history with no undo.
     */
    private fun parse(raw: String): List<Pair<String?, String>> {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw IllegalArgumentException("That file is not a DevClip backup")
        }
        val array = root.optJSONArray("clips")
            ?: throw IllegalArgumentException("That file is not a DevClip backup")

        val out = ArrayList<Pair<String?, String>>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val content = item.optString("content", "")
            if (content.isEmpty()) continue
            val title = item.optString("title", "").ifBlank { null }
            out.add(title to content)
        }
        if (out.isEmpty()) throw IllegalArgumentException("That file has no clips in it")
        return out
    }
}
