# DevClip

Highlight text anywhere on your phone, tap the floating bubble, and it is
saved. No switching apps, no pressing a Capture button, no clipboard involved.

**Start here:** [SETUP_GUIDE.md](./SETUP_GUIDE.md) — step-by-step, phone +
GitHub + cloud only, no computer required.

## 1. The one thing to understand

**DevClip never reads the clipboard to capture.** It reads the text you have
*highlighted*, straight out of Android's accessibility node tree.

This is not a preference. Since Android 10, `ClipboardManager.getPrimaryClip()`
returns null unless the calling app has window focus or is the default
keyboard, and an accessibility service gets no exemption from that. DevClip's
overlay windows are deliberately non-focusable — that is what lets them paste
into the app underneath without disturbing it — so DevClip is never "in front"
and never will be.

Everything else follows from that:

- Tapping the bubble with text selected **captures the selection**, saves it,
  and *then* puts it on the system clipboard, so the bubble is a full
  replacement for Android's own Copy button.
- Tapping the bubble with nothing selected **opens the list** instead.
- Long-pressing the bubble **always** opens the list.

Three approaches were investigated and rejected before this one: background
clipboard reads (impossible), shipping a keyboard (months of work to replace
Gboard), and Shizuku (dies on every reboot, needs a second app). The reasoning
is preserved in [BUILD-PLAN.md](./BUILD-PLAN.md) §6 so it does not get
re-derived.

## 2. System architecture

```
┌────────────────────────────────┐        ┌──────────────────────────────┐
│ ClipboardAccessibilityService  │        │         devclip.db           │
│  · reads the live selection    │        │  SQLite file, opened by both │
│  · reports where the keyboard  │        │  native and JS at the same   │
│    is                          │        │  path, same schema           │
│  · pastes into a focused field │        └──────────────┬───────────────┘
└──────────────┬─────────────────┘                       │
               │ SelectionCapture                        │ reads / writes
               ▼                                         ▼
┌────────────────────────────────┐        ┌──────────────────────────────┐
│ OverlayService                 │        │  React Native, two roots     │
│  · the bubble (edge-docked)    │ hosts  │   · OverlayApp — the         │
│  · the floating list's window  │───────▶│     floating list            │
│  · the drag-to-hide target     │        │   · App — the full screen    │
│  · the notification            │        └──────────────────────────────┘
└────────────────────────────────┘                       ▲
               │ DevClipEvents                           │
               └─────────────────────────────────────────┘
                 "something changed, go and look"
```

- The **accessibility service** does three jobs: it feeds the most recent text
  selection to `SelectionCapture`, it reports where the on-screen keyboard is
  (a non-focusable overlay window receives no IME insets, so the bubble cannot
  measure the keyboard itself), and it performs the paste.
- **`OverlayService`** owns every floating window and the state machine behind
  them. It is also where a capture actually happens, on a bubble tap.
- The **JS app** is two React roots in one process — the full-screen app and
  the floating list. They share the zustand stores and the database and
  nothing else.
- **`DevClipEvents`** is the only channel from native into JS. It carries
  prompts, never data: native can emit when there is no React instance in the
  process at all (after a reboot, or once Android has trimmed everything but
  the service), so JS re-reads the database rather than trusting an event to
  have arrived.

Nothing under `android/` is committed — `eas build` regenerates it from
`app.json` + `plugins/` on every build via `expo prebuild`, which is what
makes the native sources safe to edit by hand.

## 3. File structure

