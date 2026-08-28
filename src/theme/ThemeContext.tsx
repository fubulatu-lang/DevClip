import React, { createContext, useContext, useMemo } from 'react';
import { useColorScheme } from 'react-native';
import { lightColors, darkColors, getShadow, spacing, radii, type, ThemeColors } from './theme';
import { useSettingsStore } from '../store/settingsStore';

interface ThemeValue {
  mode: 'light' | 'dark';
  colors: ThemeColors;
  shadow: ReturnType<typeof getShadow>;
  spacing: typeof spacing;
  radii: typeof radii;
  type: typeof type;
}

const ThemeContext = createContext<ThemeValue | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const themeMode = useSettingsStore((s) => s.themeMode);
  const systemScheme = useColorScheme();

  const mode: 'light' | 'dark' =
    themeMode === 'system' ? (systemScheme === 'dark' ? 'dark' : 'light') : themeMode;

  const value = useMemo<ThemeValue>(
    () => ({
      mode,
      colors: mode === 'dark' ? darkColors : lightColors,
      shadow: getShadow(mode),
      spacing,
      radii,
      type,
    }),
    [mode]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider');
  return ctx;
}
