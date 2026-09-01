# DevClip — Build Plan (handoff)

**Status:** planning complete, nothing built yet.

**Before writing any code, refresh the branch.** This document was merged to
`main` by its own PR, which is now closed and cannot carry the implementation
work. `claude/copy-direct-to-app-n6sk25` is a snapshot from before that merge, so
starting from it means building on a stale picture of the repo:

```
git fetch origin main
git checkout -B claude/copy-direct-to-app-n6sk25 origin/main
```

The implementation goes in a **new** pull request.

This document is a complete handoff from a planning session. It is written so a
fresh Claude Code session can pick up the work with no prior context and lose
nothing — including the reasoning behind decisions and, importantly, the
approaches that were **investigated and rejected**, so they are not re-explored.

Read all of it before writing code. Section 6 (rejected approaches) is the one
most likely to be skipped and the most expensive to rediscover.

---

## 1. How to use this document

- **Sections 2–5** are the spec. Build to these.
- **Section 6** is why the obvious-looking alternatives are dead ends. Do not
  re-litigate without new information.
- **Section 7** lists five diagnosed bugs with file:line references.
- **Sections 8–10** are removals, constraints, and build order.
- **Section 11** sets testing expectations honestly.

The user is not a deep technical specialist and explicitly asked for plain
language. Explain things in natural language, not jargon. They make good
decisions when given clear trade-offs; give them options with a recommendation,
not a lecture.

**Project rule that overrides defaults:** `AGENTS.md` says to read the exact
versioned Expo docs at https://docs.expo.dev/versions/v57.0.0/ before writing
any code. Honour that.

---

## 2. The goal in one line

**Highlight text anywhere on the phone, tap the floating bubble, and the text is
saved into DevClip.** No pressing "Capture clipboard", no switching apps.

That was the user's original complaint and it is the point of all the work below.

---

## 3. Current state of the codebase

Expo SDK 57, React Native 0.86, TypeScript, zustand, expo-sqlite. Android only in
practice (native overlay + accessibility service via a config plugin).

### Key files

| Path | Role |
| --- | --- |
| `plugins/withDevClipNative.js` | Expo config plugin. Copies Kotlin files into the generated android project, adds permissions, services, receivers, strings. Runs on every prebuild/EAS build. |
| `plugins/android-src/OverlayService.kt` | Foreground service. Owns the bubble window and the React Native popup window. Most of the native work lands here. |
| `plugins/android-src/ClipboardAccessibilityService.kt` | Accessibility service. Currently: clipboard listener (broken, see §6) + `pasteIntoFocusedField`. Selection capture goes here. |
| `plugins/android-src/OverlayModule.kt` | React Native bridge module (`DevClipOverlay`) + `Prefs` object (SharedPreferences keys shared with native components). |
| `plugins/android-src/DevClipDatabaseHelper.kt` | Native SQLite access to the **same** db file the JS side opens. Schema must stay in sync with `src/db/database.ts`. |
| `plugins/android-src/BootReceiver.kt` | Restarts the bubble on boot when enabled. |
| `plugins/android-src/accessibility_service_config.xml` | Accessibility service config. Event types need changing (§9). |
| `src/native/OverlayModule.ts` | JS wrapper over the native module. Safely no-ops when the native module is absent (Expo Go). |
| `src/store/clipStore.ts` | Clip list state, capture, CRUD, sort. |
| `src/store/settingsStore.ts` | Persisted settings (AsyncStorage), mirrors some values into native via `OverlayModule`. |
| `src/db/database.ts` | JS SQLite layer. `devclip.db`, table `clips`. |
| `src/screens/OverlayScreen.tsx` | The floating window's React root content (mini + expanded). |
| `src/screens/ClipListView.tsx` | The full-screen app's list. |
| `src/screens/SettingsScreen.tsx` | Settings. |
| `src/screens/OnboardingScreen.tsx` | First-run permission flow. Needs rework (§4.6). |
| `src/components/ClipListItem.tsx` | A clip row. Paste-on-tap lives here. |
| `src/components/EditClipModal.tsx` | Edit sheet. Currently a `Modal` — must change (§7, bug 4). |
| `src/utils/clipboardCapture.ts` | Clipboard read/write + `pasteClip`. |
| `src/utils/backup.ts` | Export only, no import yet. |
| `index.ts` | Registers `App` (launcher) and `DevClipPopup` (overlay surface). |

