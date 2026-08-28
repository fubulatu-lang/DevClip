import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { setAutoStartOnBoot as nativeSetAutoStartOnBoot, setBubbleSize as nativeSetBubbleSize } from '../native/OverlayModule';

export type ThemeMode = 'light' | 'dark' | 'system';
export type BubbleSize = 'small' | 'medium' | 'large';

interface SettingsState {
  hasOnboarded: boolean;
  themeMode: ThemeMode;
  bubbleSize: BubbleSize;
  autoStartOnBoot: boolean;
  confirmBeforePaste: boolean;
  maxClips: number; // 0 = unlimited

  setOnboarded: () => void;
  setThemeMode: (mode: ThemeMode) => void;
  setBubbleSize: (size: BubbleSize) => void;
  setAutoStartOnBoot: (enabled: boolean) => void;
  setConfirmBeforePaste: (enabled: boolean) => void;
  setMaxClips: (max: number) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      hasOnboarded: false,
      themeMode: 'system',
      bubbleSize: 'medium',
      autoStartOnBoot: true,
      confirmBeforePaste: true,
      maxClips: 500,

      setOnboarded: () => set({ hasOnboarded: true }),
      setThemeMode: (mode) => set({ themeMode: mode }),
      setBubbleSize: (size) => {
        set({ bubbleSize: size });
        nativeSetBubbleSize(size);
      },
      setAutoStartOnBoot: (enabled) => {
        set({ autoStartOnBoot: enabled });
        nativeSetAutoStartOnBoot(enabled);
      },
      setConfirmBeforePaste: (enabled) => set({ confirmBeforePaste: enabled }),
      setMaxClips: (max) => set({ maxClips: max }),
    }),
    {
      name: 'devclip-settings',
      storage: createJSONStorage(() => AsyncStorage),
    }
  )
);
