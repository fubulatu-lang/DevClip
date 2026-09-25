# Decisions

Why DevClip captures the way it does, and what was tried instead. This is what
survives of the original build plan: everything else in it has been built,
and the code and `PRODUCT.md` now say what it said. **Re-deriving any of this
costs days** — do not re-explore it without new information.

## Rejected approaches

Three approaches were investigated in depth and rejected. Each looks obvious and
each is a dead end. **Re-deriving these costs days.**

### 1. Reading the clipboard in the background — impossible

Since Android 10, `ClipboardManager.getPrimaryClip()` returns null unless the app
has input focus or is the default IME. In `ClipboardService.clipboardAccessAllowed`:

```java
if (appOpsResult != AppOpsManager.MODE_ALLOWED) return false;   // app-op check
if (mPm.checkPermission(READ_CLIPBOARD_IN_BACKGROUND, pkg) == GRANTED) {
    allowed = true;                                              // the real exemption
} else {
    allowed = mWm.isUidFocused(uid) || isDefaultIme(...) || ...;  // focus check
}
```

**Consequences:**

- An `OnPrimaryClipChangedListener` in the accessibility service is dead code on
  any modern Android. Accessibility services get **no** clipboard exemption.
- `adb shell appops set com.devclip.app READ_CLIPBOARD allow` **does not work**.
  It is widely repeated and it is cargo-cult: `READ_CLIPBOARD` is already
  `MODE_ALLOWED` by default, so the command is a no-op, and it cannot touch the
  focus check at all. People report success because they test in the foreground.
- `READ_CLIPBOARD_IN_BACKGROUND` is signature-protected. `pm grant` cannot grant it.
- A Capture button inside the floating overlay cannot work — the overlay is
  deliberately non-focusable, so DevClip never counts as "in front". It would
  silently save nothing.

### 2. Shipping an IME (keyboard) — rejected

The default-IME exemption is real and package-wide: while DevClip holds
`Settings.Secure.DEFAULT_INPUT_METHOD`, *every* DevClip process gets clipboard
access regardless of focus.

But "default" means **the currently selected keyboard**. There is one slot. To get
capture this way the user must abandon Gboard and type on a keyboard we would have
to build — months of work, judged against Gboard. Rejected.

(A *clip-picker* IME that coexists with Gboard was also considered. It would
improve paste — `InputConnection.commitText()` beats accessibility paste — but
gives no capture, since it is not the default IME. Deferred, not needed.)

### 3. Shizuku — rejected as not worth it

Shizuku runs code as the shell uid (2000), which **does** have
`READ_CLIPBOARD_IN_BACKGROUND` ("Shell can access the clipboard for testing
purposes" in AOSP). Genuine background capture is achievable this way.

The clean implementation is a Shizuku `UserService` + a `FakeContext` whose
`getOpPackageName()` returns `"com.android.shell"`, then calling the ordinary
public `ClipboardManager` API — this is what scrcpy does, and it avoids
per-version AIDL reflection. (`IClipboard.getPrimaryClip` has grown a parameter
roughly every other release: `(pkg)` → `(pkg, userId)` on 10 →
`(pkg, attributionTag, userId)` on 12 → `(pkg, attributionTag, userId, deviceId)`
on 14. Reflecting on it means chasing that forever; scrcpy hit exactly this.)

**Rejected because:**

- Shizuku **dies on every reboot** for non-root users. The user must re-pair
  through wireless debugging each time. "It works until I restart my phone."
- Requires installing a second app.
- Shell cannot write DevClip's private database (uid mismatch), so it can only
  relay text — meaning a persistent foreground service must be alive anyway.
- Multi-week build for a feature the selection-capture approach delivers in a day
  with no extra permissions and no setup ritual.

**If it is ever revisited:** prefer polling `getPrimaryClipDescription().getTimestamp()`
over `addPrimaryClipChangedListener`; the listener path is the one that has
historically broken across releases.

## Deliberately deferred

**Raising the huge-selection ceiling via `ACTION_COPY` + a brief focus grab.**
Rather than DevClip reading text out, the accessibility service can tell the
*source app* to copy its own selection (`AccessibilityNodeInfo.ACTION_COPY`) — no
large payload crosses our Binder transaction. DevClip then briefly takes focus
(a focusable overlay window) to read the clipboard. The selection is already
copied by then, so losing the highlight costs nothing.

**Not in the first pass.** It depends on an untested assumption (that a brief
focus grab satisfies Android's clipboard check) and it causes a visible focus
flicker that may drop the keyboard. Ship the direct read, prove it on a device,
then add this tier for oversized selections only.

## Unproven — verify on device first

1. **That tapping the bubble does not clear the text selection.** Everything
   rests on this. The remembered-selection fallback in `SelectionCapture`
   exists because of it.
2. **How well selection reading works app to app.** Standard text fields and web
   pages should be fine. Apps that draw their own text (some games, canvas-based
   apps) may give nothing. Nothing catches all of it.
