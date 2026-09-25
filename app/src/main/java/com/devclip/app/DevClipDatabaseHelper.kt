package com.devclip.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/**
 * The clip history: one SQLite file, one flat `clips` table.
 *
 * It lives at `<filesDir>/SQLite/devclip.db`, which is where expo-sqlite kept
 * it when the app was React Native. The path stays because moving it would
 * orphan every clip saved before the conversion; nothing else about Expo
 * survives. The capture path in the services and the launcher app both open
 * this same file — it is the whole contract between them.
 */
class DevClipDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, resolveDbPath(context), null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "devclip.db"
        private const val DB_VERSION = 1

        private fun resolveDbPath(context: Context): String {
            val dir = File(context.filesDir, "SQLite")
            if (!dir.exists()) dir.mkdirs()
            return File(dir, DB_NAME).absolutePath
        }
    }

    /** What [insertClip] did, so the caller can tell the user the truth. */
    enum class InsertResult { SAVED, DUPLICATE, FAILED }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS clips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                sort_order INTEGER NOT NULL
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No schema changes yet. sort_order is unused since manual reordering
        // was removed, but the column stays — dropping one in SQLite means
        // rebuilding the table, and inserts still fill it so both sides agree
        // on the schema.
    }

    /**
     * Saves a captured clip.
     *
     * Duplicates are skipped only against the clip currently at the top. That
     * catches the double tap, which is the accident worth catching; capturing
     * the same phrase again ten clips later is a deliberate act and is kept.
     */
    fun insertClip(content: String): InsertResult {
        return try {
            val db = writableDatabase

            val isDuplicate = db.rawQuery(
                "SELECT content FROM clips ORDER BY created_at DESC, id DESC LIMIT 1;", null
            ).use { it.moveToFirst() && it.getString(0) == content }
            if (isDuplicate) return InsertResult.DUPLICATE

            val nextOrder = db.rawQuery("SELECT MAX(sort_order) FROM clips;", null)
                .use { if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) + 1 else 0 }

            db.execSQL(
                "INSERT INTO clips (title, content, created_at, sort_order) VALUES (NULL, ?, ?, ?);",
                arrayOf(content, System.currentTimeMillis(), nextOrder)
            )
            InsertResult.SAVED
        } catch (e: Exception) {
            android.util.Log.e("DevClip", "Could not save a captured clip", e)
            InsertResult.FAILED
        }
    }

    /**
     * One row of a clip list.
     *
     * [content] is the whole text unless the row was read as a preview, in
     * which case it is the start of it and [complete] is false. Anything that
     * leaves the list — a paste, an edit — reads the clip again by id with
     * [getClip] rather than trusting a preview to be all of it.
     */
    data class Clip(
        val id: Long,
        val title: String?,
        val content: String,
        val complete: Boolean = true
    )

    /**
     * Everything stored for one clip, as [restoreClip] needs it to put a
     * deleted clip back exactly where it was.
     */
    data class StoredClip(
        val id: Long,
        val title: String?,
        val content: String,
        val createdAt: Long,
        val sortOrder: Int
    )

    /**
     * The newest [limit] clips, newest first.
     *
     * Ordered by `created_at DESC, id DESC` to match the JS side exactly —
     * two clips captured inside the same millisecond would otherwise come back
     * in whatever order SQLite felt like, and the row numbers would swap
     * between reads of the same data.
     *
     * The floating list is a quick-paste surface, not the history: it reads a
     * bounded window rather than the whole table, because the whole table can
     * be the user's full limit and this runs on the main thread when the
     * bubble is tapped.
     */
    fun listClips(limit: Int, previewChars: Int? = null): List<Clip> {
        if (limit <= 0) return emptyList()
        return read(
            "SELECT id, title, ${contentColumns(previewChars)} FROM clips " +
                "ORDER BY created_at DESC, id DESC LIMIT ?;",
            arrayOf(limit.toString())
        )
    }

    /**
     * The content columns for a read: the whole text, or its first
     * [previewChars] characters and whether that was all of it.
     *
     * A list shows three lines of each clip, and a clip can be most of a
     * megabyte. Reading every one in full put the whole history in memory,
     * laid out and described to TalkBack, to show the top of each.
     */
    private fun contentColumns(previewChars: Int?): String =
        if (previewChars == null) "content, 1"
        else "substr(content, 1, $previewChars), length(content) <= $previewChars"

    /** One clip in full, or null if it has gone. */
    fun getClip(id: Long): Clip? =
        read("SELECT id, title, content, 1 FROM clips WHERE id = ?;", arrayOf(id.toString()))
            .firstOrNull()

    /** One clip with everything [restoreClip] needs, or null if it has gone. */
    fun getStoredClip(id: Long): StoredClip? = try {
        readableDatabase.rawQuery(
            "SELECT id, title, content, created_at, sort_order FROM clips WHERE id = ?;",
            arrayOf(id.toString())
        ).use { c ->
            if (!c.moveToFirst()) null
            else StoredClip(
                id = c.getLong(0),
                title = if (c.isNull(1)) null else c.getString(1),
                content = c.getString(2),
                createdAt = c.getLong(3),
                sortOrder = c.getInt(4)
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("DevClip", "Could not read a clip", e)
        null
    }

    /**
     * Puts a deleted clip back: same id, same timestamp, so it returns to the
     * same place in the list rather than arriving at the top as if captured
     * just now. OR IGNORE because an id that has somehow been taken since is
     * a clip that exists, and overwriting it would lose that one instead.
     */
    fun restoreClip(clip: StoredClip): Boolean = try {
        writableDatabase.execSQL(
            "INSERT OR IGNORE INTO clips (id, title, content, created_at, sort_order) " +
                "VALUES (?, ?, ?, ?, ?);",
            arrayOf<Any?>(clip.id, clip.title, clip.content, clip.createdAt, clip.sortOrder)
        )
        true
    } catch (e: Exception) {
        android.util.Log.e("DevClip", "Could not restore a clip", e)
        false
    }

    /**
     * The clips whose title or text contains [query], newest first.
     *
     * LIKE with the wildcards escaped. Without the escape a search for "50%"
     * or "snake_case" matches far more than it should, because % and _ mean
     * something to SQLite and nothing to the person typing them.
     *
     * The ESCAPE clause below carries ONE backslash. It sits in a raw string,
     * where a backslash is already literal, and SQLite rejects an escape that
     * is not a single character. The doubled backslashes in the replaces
     * above are a different thing entirely: those are ordinary string
     * literals, where two characters are needed to mean one backslash.
     */
    fun searchClips(query: String, previewChars: Int? = null): List<Clip> {
        if (query.isBlank()) return listClips(Int.MAX_VALUE, previewChars)
        val escaped = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        val pattern = "%$escaped%"
        return read(
            """
            SELECT id, title, ${contentColumns(previewChars)} FROM clips
            WHERE title LIKE ? ESCAPE '\' OR content LIKE ? ESCAPE '\'
            ORDER BY created_at DESC, id DESC;
            """.trimIndent(),
            arrayOf(pattern, pattern)
        )
    }

    fun updateClip(id: Long, content: String, title: String?) {
        try {
            writableDatabase.execSQL(
                "UPDATE clips SET content = ?, title = ? WHERE id = ?;",
                arrayOf<Any?>(content, title, id)
            )
        } catch (e: Exception) {
            android.util.Log.e("DevClip", "Could not update a clip", e)
        }
    }

    fun deleteClip(id: Long) {
        try {
            writableDatabase.execSQL("DELETE FROM clips WHERE id = ?;", arrayOf<Any>(id))
        } catch (e: Exception) {
            android.util.Log.e("DevClip", "Could not delete a clip", e)
        }
    }

    fun deleteAllClips() {
        try {
            writableDatabase.execSQL("DELETE FROM clips;")
        } catch (e: Exception) {
            android.util.Log.e("DevClip", "Could not clear the clip history", e)
        }
    }

    fun countClips(): Int = try {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM clips;", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    } catch (e: Exception) {
        0
    }

    /**
     * Inserts a clip from a backup, title and all.
     *
     * Separate from [insertClip], which exists for capture: that one refuses
     * a duplicate of the clip at the top, which is the double-tap guard, and
     * carries no title because a captured selection has none. An import has
     * already done its own de-duplication against the whole table.
     */
    fun insertImported(title: String?, content: String) {
        try {
            val db = writableDatabase
            val nextOrder = db.rawQuery("SELECT MAX(sort_order) FROM clips;", null)
                .use { if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) + 1 else 0 }
            db.execSQL(
                "INSERT INTO clips (title, content, created_at, sort_order) VALUES (?, ?, ?, ?);",
                arrayOf<Any?>(title, content, System.currentTimeMillis(), nextOrder)
            )
        } catch (e: Exception) {
            android.util.Log.e("DevClip", "Could not import a clip", e)
        }
    }

    /** Shared cursor walk for every read that returns clips. */
    private fun read(sql: String, args: Array<String>?): List<Clip> = try {
        readableDatabase.rawQuery(sql, args).use { cursor ->
            val out = ArrayList<Clip>(cursor.count)
            while (cursor.moveToNext()) {
                out.add(
                    Clip(
                        id = cursor.getLong(0),
                        title = if (cursor.isNull(1)) null else cursor.getString(1),
                        content = cursor.getString(2),
                        complete = cursor.getInt(3) != 0
                    )
                )
            }
            out
        }
    } catch (e: Exception) {
        android.util.Log.e("DevClip", "Could not read the clip history", e)
        emptyList()
    }

    /**
     * Keeps only the newest [max] clips. `max <= 0` means no limit.
     *
     * This has to run here as well as in the app. Capture happens with the app
     * closed — that is the whole point of it — so trimming only when the app
     * is open means the limit the user set is not a limit at all until they
     * next open DevClip.
     */
    fun trimToMax(max: Int) {
        if (max <= 0) return
        try {
            writableDatabase.execSQL(
                """
                DELETE FROM clips WHERE id NOT IN (
                    SELECT id FROM clips ORDER BY created_at DESC, id DESC LIMIT ?
                );
                """.trimIndent(),
                arrayOf<Any>(max)
            )
        } catch (e: Exception) {
            android.util.Log.w("DevClip", "Could not trim the clip history", e)
        }
    }
}
