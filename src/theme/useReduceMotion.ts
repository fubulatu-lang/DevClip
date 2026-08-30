import { useEffect, useState } from 'react';
import { AccessibilityInfo } from 'react-native';

/**
 * Tracks the platform reduce-motion setting — Android's animator duration
 * scale, iOS's Reduce Motion — and keeps following it while mounted, since
 * the user can change it without leaving the app.
 *
 * Every animation in DevClip is expected to consult this. What "respecting"
 * it means differs per animation: a transform can simply be skipped, while
 * a screen transition should become an instant cut rather than disappear.
 */
export function useReduceMotion(): boolean {
  const [reduceMotion, setReduceMotion] = useState(false);

  useEffect(() => {
    let cancelled = false;
    AccessibilityInfo.isReduceMotionEnabled().then((enabled) => {
      if (!cancelled) setReduceMotion(enabled);
    });
    const sub = AccessibilityInfo.addEventListener('reduceMotionChanged', setReduceMotion);
    return () => {
      cancelled = true;
      sub.remove();
    };
  }, []);

  return reduceMotion;
}