### Two React roots, one process

`index.ts` registers two roots. `App` is the full-screen app. `DevClipPopup` is
the floating window, mounted by `OverlayService` via
`reactHost.createSurface(...)` (`OverlayService.kt:327`). They share the zustand
stores and the SQLite database but are separate trees.

### The database is shared between JS and native

`src/db/database.ts` opens `devclip.db` through expo-sqlite;
`DevClipDatabaseHelper.kt` opens the same file directly at
`<filesDir>/SQLite/devclip.db`. **If you change the schema, change both.**

---

## 4. The agreed product spec

### 4.1 The bubble

- Lives docked to the **left or right edge**. Slides freely up and down. If
  dragged into the middle it **snaps to the nearer edge** on release.
- **Remembers its position** across service restarts and reboots.
  - Store as `edge` (left/right) + `yFraction` (0.0–1.0), **not pixels** —
    survives rotation, split-screen, foldables, different devices.
  - Lives in `Prefs` (SharedPreferences), because `OverlayService` needs it at
    startup — potentially from `BootReceiver`, long before any React context
    exists. **No JS mirror, no settings UI for position.**
  - Write on drag-release, debounced (a fling must not produce dozens of writes).
- **Two positions must be tracked:**
  - **Parked** — persisted, where the user put it.
  - **Displaced** — transient, when the keyboard pushes it up. Never persisted.
    On keyboard hide, animate back to parked.
  - If the user drags while the keyboard is up, the drop point becomes the new
    parked position (constrained to the visible band). Accepted consequence: the
    keyboard can bias parked position upward over time.

### 4.2 Bubble gestures

| Gesture | Behaviour |
| --- | --- |
| **Tap** | If text is selected anywhere on screen → capture it. Otherwise → open the mini list. |
| **Long-press** | Always open the mini list, even when text is selected. |
| **Drag** | Move along the edge. |
| **Drag to bottom-centre target** | Hide the bubble (see §4.4). |

The bubble should **visibly change when a selection is live**, so tap-to-capture
is advertised rather than hidden (a ring, tint or badge).

**Why bottom-centre matters:** the bubble lives on the edges, so dragging
straight down a rail to park it low never enters the dismiss target. Only a
deliberate diagonal drag into the middle-bottom hides it. This is a direct payoff
of edge-docking — do not move the target to a full-width bottom strip.

### 4.3 Capture behaviour

1. Read the current selection (see §9 for mechanism, and the fallback).
2. Save to SQLite.
3. **Then** also set the system clipboard, so the bubble is a full replacement
   for Android's Copy button. Order matters: if a huge clip fails the clipboard
   write, it is still saved.
4. Feedback: haptic buzz + the bubble flashes + a brief message showing the
   **first few words of what was saved**. The preview is not decoration — it
   confirms the right text was grabbed, which matters because selection reading
   is not perfect across all apps.
5. **Duplicates:** skip only if identical to the clip currently at the top.
   (Catches accidental double-taps. Older duplicates are allowed.)
6. **Size:** capture everything, do not impose a DevClip-side cap. If Android
   truncates (see §10), tell the user honestly that *Android* passed along only
   the first N characters — DevClip is not the one trimming.
7. **Password fields:** skip explicitly rather than saving a row of bullets.
8. Trimming to `maxClips` must now also run at capture time, not only when the
   app is open.

### 4.4 Hiding the bubble

Three states, where the code currently has two:

```
service stopped          → nothing
service running, awake   → bubble visible
service running, resting → bubble hidden; notification is the way back
```

