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

/**
 * Bubble diameter in dp, applied live.
 *
 * Native clamps to the same floor and ceiling this side does, because native
 * is the one that has to be right when the value came from SharedPreferences
 * rather than from here.
 */
export function setBubbleSize(sizeDp: number): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setBubbleSize(sizeDp);
}

/** Hides the bubble. The service keeps running; the notification brings it back. */
export function restBubble(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.restBubble();
}

/** Brings a hidden bubble back, at the position the user left it. */
export function wakeBubble(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.wakeBubble();
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

/**
 * How opaque the bubble is, 20-100.
 *
 * Native clamps to the same floor this side does, because native is the one
 * that has to be right when the value came from SharedPreferences rather than
 * from here.
 */
export function setBubbleAlpha(alpha: number): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setBubbleAlpha(alpha);
}

/**
 * Whether the chosen transparency applies only while the bubble is idle.
 *
 * Off, the bubble simply sits at that level. On, it rests there and returns to
 * solid the moment it is touched — which is what makes a very faint bubble
 * usable, because you can see it as you reach for it.
 */
export function setBubbleIdleFade(enabled: boolean): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setBubbleIdleFade(enabled);
}

/** How opaque the floating list is, 20-100. */
export function setPopupAlpha(alpha: number): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setPopupAlpha(alpha);
}

/** Seconds of stillness before the bubble tucks itself away. 0 never does. */
export function setTuckDelay(seconds: number): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setTuckDelay(seconds);
}

/**
 * Mirrors tap-to-arm into native.
 *
 * The floating list is drawn natively and opens with no React context behind
 * it, so a setting it obeys has to be readable from SharedPreferences — the
 * same reason the clip limit is mirrored.
 */
export function setConfirmBeforePaste(enabled: boolean): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.setConfirmBeforePaste(enabled);
}

/**
 * Whether Android is allowed to put DevClip to sleep.
 *
 * Not cosmetic. Capture depends on the accessibility service staying bound,
 * and a phone that decides DevClip is idle will quietly unbind it — leaving
 * the permission reading as granted while nothing is listening. That is the
 * hardest failure in this app to recognise from the outside, so the app says
 * so rather than waiting to be asked.
 */
export async function isBatteryOptimised(): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.isBatteryOptimised();
}

/**
 * Raises Android's own "stop optimising this app" dialog.
 *
 * Resolves false when the system would not show it, in which case native has
 * already opened the app-details screen instead — there is no way for an app
 * to grant itself this, only to ask.
 */
export async function requestIgnoreBatteryOptimisations(): Promise<boolean> {
  if (!isNativeOverlayAvailable()) return false;
  return NativeOverlay.requestIgnoreBatteryOptimisations();
}

/**
 * Opens DevClip's app-details screen.
 *
 * Samsung's "deep sleeping apps" list is not reachable through any public API
 * — it cannot even be read — so for the manufacturer most likely to break
 * capture, this plus a plain instruction is the whole of what an app can do.
 */
export function openBatterySettings(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.openBatterySettings();
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
