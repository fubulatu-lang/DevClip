import * as SQLite from 'expo-sqlite';
import { Clip, SortMode } from '../types/clip';

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

export async function getAllClips(sort: SortMode, search: string): Promise<Clip[]> {
  const db = getDb();
  let orderBy = 'sort_order ASC';
  if (sort === 'title-asc') orderBy = 'title COLLATE NOCASE ASC';
  if (sort === 'title-desc') orderBy = 'title COLLATE NOCASE DESC';
  if (sort === 'date-asc') orderBy = 'created_at ASC';
  if (sort === 'date-desc') orderBy = 'created_at DESC';

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

// Swaps sort_order between two clips (used for up/down manual reordering).
export async function swapClipOrder(a: Clip, b: Clip): Promise<void> {
  const db = getDb();
  await db.execAsync('BEGIN TRANSACTION;');
  try {
    await db.runAsync('UPDATE clips SET sort_order = ? WHERE id = ?;', [b.sortOrder, a.id]);
    await db.runAsync('UPDATE clips SET sort_order = ? WHERE id = ?;', [a.sortOrder, b.id]);
    await db.execAsync('COMMIT;');
  } catch (e) {
    await db.execAsync('ROLLBACK;');
    throw e;
  }
}

// Rewrites sort_order to match the given id order exactly. Used when the
// user switches to Manual sort, so manual reordering starts from whatever
// order they were just looking at (e.g. Newest first) instead of jumping
// back to original insertion order.
export async function snapshotOrder(orderedIds: number[]): Promise<void> {
  const db = getDb();
  await db.execAsync('BEGIN TRANSACTION;');
  try {
    for (let i = 0; i < orderedIds.length; i++) {
      await db.runAsync('UPDATE clips SET sort_order = ? WHERE id = ?;', [i, orderedIds[i]]);
    }
    await db.execAsync('COMMIT;');
  } catch (e) {
    await db.execAsync('ROLLBACK;');
    throw e;
  }
}