```
DevClip/
├── App.tsx                       # Root of the full-screen app
├── index.ts                      # Registers BOTH roots: App and DevClipPopup
├── app.json                      # Expo config: package name, plugin list
├── eas.json                      # Cloud build profiles
├── src/
│   ├── types/clip.ts
│   ├── theme/
│   │   ├── theme.ts              # Colour, spacing, radii, type, easing tokens
│   │   ├── ThemeContext.tsx
│   │   ├── useAdaptiveLayout.ts  # Window-size-class layout, not device model
│   │   └── useReduceMotion.ts
│   ├── db/database.ts            # Every SQLite query on the JS side
│   ├── store/
│   │   ├── clipStore.ts          # The clip list and every write to it
│   │   ├── settingsStore.ts      # Persisted settings, mirrored into native
│   │   ├── editStore.ts          # Which clip the edit sheet is showing
│   │   ├── pasteArmStore.ts      # Which row is one tap from pasting
│   │   └── snackbarStore.ts
│   ├── hooks/
│   │   ├── useClipSync.ts        # Keeps a list showing what is in the database
│   │   └── usePermissions.ts     # The three permissions, re-checked on resume
│   ├── native/
│   │   ├── OverlayModule.ts      # JS side of the native module
│   │   └── events.ts             # JS side of the native event channel
│   ├── utils/
│   │   ├── clipboardCapture.ts   # Manual clipboard read, and paste
│   │   └── backup.ts             # Export and merging import
│   ├── screens/
│   │   ├── PopupScreen.tsx       # The full app: gate, app bar, list, sheet
│   │   ├── ClipListView.tsx      # The full list: search, capture button
│   │   ├── OverlayScreen.tsx     # The floating list
│   │   ├── SettingsScreen.tsx
│   │   └── SetupScreen.tsx       # The permissions wall
│   └── components/
│       ├── Pressy.tsx            # Press-scale wrapper, reduce-motion aware
│       ├── SearchBar.tsx
│       ├── Slider.tsx            # Bubble size
│       ├── ClipListItem.tsx      # Numbered row, tap-to-arm paste
│       ├── EditClipSheet.tsx     # Inline sheet, NOT a Modal — see §5
│       ├── PermissionBanner.tsx  # What DevClip currently cannot do
│       ├── ErrorBoundary.tsx
│       └── Snackbar.tsx
└── plugins/
    ├── withDevClipNative.js      # Copies the Kotlin in, writes the manifest
    │                             #   and every native-facing string resource
    └── android-src/
        ├── BootReceiver.kt
        ├── Capture.kt                     # What a bubble tap actually does
        ├── ClipboardAccessibilityService.kt
        ├── DevClipDatabaseHelper.kt       # Native side of the shared db
        ├── DevClipEvents.kt               # Native → JS channel
        ├── DismissTargetView.kt           # The drag-to-hide target
        ├── ImeWatcher.kt                  # Where the keyboard is
        ├── OverlayModule.kt               # Native module + the Prefs contract
        ├── OverlayPackage.kt
        ├── OverlayService.kt              # Every floating window
        ├── SelectionCapture.kt            # Reading the live selection
        └── accessibility_service_config.xml
```

## 4. Database schema

One table, `clips`, at `<app files dir>/SQLite/devclip.db` — the exact path
`expo-sqlite` uses, matched by `DevClipDatabaseHelper.kt`. **If you change the
schema, change both sides.**

| column      | type    | notes                                              |
|-------------|---------|----------------------------------------------------|
| id          | INTEGER | primary key, autoincrement                         |
| title       | TEXT    | nullable — user-set label                          |
| content     | TEXT    | the captured text                                  |
| created_at  | INTEGER | unix ms timestamp; the only ordering key           |
| sort_order  | INTEGER | **unused** — left in place, see below              |

`sort_order` is a leftover from manual reordering, which is gone. Dropping a
column in SQLite means rebuilding the table, which is not worth putting a
user's history through for a column nobody reads. Inserts still fill it so
both sides keep agreeing on the schema.

## 5. Decisions worth knowing about

- **The bubble must never take input focus.** `FLAG_NOT_FOCUSABLE` on every
  overlay window is load-bearing for both capture and paste. Anything that
  would take focus breaks both.
- **Position in fractions, never pixels.** The bubble's position is an edge
  (left/right) plus a fraction of the way down, in SharedPreferences — so it
  survives rotation, split screen, a foldable opening, and a reboot, and is
  available to `OverlayService` at startup when there is no React context to
  ask. There is deliberately no setting for it: dragging is the only way.
- **Native owns geometry.** Only native knows where the bubble and the system
  bars are. JS lays itself out to whatever window it is given.
- **The edit sheet is not a `<Modal>`.** A Modal renders into its own Android
  window, and that window does not inherit the activity's `adjustResize` — so
  the keyboard covered it, and a `KeyboardAvoidingView` could not fix it
  because nothing was wrong with the measurement. It is an ordinary view in
  the main window, with the back button and z-order handled explicitly.
