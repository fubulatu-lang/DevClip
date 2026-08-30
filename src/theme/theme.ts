import { Platform } from 'react-native';

/**
 * DevClip visual language — Samsung One UI
 *
 * Colour is role-based, not palette-based. Depth comes from rounded
 * containers and a background tone shift, never from large blurred shadows.
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
  bg: '#FAFAFA',
  surface: '#FFFFFF',
  surfaceSunken: '#F0F0F0',
  border: 'rgba(0,0,0,0.08)',
  borderStrong: 'rgba(0,0,0,0.16)',
  ink: '#252525',
  inkSoft: '#505050',
  inkFaint: '#6E6E6E',
  inkDisabled: '#8C8C8C',
  accent: '#0072DE',
  accentSoft: '#E6F1FC',
  accentPressed: '#005CB8',
  onAccent: '#FFFFFF',
  danger: '#D32B22',
  dangerSoft: '#FBEAE9',
  success: '#00803A',
  successSoft: '#E6F4EC',
  warning: '#B35A00',
  warningSoft: '#FDF0E3',
  divider: 'rgba(0,0,0,0.08)',
  scrim: 'rgba(0,0,0,0.2)',
};

export const darkColors: ThemeColors = {
  bg: '#000000',
  surface: '#1B1B1B',
  surfaceSunken: '#252525',
  border: 'rgba(255,255,255,0.12)',
  borderStrong: 'rgba(255,255,255,0.2)',
  ink: '#FAFAFA',
  inkSoft: '#E5E5E5',
  inkFaint: '#8A8A8A',
  inkDisabled: '#5C5C5C',
  accent: '#3E91FF',
  accentSoft: '#0B2A4A',
  accentPressed: '#6BAAFF',
  onAccent: '#000000',
  danger: '#FF7B70',
  dangerSoft: '#3A2323',
  success: '#3DD68C',
  successSoft: '#123326',
  warning: '#FFA733',
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