- Drag-to-dismiss = **hide only**. The service keeps running.
- Ways back: notification action, or in-app.
- **"Resting" does not persist across service restarts.** After a reboot the
  bubble comes back visible. (User decision: hiding means "get out of my way
  now", not a preference.)
- Dismiss target: circular ✕ at bottom-centre, appears only once a drag starts,
  magnetises when near (~1.5× target radius), haptic on **entering** the magnet
  zone (not on release, so the commit is felt before letting go), plus a visible
  change. Needs a scrim behind it — a pale ✕ on a pale app is invisible.
- Require release **inside** the target, not merely near it.
- Confirmation after hiding: a plain `Toast` (no window needed — the windows were
  just torn down). Copy must adapt: if notifications are blocked, say "reopen it
  in DevClip", not "from the notification panel".
- Hiding the bubble also closes the mini list.
- Waking restores the bubble at its parked position.

### 4.5 The notification

It becomes load-bearing (it is the way back), and it is not currently built for
that.

- **Raise `IMPORTANCE_MIN` → `IMPORTANCE_LOW`** (`OverlayService.kt:137`).
  MIN-importance notifications get collapsed into the silent section and action
  buttons frequently do not render. LOW is still silent, no heads-up, but renders
  normally with actions.
- Add `setOngoing(true)` to resist swipe-away — but see §10, this is not a
  guarantee on modern Android, so **in-app restore must be first-class, not a
  fallback**.
- Content by state:

| State | Text | Actions |
| --- | --- | --- |
| Awake | DevClip is running | **Hide bubble** · **Turn off** |
| Resting | DevClip is running · Bubble hidden | **Show bubble** · **Turn off** |

- Body tap opens the app in both states.
- Avoid the word "Dismiss" in copy — it implies permanence this gesture does not
  have. Hide / Show / Turn off says what actually happens.
- The **Hide bubble** action is also the accessible equivalent of the
  drag gesture, which would otherwise be gesture-only.

### 4.6 Permissions — mandatory, but skippable

Three permissions: draw over other apps, accessibility, notifications. Two of
them cannot be a simple runtime prompt — Android requires the user to go into
system Settings and flip a switch, and accessibility shows a scary warning first.

- The setup screen is **a wall you can walk past**, with the app clearly showing
  it is crippled while permissions are missing.
- **If a permission is revoked later, throw the user back to that same wall the
  moment the app notices** (which they can again walk past). Android can also
  revoke permissions by itself if an app is unused for months.
- Not going to the Play Store for now — it is a dev build. **Revisit the
  accessibility-permission policy question before publishing** (Google requires a
  declaration and rejects apps using accessibility for non-accessibility ends).

### 4.7 The lists

- **Sort: newest-first only, app-wide.** The sort menu goes away entirely.
  Search still exists in the full app, and that is where finding things belongs.
- **Numbered rows.** Newest = 1. Numbers are **positional, not identity** — they
  renumber as clips arrive and are deleted. This is intended.
- **Mini list:** no search box. Font smaller **relative to the user's system font
  scale**, not a fixed small size — so it stays compact for this user without
  becoming unreadable for someone who has large text turned on. Same reasoning as
  the bubble's minimum size.
- **Mini list should be noticeably taller** than today, since cutting the
  expanded sheet leaves it as the only floating surface.
- Long clips: rows show a preview (first couple of lines). Full text lives in the
  database and comes out whole on paste. A half-megabyte clip rendered in full
  would make the list crawl.

### 4.8 Paste

- **Tap once to arm, tap again to paste.** The row visibly changes to "Tap again
  to paste" for ~2 seconds, then relaxes.
- Applies in **both** the full app and the mini list — same behaviour in both.
- The existing `confirmBeforePaste` setting still turns this on/off.
- This replaces the current `Alert.alert` confirmation, which is broken in the
  floating window (§7, bug 5) and would be cramped there even if it worked.

### 4.9 Settings

- **Bubble size becomes a slider.** Floor ≈ 48dp (Android's comfortable tap
  target — smaller and it gets missed, especially over a keyboard), ceiling
  ≈ 1.5× that.
  - **It must resize live.** `setBubbleSize` currently tears down and restarts
    the bubble; with a slider that would flicker on every pixel of the drag.
- Keep: theme, `autoStartOnBoot`, `confirmBeforePaste`, `maxClips`.
- Remove: sort-related settings, bubble position (never added — deliberately not
  configurable, drag is the only way).

### 4.10 Backup

- **Export and import.**
- **Import merges**, skipping anything whose text already exists. Re-importing
  the same file twice does nothing the second time.
- Export file **includes timestamps** so ordering survives the round trip.

---

## 5. Capture mechanism

**Read the selection directly from the accessibility node tree. Never touch the
clipboard for capture.** This is the core insight of the whole design — see §6
for why every clipboard-based approach is dead.

Two paths, build **both** from the start:

1. **Live read (primary).** `getRootInActiveWindow()`, walk for a node where
   `getTextSelectionStart() != getTextSelectionEnd()`, take
   `node.getText().subSequence(start, end)`. A tree walk on a user tap is fine —
   it is a gesture, not a hot loop.
2. **Remembered selection (fallback).** Listen for
   `TYPE_VIEW_TEXT_SELECTION_CHANGED`, cache the last
   `(text, fromIndex, toIndex, timestamp)` where `from != to`. Filter equal
   indices — those are plain cursor moves and fire on every keystroke in an edit
   field. If the live read comes back empty at tap time, use a recent cached
   value.

**Config change required:** `accessibility_service_config.xml` currently declares
`typeWindowStateChanged|typeViewFocused`. Add `typeViewTextSelectionChanged`.

No new permissions. The service already declares `canRetrieveWindowContent="true"`
and `flagRetrieveInteractiveWindows`, which is exactly what is needed.

**Why the fallback exists:** the entire feature rests on the assumption that
tapping the bubble does not clear the user's text selection. It should not,
because the bubble window is `FLAG_NOT_FOCUSABLE` (`OverlayService.kt:216`) and
never takes input focus — the same property that makes `pasteIntoFocusedField`
work. But this **cannot be verified without a device**, so the fallback is built
from day one rather than discovered late.

### Keyboard avoidance

Detect the IME window via `getWindows()` → the entry with
`AccessibilityWindowInfo.TYPE_INPUT_METHOD` → `getBoundsInScreen()`. Requires the
accessibility service, which is already mandatory.

There is **no non-accessibility route worth building**: an overlay window with
`FLAG_NOT_FOCUSABLE` does not receive IME insets, so it cannot learn the keyboard
height on its own. If accessibility is off, the user has no capture either, so
degrade quietly.

Z-order is fine: `TYPE_APPLICATION_OVERLAY` draws above the IME — which is
exactly why the collision exists — so the dismiss target renders over a raised
keyboard without trouble.

---

## 6. Rejected approaches — do not re-explore

Three approaches were investigated in depth and rejected. Each looks obvious and
each is a dead end. **Re-deriving these costs days.**

### 6.1 Reading the clipboard in the background — impossible

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

- `ClipboardAccessibilityService.kt`'s `OnPrimaryClipChangedListener` is dead code
  on any modern Android. Accessibility services get **no** clipboard exemption.
- `adb shell appops set com.devclip.app READ_CLIPBOARD allow` **does not work**.
  It is widely repeated and it is cargo-cult: `READ_CLIPBOARD` is already
  `MODE_ALLOWED` by default, so the command is a no-op, and it cannot touch the
  focus check at all. People report success because they test in the foreground.
- `READ_CLIPBOARD_IN_BACKGROUND` is signature-protected. `pm grant` cannot grant it.
- **The Capture button inside the floating overlay cannot work** — the overlay is
  deliberately non-focusable, so DevClip never counts as "in front". It silently
  saves nothing. (In the full-screen app it works fine.)

### 6.2 Shipping an IME (keyboard) — rejected

The default-IME exemption is real and package-wide: while DevClip holds
`Settings.Secure.DEFAULT_INPUT_METHOD`, *every* DevClip process gets clipboard
access regardless of focus.

But "default" means **the currently selected keyboard**. There is one slot. To get
capture this way the user must abandon Gboard and type on a keyboard we would have
to build — months of work, judged against Gboard. Rejected.

(A *clip-picker* IME that coexists with Gboard was also considered. It would
improve paste — `InputConnection.commitText()` beats accessibility paste — but
gives no capture, since it is not the default IME. Deferred, not needed.)

### 6.3 Shizuku — rejected as not worth it

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

---

## 7. Bugs to fix — five, four diagnosed

### Bug 1 — mini list does not follow the bubble while dragging. **Proven.**

`OverlayService.kt:262`:

```kotlin
if (popupVisible && mode == MODE_MINI) applyPopupGeometry()
```

`applyPopupGeometry()` only mutates the `params` object. It does **not** call
`windowManager.updateViewLayout()`. Every other call site pairs the two —
lines 118–119, 365–366, 411–412. The drag handler is the only place that does
not, so the popup's intended geometry is recalculated on every frame and never
pushed to the window manager.

**Fix:** call `updatePopupLayout()` after it, as everywhere else.

### Bug 2 — tapping the bubble shows an empty shell. **Three suspects, needs a device log.**

The visible coloured background is a deliberate safety net
(`OverlayService.kt:341-347`) so an undrawn window is not invisible. The user is
seeing the safety net with nothing behind it.

Suspects:

1. **The React host was never started.** The surface is created and started at
   `OverlayService.kt:327-328`, but if the service was launched by `BootReceiver`
   or the app was swiped away, no Activity ever ran and the JS bundle may never
   have loaded.
2. **Something throws while the overlay tree mounts** — most likely `init()`
   opening SQLite (`OverlayScreen.tsx:45`). There is no error boundary in
   `OverlayApp.tsx`, so React renders nothing rather than surfacing the error.
3. **Measurement** — the surface starts before it has layout constraints.

**Approach:** defend against all three (explicitly start the host if it is not
running; add an error boundary that renders the error; add a visible loading
state). That will probably fix it. If it survives, get a logcat from the user —
that will name the cause.

### Bug 3 — Capture button dead inside the floating window

See §6.1. Being removed anyway as part of the cleanup.

### Bug 4 — edit sheet buried by the keyboard. **Diagnosed.**

`EditClipModal.tsx:111` uses a React Native `<Modal>`. The comment at
`EditClipModal.tsx:117-122` argues no keyboard handling is needed because the
activity uses `adjustResize`.

That reasoning is correct for the activity's window — but `<Modal>` renders into
its **own Android window**, which does not inherit the activity's soft-input mode.
The main window shrinks, the modal's window does not, and the sheet stays pinned
to the bottom of the full screen under the keyboard.

A previous attempt added `KeyboardAvoidingView behavior="height"`, which did
nothing, and the wrong conclusion was drawn.

**Fix (user's decision):** stop making the edit sheet a pop-up. Render it inline
in the main window as an absolutely-positioned sheet. Then the existing comment
becomes true and `adjustResize` handles the keyboard. Requires handling back-button
dismissal and z-ordering manually.

### Bug 5 — confirm-before-paste dead in the floating window. **Diagnosed.**

`ClipListItem.tsx:110` uses `Alert.alert`. Android system dialogs need a
foreground Activity to attach to; the floating window has none by design. With
confirm enabled, tapping a clip in the mini list likely does nothing at all.

**Fix:** the tap-to-arm / tap-again-to-paste interaction in §4.8. Solves the bug
and the "dialog in a tiny window" design problem at once.

---

## 8. Removals

**Dead already — no decision needed:**

- `PopupState` in `src/types/clip.ts` — declared, referenced nowhere.
- The Capture button inside the floating overlay (bug 3).

**Decided removals:**

- **The expanded half-screen sheet.** `MODE_EXPANDED` and its geometry branch in
  `applyPopupGeometry()`, the mode toggle at `OverlayScreen.tsx:144`, the
  `OverlayMode` type in `src/native/OverlayModule.ts`, `ACTION_SET_MODE`,
  `setOverlayMode`. Removes the trickiest native geometry code.
- **Manual reordering.** `moveUp`/`moveDown` in `clipStore.ts`, `swapClipOrder`
  and `snapshotOrder` in `database.ts`, the up/down buttons in `ClipListItem.tsx`,
  the `'manual'` sort mode, and the reorder strings.
- **The sort menu entirely** — `src/components/SortMenu.tsx`, the `SortMode` type,
  sort state in `clipStore`. Newest-first is the only order.
- **Search in the mini list** — never add it; search stays in the full app only.

**Kept:**

- The Capture button in the **full-screen app** — it works there, and it is the
  only way to catch something copied with Android's own Copy button. Shrink it
  down from the big primary button it is now.
- `confirmBeforePaste` (reworked per §4.8), `maxClips`, `autoStartOnBoot`, theme,
  bubble size (now a slider).

**Schema note:** `clips.sort_order` becomes unused once manual reordering goes.
**Leave the column in place.** Dropping columns in SQLite is more trouble than it
is worth, and existing user data must not be disturbed.

---

## 9. Naming cleanup to do first

`ACTION_HIDE` (`OverlayService.kt:62`, dispatched at `:126`) currently means
"hide the popup, bubble stays" and is reached from JS as `hideOverlay()`. The new
hide-the-bubble action is a different thing.

Rename the existing one to `ACTION_HIDE_POPUP` and add `ACTION_REST` /
`ACTION_WAKE` / `ACTION_STOP`. Do this before adding the new actions, so nobody
reads the wrong one later.

---

## 10. Platform constraints and unproven assumptions

### Hard limits (cannot be engineered away)

- **~1MB Binder transaction buffer, shared across the whole process.** Everything
  passing between apps goes through it. This caps both reading a huge selection
  out of another app *and* putting it on the system clipboard. Practically,
  transactions that break it are often much smaller than 1MB because the buffer
  is shared. A few hundred thousand characters is comfortable; approaching a
  million is unreliable in a load-dependent way; beyond that it fails.
  - For scale: a very long web article is ~50,000 characters.
  - **DevClip's own database has no such limit.** The constraint is purely the
    handoff between apps. Message the user accordingly: *Android* truncated it,
    DevClip did not.
- **Android 13+ lets users swipe away foreground-service notifications** while
  the service keeps running. `setOngoing(true)` is the lever but the guarantee
  has drifted across releases. Hence: in-app restore is first-class.
- **Android 13+ `POST_NOTIFICATIONS` denial** means no notification at all, so
  the notification route to restore the bubble silently does not exist.
  `isNotificationPermissionGranted()` already exists in `src/native/OverlayModule.ts`
  — check it and adapt the toast copy.

### Deliberately deferred

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

### Unproven — verify on device first

1. **That tapping the bubble does not clear the text selection.** Everything
   rests on this. The remembered-selection fallback (§5) exists because of it.
2. **How well selection reading works app to app.** Standard text fields and web
   pages should be fine. Apps that draw their own text (some games, canvas-based
   apps) may give nothing. Nothing catches all of it.

---

## 11. Build order

One pull request on a **freshly reset** `claude/copy-direct-to-app-n6sk25` (see
the top of this document), **commits split by area** so it is reviewable in
pieces and a single bad part can be reverted.

1. **Bug fixes + naming cleanup** — bug 1 (one line), bug 2 defences, bug 4
   (inline edit sheet), the `ACTION_HIDE` rename. Gets the user a working app early.
2. **Removals** — expanded sheet, manual reordering, sort menu, dead types, the
   overlay Capture button. Smaller surface for everything that follows.
3. **Native → JS event channel.** A small `DeviceEventEmitter` channel from
   native. **Three separate features need it** — a new clip captured, bubble
   rest/wake state, and the currently-stale clip list. Build it once, properly.
   - Note: `OverlayScreen.tsx:45` runs `init()` once on mount and nothing
     refreshes on foreground or on native insert. Native could be saving clips
     perfectly today and the list would never show them. **Fix this before
     testing capture** or the feature will look broken while working.
4. **The capture feature** — selection reading (both paths), the tap/long-press
   split, clipboard write, dedupe, feedback, password-field skip, capture-time
   trimming. *This is the feature everything else is in service of.*
5. **Keyboard avoidance** — parked vs displaced positions.
6. **Bubble work** — edge-docking, persisted position, drag-to-hide with the
   bottom-centre target, notification rework, live-resizing size slider.
7. **Lists** — numbering, newest-first, mini list sizing and relative font scale,
   tap-to-arm paste.
8. **Permissions wall** — skippable gate, re-gate on revocation.
9. **Backup** — export with timestamps, merging import.
10. **Docs** — `README.md`, `PRODUCT.md`, `DESIGN.md`, `SETUP_GUIDE.md` all
    describe the expanded sheet, the big Capture button, and background clipboard
    capture. All of that is going away. Update them rather than leaving docs
    describing an app that no longer exists. `ONE-UI-AUDIT.md` will also go stale.

---

## 12. Testing expectations — be honest with the user

- **None of this can be tested from a Claude Code session.** No device. Code can
  be written and checked for correctness, but "does the bubble actually grab the
  text" only the user can answer.
- **A full rebuild is required, not a JS reload.** All the native pieces change;
  fast refresh will not pick them up. Custom dev client via EAS — Expo Go cannot
  run any of this.
- **Bug 2 may need a logcat from the user** if the defences do not fix it.
- First two things to check on device: the selection surviving the bubble tap,
  and how selection reading behaves across the user's most-used apps.

### CI — what runs on every PR

`.github/workflows/ci.yml` has two jobs. **Run both locally before pushing**;
they are quick and a red PR costs a cycle.

| Job | Command | Gate? |
| --- | --- | --- |
| Typecheck | `npm ci` then `npx tsc --noEmit` | **Yes — this one fails the build.** |
| One UI conformance | `python3 .claude/one-ui/scripts/oneui_scan.py src` | No — `continue-on-error: true`. |

Two things to know:

- **`npm ci`, not `npm install`.** It installs the lockfile exactly and fails
  loudly if `package.json` and `package-lock.json` have drifted apart. If a
  dependency changes, commit the updated lockfile.
- **The One UI scan is advisory and cannot fail the build.** It covers the
  mechanically checkable subset of the One UI rules — hardcoded colours,
  off-scale spacing, missing reduced-motion guards. A finding is a prompt to
  look, not a gate.

  That said, **run it anyway on this work.** It will be quiet on the native
  Kotlin (it only scans `src`), but this change touches a lot of the interface —
  mini-list font sizing, numbered rows, notification actions, the tap-to-arm
  paste state, the size slider, the permissions wall — and that is exactly the
  surface it has opinions about. Cheaper to read its output than to have a human
  find the same things.

The repo also has `.claude/skills/one-ui-*` skills covering the areas the scanner
cannot check mechanically (structure, copy, icon metaphors, motion curves). Worth
invoking for the new UI rather than guessing at the conventions this codebase
already follows.

---

## 13. Design principles carried through this work

Recorded so they are not accidentally reversed:

- **Never read the clipboard for capture.** The whole design exists to avoid it.
- **The bubble must never take input focus.** `FLAG_NOT_FOCUSABLE` at
  `OverlayService.kt:216` and `:372` is load-bearing for both paste and capture.
- **Position in fractions, never pixels.** Rotation, split-screen, foldables.
- **Native owns geometry.** Only native knows where the bubble and system bars
  are; JS asks for a shape by name.
- **Sizes relative to the user's accessibility settings**, not absolute — both
  the mini list font and the bubble size floor.
- **Every gesture needs a non-gesture equivalent** (drag-to-hide ↔ the
  notification's Hide button).
- **Say what actually happened.** Silent failure has bitten this codebase
  repeatedly — the empty overlay, the dead Capture button, the dialog that never
  appears. Prefer a visible message over a no-op.
