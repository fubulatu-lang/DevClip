import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  setAutoStartOnBoot as nativeSetAutoStartOnBoot,
  setBubbleSize as nativeSetBubbleSize,
  setMaxClips as nativeSetMaxClips,
} from '../native/OverlayModule';

export type ThemeMode = 'light' | 'dark' | 'system';

/**
 * Bubble diameter in dp.
 *
 * The floor is Android's comfortable touch target. Below 48dp a bubble gets
 * missed, and it gets missed most when it is sitting over a keyboard — which
 * is exactly where it needs to be hit. The ceiling is 1.5x that; past it the
 * bubble stops being a bubble and starts being something in the way.
 *
 * Both are mirrored in Prefs on the native side, which has to hold the same
 * limits because it reads the stored value directly at startup.
 */
export const MIN_BUBBLE_SIZE = 48;
export const MAX_BUBBLE_SIZE = 72;
export const DEFAULT_BUBBLE_SIZE = 56;

interface SettingsState {
  hasOnboarded: boolean;
  themeMode: ThemeMode;
  bubbleSize: number;
  autoStartOnBoot: boolean;
  confirmBeforePaste: boolean;
  maxClips: number; // 0 = unlimited

  setOnboarded: () => void;
  setThemeMode: (mode: ThemeMode) => void;
  setBubbleSize: (sizeDp: number) => void;
  setAutoStartOnBoot: (enabled: boolean) => void;
  setConfirmBeforePaste: (enabled: boolean) => void;
  setMaxClips: (max: number) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      hasOnboarded: false,
      themeMode: 'system',
      bubbleSize: DEFAULT_BUBBLE_SIZE,
      autoStartOnBoot: true,
      confirmBeforePaste: true,
      maxClips: 500,

      setOnboarded: () => set({ hasOnboarded: true }),
      setThemeMode: (mode) => set({ themeMode: mode }),
      setBubbleSize: (sizeDp) => {
        const clamped = Math.round(
          Math.min(MAX_BUBBLE_SIZE, Math.max(MIN_BUBBLE_SIZE, sizeDp))
        );
        set({ bubbleSize: clamped });
        nativeSetBubbleSize(clamped);
      },
      setAutoStartOnBoot: (enabled) => {
        set({ autoStartOnBoot: enabled });
        nativeSetAutoStartOnBoot(enabled);
      },
      setConfirmBeforePaste: (enabled) => set({ confirmBeforePaste: enabled }),
      setMaxClips: (max) => {
        set({ maxClips: max });
        nativeSetMaxClips(max);
      },
    }),
    {
      name: 'devclip-settings',
      storage: createJSONStorage(() => AsyncStorage),
      version: 2,
      /**
       * Bubble size was three names; it is a number of dp now. Without this
       * an existing install rehydrates the string 'medium' into a field the
       * slider reads as a number, and the slider renders at NaN.
       */
      migrate: (persisted, version) => {
        const state = persisted as Partial<SettingsState> & { bubbleSize?: unknown };
        if (version < 2) {
          const legacy: Record<string, number> = {
            small: MIN_BUBBLE_SIZE,
            medium: DEFAULT_BUBBLE_SIZE,
            large: MAX_BUBBLE_SIZE,
          };
          const stored = state?.bubbleSize;
          state.bubbleSize =
            typeof stored === 'string' ? (legacy[stored] ?? DEFAULT_BUBBLE_SIZE) : DEFAULT_BUBBLE_SIZE;
        }
        return state as SettingsState;
      },
      /**
       * Native components read these from SharedPreferences and can run with
       * no JS at all — the service started by BootReceiver, capture with the
       * app closed. The setters below push each change across, but a value
       * that was never changed on this install had never been pushed at all,
       * so native fell back to its own defaults and quietly disagreed with
       * what Settings was showing. Pushing the whole set once on rehydrate
       * makes the two sides agree from the first launch.
       */
      onRehydrateStorage: () => (state) => {
        if (!state) return;
        nativeSetBubbleSize(state.bubbleSize);
        nativeSetAutoStartOnBoot(state.autoStartOnBoot);
        nativeSetMaxClips(state.maxClips);
      },
    }
  )
);
