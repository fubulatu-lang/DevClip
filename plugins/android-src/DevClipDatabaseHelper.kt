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
        // No schema changes yet.
    }

    /** Inserts a newly-copied clip, skipping exact duplicates of the most recent entry. */
    fun insertClip(content: String) {
        val db = writableDatabase
        val last = db.rawQuery("SELECT content FROM clips ORDER BY created_at DESC LIMIT 1;", null)
        val isDuplicate = last.use {
            it.moveToFirst() && it.getString(0) == content
        }
        if (isDuplicate) return

        val maxOrderCursor = db.rawQuery("SELECT MAX(sort_order) FROM clips;", null)
        val nextOrder = maxOrderCursor.use {
            if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) + 1 else 0
        }

        db.execSQL(
            "INSERT INTO clips (title, content, created_at, sort_order) VALUES (NULL, ?, ?, ?);",
            arrayOf(content, System.currentTimeMillis(), nextOrder)
        )
    }
}
