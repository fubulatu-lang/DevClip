import * as Clipboard from 'expo-clipboard';
import { isNativeOverlayAvailable, pasteIntoFocusedField } from '../native/OverlayModule';

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

// Result of attempting to hand a clip back to whatever the user was doing:
// 'pasted'      — inserted directly into the field they were using
// 'copiedOnly'  — no focused field found (or it doesn't support paste);
//                 the text is still on the clipboard for a manual paste
export type PasteResult = 'pasted' | 'copiedOnly';

export async function pasteClip(text: string): Promise<PasteResult> {
  if (isNativeOverlayAvailable()) {
    const didPaste = await pasteIntoFocusedField(text);
    if (didPaste) return 'pasted';
  }
  // No native module (Expo Go), or native paste couldn't find/use a focused
  // field — the clipboard is already set by pasteIntoFocusedField in the
  // native-available case; set it here for the Expo Go / fallback case too.
  await writeSystemClipboard(text);
  return 'copiedOnly';
}
