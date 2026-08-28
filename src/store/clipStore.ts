import { create } from 'zustand';
import { Clip, SortMode } from '../types/clip';
import * as db from '../db/database';

interface ClipStoreState {
  clips: Clip[];
  search: string;
  sort: SortMode;
  loading: boolean;
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
}

export const useClipStore = create<ClipStoreState>((set, get) => ({
  clips: [],
  search: '',
  sort: 'date-desc',
  loading: false,

  init: async () => {
    await db.initDatabase();
    await get().refresh();
  },

  refresh: async () => {
    set({ loading: true });
    const clips = await db.getAllClips(get().sort, get().search);
    set({ clips, loading: false });
  },

  setSearch: (q: string) => {
    set({ search: q });
    get().refresh();
  },

  setSort: async (s: SortMode) => {
    const { sort: previousSort, clips } = get();
    if (s === 'manual' && previousSort !== 'manual') {
      // Snapshot the order the user is currently looking at (e.g. Newest
      // first) as the new manual order, instead of jumping back to
      // whatever order clips were originally inserted in.
      await db.snapshotOrder(clips.map((c) => c.id));
    }
    set({ sort: s });
    await get().refresh();
  },

  addClip: async (content: string, title: string | null = null) => {
    await db.addClip(content, title);
    await get().refresh();
  },

  updateClip: async (id: number, content: string, title: string | null) => {
    await db.updateClip(id, content, title);
    await get().refresh();
  },

  deleteClip: async (id: number) => {
    await db.deleteClip(id);
    await get().refresh();
  },

  clearAll: async () => {
    await db.deleteAllClips();
    await get().refresh();
  },

  trimToMax: async (max: number) => {
    await db.trimClipsToMax(max);
    await get().refresh();
  },

  // Manual reorder only makes sense while sort === 'manual'
  moveUp: async (index: number) => {
    const { clips, sort } = get();
    if (sort !== 'manual' || index <= 0) return;
    await db.swapClipOrder(clips[index], clips[index - 1]);
    await get().refresh();
  },

  moveDown: async (index: number) => {
    const { clips, sort } = get();
    if (sort !== 'manual' || index >= clips.length - 1) return;
    await db.swapClipOrder(clips[index], clips[index + 1]);
    await get().refresh();
  },
}));
