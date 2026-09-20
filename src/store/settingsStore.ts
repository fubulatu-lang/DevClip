import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  setAutoStartOnBoot as nativeSetAutoStartOnBoot,
  setBubbleAlpha as nativeSetBubbleAlpha,
  setBubbleIdleFade as nativeSetBubbleIdleFade,
  setBubbleSize as nativeSetBubbleSize,
  setConfirmBeforePaste as nativeSetConfirmBeforePaste,
  setMaxClips as nativeSetMaxClips,
  setPopupAlpha as nativeSetPopupAlpha,
  setTuckDelay as nativeSetTuckDelay,
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

/**
 * How opaque the bubble and the floating list are, as percentages.
 *
 * The floor is not zero. A window at zero opacity is invisible but still takes
 * touches, which from the outside is indistinguishable from a phone that has
 * started ignoring part of its own screen — so the slider stops well before
 * the point where the bubble could be lost entirely.
 *
 * Mirrored in Prefs on the native side, which clamps to the same floor because
 * it reads the stored value directly at startup.
 */
export const MIN_ALPHA = 20;
export const MAX_ALPHA = 100;
export const DEFAULT_ALPHA = 100;

/**
 * Seconds of stillness before the bubble tucks itself into the screen edge.
 *
 * Zero means never, and is the default: a bubble that disappears on its own is
 * a surprise the first time it happens, so it should be something the user
 * turned on rather than something they have to discover and switch off.
 */
export const MIN_TUCK_DELAY = 0;
export const MAX_TUCK_DELAY = 120;
export const DEFAULT_TUCK_DELAY = 0;

/**
 * The permission picture at the moment the user last walked past setup.
 *
 * Kept so the wall can tell "they already decided to live without this" from
 * "something they had has since been taken away" — Android revokes
 * permissions on its own for apps that have not been opened in months, and
 * says nothing about it.
 */
export interface SkippedPermissions {
  overlay: boolean;
  accessibility: boolean;
  notifications: boolean;
}

interface SettingsState {
  hasOnboarded: boolean;
  permissionSkip: SkippedPermissions | null;
  themeMode: ThemeMode;
  bubbleSize: number;
  autoStartOnBoot: boolean;
  confirmBeforePaste: boolean;
  maxClips: number; // 0 = unlimited
  bubbleAlpha: number;
  /** Whether bubbleAlpha applies only while the bubble is idle. */
  bubbleIdleFade: boolean;
  popupAlpha: number;
  /** Seconds before the bubble becomes an edge handle. 0 = never. */
  tuckDelay: number;

  /** Records what was granted when the user chose to carry on without the rest. */
  skipPermissions: (state: SkippedPermissions) => void;
  /** Puts the setup wall back up, from the banner in the app. */
  clearPermissionSkip: () => void;
  setThemeMode: (mode: ThemeMode) => void;
  setBubbleSize: (sizeDp: number) => void;
  setAutoStartOnBoot: (enabled: boolean) => void;
  setConfirmBeforePaste: (enabled: boolean) => void;
  setMaxClips: (max: number) => void;
  setBubbleAlpha: (alpha: number) => void;
  setBubbleIdleFade: (enabled: boolean) => void;
  setPopupAlpha: (alpha: number) => void;
  setTuckDelay: (seconds: number) => void;
}

/** Shared clamp for both transparency sliders. */
const clampAlpha = (value: number): number =>
  Math.round(Math.min(MAX_ALPHA, Math.max(MIN_ALPHA, value)));

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      hasOnboarded: false,
      permissionSkip: null,
      themeMode: 'system',
      bubbleSize: DEFAULT_BUBBLE_SIZE,
      autoStartOnBoot: true,
      confirmBeforePaste: true,
      maxClips: 500,
      bubbleAlpha: DEFAULT_ALPHA,
      bubbleIdleFade: false,
      popupAlpha: DEFAULT_ALPHA,
      tuckDelay: DEFAULT_TUCK_DELAY,

      skipPermissions: (state) => set({ hasOnboarded: true, permissionSkip: state }),
      clearPermissionSkip: () => set({ permissionSkip: null }),
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
      // Mirrored now, unlike before. The floating list is drawn natively and
      // opens with no React context behind it, so the setting that decides
      // whether a tap pastes immediately has to be where native can read it.
      setConfirmBeforePaste: (enabled) => {
        set({ confirmBeforePaste: enabled });
        nativeSetConfirmBeforePaste(enabled);
      },
      setMaxClips: (max) => {
        set({ maxClips: max });
        nativeSetMaxClips(max);
      },
      setBubbleAlpha: (alpha) => {
        const clamped = clampAlpha(alpha);
        set({ bubbleAlpha: clamped });
        nativeSetBubbleAlpha(clamped);
      },
      setBubbleIdleFade: (enabled) => {
        set({ bubbleIdleFade: enabled });
        nativeSetBubbleIdleFade(enabled);
      },
      setPopupAlpha: (alpha) => {
        const clamped = clampAlpha(alpha);
        set({ popupAlpha: clamped });
        nativeSetPopupAlpha(clamped);
      },
      setTuckDelay: (seconds) => {
        const clamped = Math.round(
          Math.min(MAX_TUCK_DELAY, Math.max(MIN_TUCK_DELAY, seconds))
        );
        set({ tuckDelay: clamped });
        nativeSetTuckDelay(clamped);
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
        nativeSetConfirmBeforePaste(state.confirmBeforePaste);
        nativeSetBubbleAlpha(state.bubbleAlpha);
        nativeSetBubbleIdleFade(state.bubbleIdleFade);
        nativeSetPopupAlpha(state.popupAlpha);
        nativeSetTuckDelay(state.tuckDelay);
      },
    }
  )
);
