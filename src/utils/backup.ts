import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import * as db from '../db/database';

/**
 * Exports every clip as a JSON file and hands it to the OS share sheet, so
 * the user picks where it goes (Drive, Files, email, etc.) per export,
 * rather than DevClip holding a persisted folder permission. Live capture
 * keeps writing to the fixed internal database regardless — this is a
 * point-in-time snapshot, not a sync destination.
 */
export async function exportBackup(): Promise<void> {
  const clips = await db.getAllClips('');

  const payload = {
    exportedAt: new Date().toISOString(),
    app: 'DevClip',
    version: 1,
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
