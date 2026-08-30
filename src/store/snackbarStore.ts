import { create } from 'zustand';

interface SnackbarState {
  message: string | null;
  show: (message: string) => void;
  dismiss: () => void;
}

/**
 * One transient message at a time. A newer message replaces an older one
 * rather than queueing: this is feedback about what just happened, and a
 * backlog of stale notices is worse than the latest truth.
 */
export const useSnackbarStore = create<SnackbarState>((set) => ({
  message: null,
  show: (message) => set({ message }),
  dismiss: () => set({ message: null }),
}));
