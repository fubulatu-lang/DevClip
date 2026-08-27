import * as Clipboard from 'expo-clipboard';

// Phase 1: reads whatever is currently on the system clipboard, on demand
// (e.g. a "Capture" button, or when the app comes to the foreground).
// Phase 2 replaces/augments this with ClipboardAccessibilityService.kt,
// which captures copies automatically in the background system-wide.
export async function readSystemClipboard(): Promise<string | null> {
  const hasText = await Clipboard.hasStringAsync();
  if (!hasText) return null;
  const text = await Clipboard.getStringAsync();
  return text && text.trim().length > 0 ? text : null;
}

export async function writeSystemClipboard(text: string): Promise<void> {
  await Clipboard.setStringAsync(text);
}
