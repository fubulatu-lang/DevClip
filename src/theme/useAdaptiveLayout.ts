import { useWindowDimensions } from 'react-native';

export type SizeClass = 'compact' | 'medium' | 'expanded';

export interface AdaptiveLayout {
  sizeClass: SizeClass;
  /** Horizontal inset from the window edge, in dp. */
  gutter: number;
  /** How many clip cards sit side by side. */
  columns: number;
}

/**
 * One UI's own adaptive rule, from AdaptiveCoordinatorLayout: below 589dp the
 * 24dp keyline is the whole margin; from 589dp the side margin becomes 5% of
 * the width; from 960dp it becomes 12.5%.
 *
 * Driven by the window, never by a device model — so a phone in landscape, a
 * tablet in split view, and a folding phone opening all pick the right shape
 * for the width they actually have, including the small overlay window, which
 * is simply always compact.
 */
export function useAdaptiveLayout(): AdaptiveLayout {
  const { width } = useWindowDimensions();

  if (width >= 960) {
    return { sizeClass: 'expanded', gutter: Math.round(width * 0.125), columns: 2 };
  }
  if (width >= 589) {
    return { sizeClass: 'medium', gutter: Math.round(width * 0.05), columns: 2 };
  }
  return { sizeClass: 'compact', gutter: 24, columns: 1 };
}
