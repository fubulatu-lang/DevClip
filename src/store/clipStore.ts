import { create } from 'zustand';
import { Clip, SortMode } from '../types/clip';
import * as db from '../db/database';

interface ClipStoreState {
  clips: Clip[];
  search: string;
  sort: SortMode;
  loading: boolean;
  /** Human-readable message for the last failed operation, or null. Cleared on the next successful action. */
  error: string | null;
  init: () => Promise<void>;
  refresh: () => Promise<void>;
  setSearch: (q: string) => void;
  setSort: (s: SortMode) => Promise<void>;
  addClip: (content: string, title?: string | null) => Promise<void>;
  updateClip: (id: number, content: string, title: string | null) => Promise<void>;
  deleteClip: (id: number) => Promise<void>;
  clearAll: () => Promise<void>;
  trimToMax: (max: number) => Promise<void>;
  moveUp: (index: number) => Promise<void>;
  moveDown: (index: number) => Promise<void>;
  dismissError: () => void;
}

// Every db.* call below used to run unguarded — a failed write (e.g. the
// SQLite file briefly locked by the native capture service) would throw an
// unhandled rejection and the UI would just silently not update, with the
// user never told anything went wrong. Each action now reports failures via
// `error` instead of failing invisibly.
async function runOrReport(set: (partial: Partial<ClipStoreState>) => void, fallbackMessage: string, action: () => Promise<void>) {
  try {
    await action();
    set({ error: null });
  } catch (e) {
    set({ error: fallbackMessage, loading: false });
  }
}

export const useClipStore = create<ClipStoreState>((set, get) => ({
  clips: [],
  search: '',
  sort: 'date-desc',
  loading: false,
  error: null,

  init: async () => {
    await runOrReport(set, 'Could not open your clip history.', async () => {
      await db.initDatabase();
      await get().refresh();
    });
  },

  refresh: async () => {
    set({ loading: true });
    await runOrReport(set, 'Could not load your clips.', async () => {
      const clips = await db.getAllClips(get().sort, get().search);
      set({ clips, loading: false });
    });
  },

  setSearch: (q: string) => {
    set({ search: q });
    get().refresh();
  },

  setSort: async (s: SortMode) => {
    await runOrReport(set, 'Could not change sort order.', async () => {
      const { sort: previousSort, clips } = get();
      if (s === 'manual' && previousSort !== 'manual') {
        // Snapshot the order the user is currently looking at (e.g. Newest
        // first) as the new manual order, instead of jumping back to
        // whatever order clips were originally inserted in.
        await db.snapshotOrder(clips.map((c) => c.id));
      }
      set({ sort: s });
      await get().refresh();
    });
  },

  addClip: async (content: string, title: string | null = null) => {
    await runOrReport(set, 'Could not save that clip. Try capturing again.', async () => {
      await db.addClip(content, title);
      await get().refresh();
    });
  },

  updateClip: async (id: number, content: string, title: string | null) => {
    await runOrReport(set, 'Could not save your changes.', async () => {
      await db.updateClip(id, content, title);
      await get().refresh();
    });
  },

  deleteClip: async (id: number) => {
    await runOrReport(set, 'Could not delete that clip.', async () => {
      await db.deleteClip(id);
      await get().refresh();
    });
  },

  clearAll: async () => {
    await runOrReport(set, 'Could not clear your clips.', async () => {
      await db.deleteAllClips();
      await get().refresh();
    });
  },

  trimToMax: async (max: number) => {
    await runOrReport(set, 'Could not trim your clip history.', async () => {
      await db.trimClipsToMax(max);
      await get().refresh();
    });
  },

  // Manual reorder only makes sense while sort === 'manual'
  moveUp: async (index: number) => {
    const { clips, sort } = get();
    if (sort !== 'manual' || index <= 0) return;
    await runOrReport(set, 'Could not reorder that clip.', async () => {
      await db.swapClipOrder(clips[index], clips[index - 1]);
      await get().refresh();
    });
  },

  moveDown: async (index: number) => {
    const { clips, sort } = get();
    if (sort !== 'manual' || index >= clips.length - 1) return;
    await runOrReport(set, 'Could not reorder that clip.', async () => {
      await db.swapClipOrder(clips[index], clips[index + 1]);
      await get().refresh();
    });
  },

  dismissError: () => set({ error: null }),
}));
