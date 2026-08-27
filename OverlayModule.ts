import { NativeModules, Platform } from 'react-native';

// This maps to the native module registered in
// plugins/android-src/OverlayModule.kt (Phase 2). Until you build a custom
// dev client with EAS (see SETUP_GUIDE.md), this native module does not
// exist, so every function below safely no-ops / resolves false instead of
// crashing the app in Expo Go.
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

export function startBubble(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.startBubble();
}

export function stopBubble(): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.stopBubble();
}

// width/height in dp
export function resizePopupWindow(width: number, height: number): void {
  if (!isNativeOverlayAvailable()) return;
  NativeOverlay.resizePopupWindow(width, height);
}