- **Sizes relative to the user's settings, not absolute.** The floating list's
  type is 85% of the full app's *token*, which React Native then multiplies by
  the system font scale; the bubble's floor is 48dp because that is Android's
  comfortable touch target. Nothing passes `allowFontScaling={false}`.
- **Every gesture has a non-gesture equivalent.** Drag-to-hide is also the
  notification's Hide button and a switch in Settings; long-press-to-edit is
  also the more button on each row.
- **Say what actually happened.** Silent failure has bitten this codebase
  repeatedly — the empty overlay, the dead Capture button, the dialog that
  never appeared. Prefer a visible message over a no-op.

## 6. Interface

**The bubble.** Docked to the left or right edge, dragged up and down it, and
snapping back to the nearer edge if dropped in the middle. It wears a ring
when a selection is live, so tap-to-capture is advertised rather than hidden.
Dragging it into the circle at the bottom **centre** hides it — the target is
centred rather than a full-width strip precisely so that dragging down the
rail to park it low never triggers it. Hidden is not off: the service keeps
running, and the notification, or Settings, brings the bubble back. A reboot
brings it back too, because hiding means "get out of my way now", not a
preference.

**The lists.** Newest first, everywhere, with rows numbered from 1. The number
is positional, not an identity — it renumbers as clips arrive and are deleted,
which is what makes it useful. Rows show a preview; the full text lives in the
database and comes out whole on paste.

**Pasting.** Tap once to arm, tap again to paste. DevClip sets the clipboard
*and* asks the accessibility service to paste directly into whatever field was
last focused in the app underneath, falling back to "it's on your clipboard"
when there is no field to paste into. The arm step is what
`confirmBeforePaste` controls; it replaced an `Alert.alert` that could not
work in a floating window at all, because an Android dialog needs a foreground
Activity and the floating window has none by design.

**Settings.**

| Setting | What it does |
|---|---|
| Permissions status | Live status for text capture, the bubble, and notifications — Android can revoke these silently |
| Theme | Light / Dark / Auto |
| Text capture | Opens the Accessibility settings screen |
| Notifications | Requests the system permission dialog |
| Floating bubble | Starts and stops the service |
| Show the bubble | Hides or brings back the bubble while the service keeps running |
| Bubble size | A 48–72dp slider, applied live |
| Auto-start after reboot | Restarts the bubble after the phone restarts |
| Confirm before paste | Off pastes on a single tap |
| Keep at most | Trims beyond 100 / 500 / 1000 / unlimited, enforced at capture time as well as in the app |
| Export backup | A JSON snapshot with timestamps, via the OS share sheet |
| Import backup | Merges, skipping anything already stored — importing twice does nothing the second time |
| Clear all clips | Wipes the database |

**Setup** is a wall you can walk past. Two of the three permissions cannot be
a dialog — Android sends you into system Settings — so refusing entry until
they are granted is not something an app gets to do. A banner in the app then
says what DevClip cannot currently do. If a permission is revoked later the
wall comes back, and can be walked past again.

## 7. Limits that cannot be engineered away

- **~1MB Binder transaction buffer, shared across the process.** Everything
  passing between apps crosses it, which caps both reading a huge selection
  out of another app and putting one on the system clipboard. A few hundred
  thousand characters is comfortable; approaching a million is unreliable in a
  load-dependent way. For scale, a very long web article is ~50,000
  characters. **DevClip's own database has no such limit** — so a very large
  clip is saved in full and the user is told that *Android* may have handed
  over less than they highlighted.
- **Android 13+ lets users swipe away foreground-service notifications** while
  the service keeps running, and can deny `POST_NOTIFICATIONS` outright. That
  is why bringing a hidden bubble back is a first-class control in Settings
  and not merely a notification action.
- **Selection reading is not perfect app to app.** Standard text fields and
  web pages are fine. Apps that draw their own text — some games,
  canvas-based apps — may give nothing. This is why the capture confirmation
  shows the first few words of what was saved.

## 8. Adding cloud sync later (not built)

Storage is isolated behind `src/db/database.ts`, so sync would mean a service
that reads unsynced rows and a `synced_at` column. The UI layer would not
change.
