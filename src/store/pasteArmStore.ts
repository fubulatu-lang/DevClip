import { create } from 'zustand';

/** How long an armed row stays armed before it relaxes. */
const ARM_TIMEOUT_MS = 2000;

interface PasteArmState {
  /** The clip that is one tap away from pasting, or null. */
  armedId: number | null;
  arm: (id: number) => void;
  disarm: () => void;
}

let armTimer: ReturnType<typeof setTimeout> | null = null;

/**
 * Tap once to arm, tap again to paste.
 *
 * This replaces an `Alert.alert` confirmation, which was not merely cramped
 * in a 320dp floating window — it did not work there at all. An Android
 * system dialog needs a foreground Activity to attach to, and the floating
 * window has none by design, so with "Confirm before paste" turned on,
 * tapping a clip in the floating list did nothing whatsoever.
 *
 * One armed row at a time, held here rather than in each row, so that arming
 * one clip and then tapping another arms the second rather than pasting it.
 * That is the case the guard exists for: a mis-tap on the wrong row should
 * never paste the wrong text into somebody's message.
 *
 * It relaxes on a timer, because an armed row left armed indefinitely turns
 * the next tap on it — minutes later, in a different context — into a paste
 * the user did not mean.
 */
export const usePasteArmStore = create<PasteArmState>((set) => ({
  armedId: null,

  arm: (id) => {
    if (armTimer) clearTimeout(armTimer);
    set({ armedId: id });
    armTimer = setTimeout(() => {
      armTimer = null;
      set({ armedId: null });
    }, ARM_TIMEOUT_MS);
  },

  disarm: () => {
    if (armTimer) clearTimeout(armTimer);
    armTimer = null;
    set({ armedId: null });
  },
}));
