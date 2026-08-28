import { Platform } from 'react-native';

/**
 * DevClip visual language — "Soft Structuralism"
 *
 * A near-white, silver-grey surface with airy floating cards, restrained
 * accent color, and diffused (never harsh) shadows in light mode; the same
 * structure inverted into a near-black, graphite surface for dark mode.
 */
export interface ThemeColors {
  bg: string;
  surface: string;
  surfaceSunken: string;
  border: string;
  borderStrong: string;
  ink: string;
  inkSoft: string;
  inkFaint: string;
  accent: string;
  accentSoft: string;
  accentPressed: string;
  danger: string;
  dangerSoft: string;
}

export const lightColors: ThemeColors = {
  bg: '#F4F4F6',
  surface: '#FFFFFF',
  surfaceSunken: '#ECECEF',
  border: 'rgba(17,17,20,0.06)',
  borderStrong: 'rgba(17,17,20,0.1)',
  ink: '#111114',
  inkSoft: '#5A5A63',
  inkFaint: '#9C9CA3',
  accent: '#3D4CF0',
  accentSoft: '#EAEBFD',
  accentPressed: '#2E3BD1',
  danger: '#E0453F',
  dangerSoft: '#FBEAE9',
};

export const darkColors: ThemeColors = {
  bg: '#0E0E11',
  surface: '#1A1A1F',
  surfaceSunken: '#232329',
  border: 'rgba(255,255,255,0.07)',
  borderStrong: 'rgba(255,255,255,0.12)',
  ink: '#F2F2F4',
  inkSoft: '#B4B4BC',
  inkFaint: '#75757E',
  accent: '#7C87F7',
  accentSoft: '#262A55',
  accentPressed: '#9AA3FA',
  danger: '#F17872',
  dangerSoft: '#3A2323',
};

export const spacing = {
  xs: 4,
  sm: 8,
  md: 14,
  lg: 20,
  xl: 28,
  xxl: 40,
};

export const radii = {
  sm: 10,
  md: 16,
  lg: 22,
  pill: 999,
};

export const type = {
  regular: 'Manrope_400Regular',
  medium: 'Manrope_500Medium',
  semibold: 'Manrope_600SemiBold',
  bold: 'Manrope_700Bold',
  extrabold: 'Manrope_800ExtraBold',
};

export function getShadow(mode: 'light' | 'dark') {
  const shadowColor = mode === 'dark' ? '#000000' : '#111114';
  return {
    card: Platform.select({
      ios: {
        shadowColor,
        shadowOpacity: mode === 'dark' ? 0.35 : 0.08,
        shadowRadius: 16,
        shadowOffset: { width: 0, height: 6 },
      },
      android: { elevation: 3 },
      default: {},
    }),
    floating: Platform.select({
      ios: {
        shadowColor,
        shadowOpacity: mode === 'dark' ? 0.5 : 0.16,
        shadowRadius: 24,
        shadowOffset: { width: 0, height: 10 },
      },
      android: { elevation: 8 },
      default: {},
    }),
  };
}
