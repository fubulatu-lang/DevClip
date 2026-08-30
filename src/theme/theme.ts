import { Platform } from 'react-native';

/**
 * DevClip visual language — Samsung One UI, in the app icon's colours
 *
 * Colour is role-based, not palette-based. Depth comes from rounded
 * containers and a background tone shift, never from large blurred shadows.
 *
 * The hues come from the app icon: its slate #345065 and its blue #3498DB.
 * Both are used darkened — the icon blue is only 3.15:1 under white text and
 * 3.02:1 as text, so taken literally it would fail WCAG AA everywhere it
 * matters. Every pair below is measured.
 * Type is deliberately large: 17sp body is the One UI default, not 13sp.
 *
 * Token values trace to the One UI design guidelines and the
 * tribalfs/oneui-design library resources. Values marked (derived) are
 * reasonable adaptations where no published constant exists, or where the
 * published value would fail WCAG 2.1 AA.
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
  /** Disabled text and truly decorative marks only — below AA by design. */
  inkDisabled: string;
  accent: string;
  accentSoft: string;
  accentPressed: string;
  /** Text/icon colour on top of an accent fill. */
  onAccent: string;
  danger: string;
  dangerSoft: string;
  success: string;
  successSoft: string;
  /** One UI functional orange — caution, not decoration. */
  warning: string;
  warningSoft: string;
  divider: string;
  /** One UI scrim: #33000000. */
  scrim: string;
}

export const lightColors: ThemeColors = {
  bg: '#F4F7F9',
  surface: '#FFFFFF',
  surfaceSunken: '#E8EDF1',
  border: 'rgba(27,42,53,0.10)',
  borderStrong: 'rgba(27,42,53,0.20)',
  ink: '#1B2A35',
  inkSoft: '#46606F',
  inkFaint: '#5A6E7D',
  inkDisabled: '#93A4B0',
  accent: '#1D6FA9',
  accentSoft: '#E4F0F8',
  accentPressed: '#175A8A',
  onAccent: '#FFFFFF',
  danger: '#C62F26',
  dangerSoft: '#FBEAE9',
  success: '#0F7A4A',
  successSoft: '#E6F4EC',
  warning: '#A65A00',
  warningSoft: '#FDF0E3',
  divider: 'rgba(27,42,53,0.10)',
  scrim: 'rgba(0,0,0,0.2)',
};

export const darkColors: ThemeColors = {
  bg: '#16242E',
  surface: '#243F4F',
  surfaceSunken: '#2E4E61',
  border: 'rgba(255,255,255,0.12)',
  borderStrong: 'rgba(255,255,255,0.22)',
  ink: '#F2F6F8',
  inkSoft: '#D4DFE6',
  inkFaint: '#9DB3C0',
  inkDisabled: '#5E7686',
  accent: '#5FB0E8',
  accentSoft: '#123243',
  accentPressed: '#88C6F0',
  onAccent: '#0E1A22',
  danger: '#FF8A80',
  dangerSoft: '#3A2323',
  success: '#4FD18B',
  successSoft: '#123326',
  warning: '#FFB84D',
  warningSoft: '#3A2A14',
  divider: 'rgba(255,255,255,0.12)',
  scrim: 'rgba(0,0,0,0.2)',
};

/**
 * One UI spacing runs on a 2dp-resolution scale: 2, 4, 6, 8, 10, 12, 16,
 * 18, 20, 24. `keyline` is the single most important spacing rule in the
 * system — 24dp minimum from each screen edge, to clear curved edges and
 * the Reject/Grip touch-blocking zones.
 */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  keyline: 24,
};

/**
 * One UI buttons are pill-shaped (26dp). A 4dp or 8dp radius button is a
 * strong signal the design is Material, not One UI.
 */
export const radii = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 22,
  container: 26,
  pill: 999,
};

/**
 * One UI's type scale is notably larger than Material's — this is
 * deliberate and is a defining characteristic of the system.
 */
export const text = {
  /** Expanded app bar title; collapses to `title`. */
  display: { fontSize: 34, fontWeight: '400' },
  /** Section and card titles. */
  title: { fontSize: 18, fontWeight: '500' },
  /** Body, list primary, button label. The workhorse. */
  body: { fontSize: 17, fontWeight: '400' },
  button: { fontSize: 17, fontWeight: '500' },
  /** Secondary and supporting text. */
  secondary: { fontSize: 15, fontWeight: '400' },
  /** Caption and metadata. */
  caption: { fontSize: 13, fontWeight: '400' },
  /** Micro labels — tab bars, dense chips. Floor for readable text. */
  micro: { fontSize: 12, fontWeight: '400' },
} as const;

/**
 * The system font resolves to SamsungOne / One UI Sans on Samsung devices,
 * and to the platform default elsewhere. `undefined` is how React Native
 * asks for it; weight is carried by `fontWeight`, not by a family name.
 */
export const type = {
  regular: undefined,
  medium: undefined,
  semibold: undefined,
  bold: undefined,
  extrabold: undefined,
};

export const weight = {
  regular: '400',
  medium: '500',
  semibold: '600',
  bold: '700',
} as const;

/**
 * One UI symbols sit on a consistent optical grid; the default symbol size
 * is 24dp. Stroke weight is uniform across the whole set — mixing weights
 * in one product is the most common iconography violation.
 */
export const icon = {
  /** Inside a compact container: a 32dp circle button, an inline chip. */
  sm: 18,
  /** The One UI default, and what a standalone symbol should be. */
  md: 24,
  /** Empty-state and onboarding illustration. */
  lg: 48,
  stroke: 1.5,
};

/**
 * Real One UI easing curves, from CachedInterpolatorFactory. Tuples are
 * cubic-bezier control points, ready for `Easing.bezier(...)`.
 */
export const easing = {
  sineInOut: [0.33, 0, 0.67, 1],
  standard: [0.4, 0, 0.2, 1],
  emphasizedDecelerate: [0.22, 0.25, 0, 1],
  backGesture: [0.1, 0.1, 0, 1],
  drawerSettle: [0, 0, 0, 1],
} as const;

/** Nothing routine should exceed 500ms. */
export const duration = {
  instant: 100,
  short: 120,
  standard: 167,
  medium: 200,
  emphasized: 260,
  long: 400,
  extended: 500,
};

/**
 * One UI conveys depth with rounded containers, a background tone shift and
 * scrim — not with drop shadows. Large blurred shadows read as Material.
 * Cards separate by tone alone; only genuinely floating surfaces (the
 * overlay window itself, dialogs) carry any elevation at all.
 */
export function getShadow(mode: 'light' | 'dark') {
  const shadowColor = '#000000';
  return {
    card: Platform.select({
      ios: {},
      android: { elevation: 0 },
      default: {},
    }),
    floating: Platform.select({
      ios: {
        shadowColor,
        shadowOpacity: mode === 'dark' ? 0.24 : 0.1,
        shadowRadius: 6,
        shadowOffset: { width: 0, height: 2 },
      },
      android: { elevation: 2 },
      default: {},
    }),
  };
}
