import * as SQLite from 'expo-sqlite';
import { Clip } from '../types/clip';

// IMPORTANT: This filename/location must exactly match the path the native
// ClipboardAccessibilityService.kt writes to, so both the JS app and the
// background service read/write the SAME database file. See
// plugins/android-src/DevClipDatabaseHelper.kt for the native side.
const DB_NAME = 'devclip.db';

let dbInstance: SQLite.SQLiteDatabase | null = null;

function getDb(): SQLite.SQLiteDatabase {
  if (!dbInstance) {
    dbInstance = SQLite.openDatabaseSync(DB_NAME);
  }
  return dbInstance;
}

export async function initDatabase(): Promise<void> {
  const db = getDb();
  await db.execAsync(`
    PRAGMA journal_mode = WAL;
    CREATE TABLE IF NOT EXISTS clips (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT,
      content TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      sort_order INTEGER NOT NULL
    );
  `);
}

function rowToClip(row: any): Clip {
  return {
    id: row.id,
    title: row.title,
    content: row.content,
    createdAt: row.created_at,
    sortOrder: row.sort_order,
  };
}

/**
 * Newest first, always.
 *
 * There is one order in DevClip now. Rows are numbered by position in the
 * list, and a number that means something different depending on a sort menu
 * is worse than no number at all. Finding a specific clip is what search is
 * for, and search is in the full app where there is room for it.
 *
 * `id DESC` breaks the tie: two clips captured inside the same millisecond
 * would otherwise come back in whatever order SQLite felt like, and the row
 * numbers would swap between refreshes.
 */
export async function getAllClips(search: string): Promise<Clip[]> {
  const db = getDb();
  const orderBy = 'created_at DESC, id DESC';

  const query = search
    ? `SELECT * FROM clips WHERE title LIKE ? OR content LIKE ? ORDER BY ${orderBy};`
    : `SELECT * FROM clips ORDER BY ${orderBy};`;
  const params = search ? [`%${search}%`, `%${search}%`] : [];

  const rows = await db.getAllAsync(query, params);
  return rows.map(rowToClip);
}

export async function addClip(content: string, title: string | null = null): Promise<Clip> {
  const db = getDb();
  const maxRow = await db.getFirstAsync<{ maxOrder: number | null }>(
    'SELECT MAX(sort_order) as maxOrder FROM clips;'
  );
  const nextOrder = (maxRow?.maxOrder ?? -1) + 1;
  const createdAt = Date.now();

  const result = await db.runAsync(
    'INSERT INTO clips (title, content, created_at, sort_order) VALUES (?, ?, ?, ?);',
    [title, content, createdAt, nextOrder]
  );

  return {
    id: result.lastInsertRowId,
    title,
    content,
    createdAt,
    sortOrder: nextOrder,
  };
}

export async function updateClip(id: number, content: string, title: string | null): Promise<void> {
  const db = getDb();
  await db.runAsync('UPDATE clips SET content = ?, title = ? WHERE id = ?;', [content, title, id]);
}

export async function deleteClip(id: number): Promise<void> {
  const db = getDb();
  await db.runAsync('DELETE FROM clips WHERE id = ?;', [id]);
}

export async function deleteAllClips(): Promise<void> {
  const db = getDb();
  await db.execAsync('DELETE FROM clips;');
}

/**
 * Merges imported clips in, skipping any whose text is already stored.
 *
 * One transaction for the whole file. A backup can be thousands of rows, and
 * a partially-applied import — the app killed halfway through — would leave a
 * history that is neither what it was nor what was asked for.
 *
 * The existing text is read once into a Set rather than queried per row: a
 * thousand-row import would otherwise be a thousand SELECTs, and the check is
 * exact equality either way.
 */
export async function importClips(
  clips: { title: string | null; content: string; createdAt: number }[]
): Promise<{ added: number; skipped: number }> {
  const db = getDb();
  const existingRows = await db.getAllAsync<{ content: string }>('SELECT content FROM clips;');
  const existing = new Set(existingRows.map((r) => r.content));

  const maxRow = await db.getFirstAsync<{ maxOrder: number | null }>(
    'SELECT MAX(sort_order) as maxOrder FROM clips;'
  );
  let nextOrder = (maxRow?.maxOrder ?? -1) + 1;

  let added = 0;
  let skipped = 0;

  await db.execAsync('BEGIN TRANSACTION;');
  try {
    for (const clip of clips) {
      // Guards against duplicates already in the database and duplicates
      // within the file itself, which a hand-merged backup can carry.
      if (existing.has(clip.content)) {
        skipped++;
        continue;
      }
      existing.add(clip.content);
      await db.runAsync(
        'INSERT INTO clips (title, content, created_at, sort_order) VALUES (?, ?, ?, ?);',
        [clip.title, clip.content, clip.createdAt, nextOrder++]
      );
      added++;
    }
    await db.execAsync('COMMIT;');
  } catch (e) {
    await db.execAsync('ROLLBACK;');
    throw e;
  }

  return { added, skipped };
}

// Keeps only the most recent `max` clips (by created_at). max <= 0 means unlimited.
export async function trimClipsToMax(max: number): Promise<void> {
  if (max <= 0) return;
  const db = getDb();
  await db.runAsync(
    `DELETE FROM clips WHERE id NOT IN (
       SELECT id FROM clips ORDER BY created_at DESC LIMIT ?
     );`,
    [max]
  );
}

