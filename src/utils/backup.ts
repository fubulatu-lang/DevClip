import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import * as db from '../db/database';

/** Bumped only when the shape below changes in a way an old reader would misread. */
const BACKUP_VERSION = 1;

interface BackupClip {
  title: string | null;
  content: string;
  createdAt: number;
}

/**
 * Exports every clip as a JSON file and hands it to the OS share sheet, so
 * the user picks where it goes (Drive, Files, email, etc.) per export,
 * rather than DevClip holding a persisted folder permission. Live capture
 * keeps writing to the fixed internal database regardless — this is a
 * point-in-time snapshot, not a sync destination.
 *
 * Timestamps go in the file. Without them a restored history arrives in one
 * undifferentiated block dated to the moment of import, and since the whole
 * app is ordered newest-first, that would mean the order the user built up
 * over months did not survive the round trip.
 */
export async function exportBackup(): Promise<void> {
  const clips = await db.getAllClips('');

  const payload = {
    exportedAt: new Date().toISOString(),
    app: 'DevClip',
    version: BACKUP_VERSION,
    clips: clips.map((c) => ({
      title: c.title,
      content: c.content,
      createdAt: c.createdAt,
    })),
  };

  const filename = `devclip-backup-${Date.now()}.json`;
  const file = new File(Paths.cache, filename);
  if (file.exists) file.delete();
  file.create();
  file.write(JSON.stringify(payload, null, 2));

  const canShare = await Sharing.isAvailableAsync();
  if (!canShare) {
    throw new Error('Sharing is not available on this device.');
  }
  await Sharing.shareAsync(file.uri, {
    mimeType: 'application/json',
    dialogTitle: 'Export DevClip backup',
  });
}

export interface ImportResult {
  added: number;
  /** Already present, by exact text. */
  skipped: number;
  /** The user closed the picker. Not a failure, and not worth a message. */
  cancelled: boolean;
}

/**
 * Imports a backup file, merging rather than replacing.
 *
 * Merge, and skip anything whose text is already there, so importing the same
 * file twice does nothing the second time. Replacing would make an import a
 * destructive act that looks like a safe one — a user restoring an old backup
 * onto a phone with newer clips on it would silently lose the newer clips,
 * and there is no undo.
 *
 * The dedupe is on content alone. Two clips with the same text and different
 * timestamps are the same clip as far as anyone reading the list is
 * concerned, and keeping both would mean a re-import quietly doubling the
 * history.
 */
export async function importBackup(): Promise<ImportResult> {
  const picked = await File.pickFileAsync({ mimeTypes: 'application/json' });
  if (picked.canceled) return { added: 0, skipped: 0, cancelled: true };

  const raw = await picked.result.text();
  const clips = parseBackup(raw);

  const result = await db.importClips(clips);
  return { ...result, cancelled: false };
}

/**
 * Reads a backup file into clips, and refuses anything it cannot vouch for.
 *
 * A file picker hands back whatever the user chose, which will sometimes not
 * be a DevClip backup at all. Failing loudly here is the whole point: the
 * alternative is importing a hundred rows of `undefined` into a history that
 * has no undo.
 */
function parseBackup(raw: string): BackupClip[] {
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (e) {
    throw new Error('That file is not a DevClip backup.');
  }

  const clips = (parsed as { clips?: unknown })?.clips;
  if (!Array.isArray(clips)) {
    throw new Error('That file is not a DevClip backup.');
  }

  const now = Date.now();
  const valid: BackupClip[] = [];
  for (const entry of clips) {
    const content = (entry as { content?: unknown })?.content;
    if (typeof content !== 'string' || content.length === 0) continue;

    const title = (entry as { title?: unknown })?.title;
    const createdAt = (entry as { createdAt?: unknown })?.createdAt;
    valid.push({
      content,
      title: typeof title === 'string' && title.length > 0 ? title : null,
      // A backup written before timestamps existed, or a hand-edited file.
      // Landing it at "now" is the honest fallback: it says the app does not
      // know when this was captured, rather than inventing a plausible past.
      createdAt: typeof createdAt === 'number' && Number.isFinite(createdAt) ? createdAt : now,
    });
  }

  if (valid.length === 0) {
    throw new Error('That file has no clips in it.');
  }
  return valid;
}
