import { NativeModules, Platform, PermissionsAndroid } from 'react-native';

// This maps to the native module registered in
// plugins/android-src/OverlayModule.kt. Until you build a custom dev client
// with EAS, this native module does not exist, so every function below
// safely no-ops / resolves false instead of crashing the app in Expo Go.
const NativeOverlay = NativeModules.DevClipOverlay;

export const isNativeOverlayAvailable = (): boolean =>
  Platform.OS === 'android' && !!NativeOverlay;

export async function requestOverlayPermission(): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.requestOverlayPermission();
}

export async function requestAccessibilityPermission(): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.requestAccessibilityPermission();
}

export async function isAccessibilityServiceEnabled(): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.isAccessibilityServiceEnabled();
}

export async function isOverlayPermissionGranted(): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.isOverlayPermissionGranted();
}

export function startBubble(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.startBubble();
}

export function stopBubble(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.stopBubble();
}

/** Closes the floating list. The bubble stays. */
export function hidePopup(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.hidePopup();
}

/** Opens the full-screen app and closes the overlay. */
export function openFullApp(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.openFullApp();
}

export function setBubbleSize(size: 'small' | 'medium' | 'large'): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setBubbleSize(size);
}

/**
 * Mirrors the clip limit into native.
 *
 * Capture happens with the app closed, so trimming has to happen there too —
 * a limit only applied while DevClip is open is not a limit.
 */
export function setMaxClips(max: number): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setMaxClips(max);
}

export function setAutoStartOnBoot(enabled: boolean): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setAutoStartOnBoot(enabled);
}

export async function isBubbleRunning(): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.isBubbleRunning();
}

// Sets the clipboard AND attempts to paste directly into whatever field was
// last focused in the app underneath (our overlay windows are non-focusable,
// so focus stays there). Returns false if there was no focused field or it
// doesn't support paste — the text is still on the clipboard either way, so
// callers should fall back to telling the user to paste manually.
export async function pasteIntoFocusedField(text: string): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.pasteIntoFocusedField(text);
}

// Standard Android runtime permission (Android 13+/API 33+). Needed for the
// foreground service's notification to actually show. This one DOES trigger
// the normal system permission dialog, unlike overlay/accessibility which
// require a manual Settings toggle.
export async function requestNotificationPermission(): Promise<boolean> {
  if (Platform.OS !== 'android' || Platform.Version < 33) return true;
  const granted = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
  return granted === PermissionsAndroid.RESULTS.GRANTED;
}

export async function isNotificationPermissionGranted(): Promise<boolean> {
  if (Platform.OS !== 'android' || Platform.Version < 33) return true;
  return PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS);
}
