package com.devclip.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/**
 * Writes directly into the same SQLite file that the JS side opens via
 * expo-sqlite's `openDatabaseSync('devclip.db')`. expo-sqlite stores its
 * databases at: <filesDir>/SQLite/<name> — see
 * node_modules/expo-sqlite/android/.../SQLiteModule.kt (DATABASE_DIRECTORY).
 *
 * Keep DB_NAME and the `clips` table schema below in sync with
 * src/db/database.ts on the JS side. If you change one, change both.
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

    /** One row of the clip list, as the floating list needs it. */
    data class Clip(val id: Long, val title: String?, val content: String)

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
    fun listClips(limit: Int): List<Clip> {
        if (limit <= 0) return emptyList()
        return read(
            "SELECT id, title, content FROM clips ORDER BY created_at DESC, id DESC LIMIT ?;",
            arrayOf(limit.toString())
        )
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
    fun searchClips(query: String): List<Clip> {
        if (query.isBlank()) return listClips(Int.MAX_VALUE)
        val escaped = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        val pattern = "%$escaped%"
        return read(
            """
            SELECT id, title, content FROM clips
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

    /** Shared cursor walk for every read that returns clips. */
    private fun read(sql: String, args: Array<String>?): List<Clip> = try {
        readableDatabase.rawQuery(sql, args).use { cursor ->
            val out = ArrayList<Clip>(cursor.count)
            while (cursor.moveToNext()) {
                out.add(
                    Clip(
                        id = cursor.getLong(0),
                        title = if (cursor.isNull(1)) null else cursor.getString(1),
                        content = cursor.getString(2)
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
