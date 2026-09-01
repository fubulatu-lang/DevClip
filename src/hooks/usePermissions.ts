import { useCallback, useEffect, useState } from 'react';
import { AppState } from 'react-native';
import {
  isNativeOverlayAvailable,
  isAccessibilityServiceEnabled,
  isOverlayPermissionGranted,
  isNotificationPermissionGranted,
} from '../native/OverlayModule';

export interface PermissionState {
  /** Draw over other apps. Without it there is no bubble. */
  overlay: boolean;
  /** Accessibility. Without it there is no capture and no paste. */
  accessibility: boolean;
  /** Notifications. Without them there is no way back to a hidden bubble. */
  notifications: boolean;
}

export const ALL_MISSING: PermissionState = {
  overlay: false,
  accessibility: false,
  notifications: false,
};

export function allGranted(state: PermissionState): boolean {
  return state.overlay && state.accessibility && state.notifications;
}

/**
 * True if anything that was granted at [before] is missing at [now].
 *
 * The test is regression, not difference. Granting one more permission
 * changes the state too, and throwing the user back at the setup screen for
 * making progress would be absurd.
 */
export function hasRegressed(before: PermissionState, now: PermissionState): boolean {
  return (
    (before.overlay && !now.overlay) ||
    (before.accessibility && !now.accessibility) ||
    (before.notifications && !now.notifications)
  );
}

/**
 * Tracks the three permissions DevClip needs, and keeps tracking them.
 *
 * Two of the three cannot be asked for with a dialog: Android makes the user
 * walk into system Settings and flip a switch, and shows a broad warning
 * screen for the accessibility one. So there is no callback to wait on — the
 * only way to know whether the user did it is to look again when they come
 * back, which is what the AppState listener is for.
 *
 * Android can also revoke permissions by itself, for an app that has not been
 * opened in a few months. Nothing announces that either.
 */
export function usePermissions(): { permissions: PermissionState; refresh: () => void } {
  const [permissions, setPermissions] = useState<PermissionState>(ALL_MISSING);

  const refresh = useCallback(async () => {
    // In Expo Go there is no native module and none of this exists. Reporting
    // everything as granted would be a lie; reporting it as missing would put
    // an unskippable-looking wall in front of a build that cannot satisfy it.
    // The gate handles that case by not gating.
    if (!isNativeOverlayAvailable()) {
      setPermissions(ALL_MISSING);
      return;
    }
    const [overlay, accessibility, notifications] = await Promise.all([
      isOverlayPermissionGranted(),
      isAccessibilityServiceEnabled(),
      isNotificationPermissionGranted(),
    ]);
    setPermissions({ overlay, accessibility, notifications });
  }, []);

  useEffect(() => {
    refresh();
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') refresh();
    });
    return () => sub.remove();
  }, [refresh]);

  return { permissions, refresh };
}
