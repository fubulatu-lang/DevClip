import { create } from 'zustand';
import { Clip, SortMode } from '../types/clip';
import * as db from '../db/database';
import { readSystemClipboard } from '../utils/clipboardCapture';

interface ClipStoreState {
  clips: Clip[];
  search: string;
  sort: SortMode;
  /**
   * True only while a capture is in flight — the Capture button's own busy
   * state, and nothing else's.
   */
  capturing: boolean;
  /**
   * False until the first load has finished, however it finished.
   *
   * Without it an empty `clips` means two different things — "nothing saved
   * yet" and "not read yet" — and the list shows "No clips yet" during the
   * moment before SQLite answers. In the floating window that moment is the
   * whole first impression, and if `init` throws it never ends: the empty
   * state would sit there permanently claiming the history is empty when in
   * fact it was never opened.
   */
  initialised: boolean;
  /** Human-readable message for the last failed operation, or null. Cleared on the next successful action. */
  error: string | null;
  init: () => Promise<void>;
  refresh: () => Promise<void>;
  setSearch: (q: string) => void;
  setSort: (s: SortMode) => Promise<void>;
  capture: () => Promise<void>;
  updateClip: (id: number, content: string, title: string | null) => Promise<void>;
  deleteClip: (id: number) => Promise<void>;
  clearAll: () => Promise<void>;
  trimToMax: (max: number) => Promise<void>;
  moveUp: (index: number) => Promise<void>;
  moveDown: (index: number) => Promise<void>;
  dismissError: () => void;
}

/**
 * Every refresh is a race. Two in flight at once — two keystrokes, or a
 * keystroke and a delete — resolve in whatever order SQLite finishes them,
 * and the loser overwrites the winner. Each refresh takes a ticket, and only
 * the newest ticket is allowed to publish its result.
 */
let refreshToken = 0;

/**
 * The field updates on every keystroke; the query does not. Typing "invoice"
 * used to run seven queries against SQLite, each of which could land out of
 * order on top of the last.
 */
let searchTimer: ReturnType<typeof setTimeout> | null = null;
const SEARCH_DEBOUNCE_MS = 200;

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
    set({ error: fallbackMessage });
  }
}

export const useClipStore = create<ClipStoreState>((set, get) => ({
  clips: [],
  search: '',
  sort: 'date-desc',
  capturing: false,
  initialised: false,
  error: null,

  init: async () => {
    try {
      await runOrReport(set, 'Could not open your clip history.', async () => {
        await db.initDatabase();
        await get().refresh();
      });
    } finally {
      // Set even on failure: the load is over either way, and the error banner
      // is the honest thing to show, not a spinner that never stops.
      set({ initialised: true });
    }
  },

  refresh: async () => {
    const token = ++refreshToken;
    await runOrReport(set, 'Could not load your clips.', async () => {
      const clips = await db.getAllClips(get().sort, get().search);
      // A newer refresh started while this query was running. Its results are
      // the ones the user is waiting for; these are already stale, and
      // publishing them would show results for a query that has moved on.
      if (token !== refreshToken) return;
      set({ clips });
    });
  },

  setSearch: (q: string) => {
    // Set the text immediately so typing never lags, and query once the user
    // pauses.
    set({ search: q });
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      searchTimer = null;
      get().refresh();
    }, SEARCH_DEBOUNCE_MS);
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

  /**
   * Read the system clipboard and save whatever is on it.
   *
   * Both screens used to carry their own copy of this, and drove the button's
   * busy state from `loading` — which every refresh set. Deleting a clip or
   * reordering one therefore flashed "Capturing…" on a button that was doing
   * nothing of the sort, and disabled it mid-gesture. `capturing` is set here
   * and nowhere else, and covers the clipboard read as well as the write,
   * since the read is the half that can actually make the user wait.
   */
  capture: async () => {
    // A second tap while the first is still reading would capture twice.
    if (get().capturing) return;
    set({ capturing: true });
    try {
      await runOrReport(set, 'Could not save that clip. Try capturing again.', async () => {
        const text = await readSystemClipboard();
        if (!text) return;
        await db.addClip(text, null);
        await get().refresh();
      });
    } finally {
      set({ capturing: false });
    }
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
