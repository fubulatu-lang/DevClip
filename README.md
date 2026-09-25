# DevClip

Highlight text anywhere on your phone, tap the floating bubble, and it is
saved. No switching apps, no pressing a Capture button, no clipboard involved.

**Start here:** [SETUP_GUIDE.md](./SETUP_GUIDE.md) — step-by-step, phone +
GitHub only, no computer required.

## The one thing to understand

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
- Selections inside a WebView are not in the accessibility tree, so DevClip is
  also an entry in Android's text-selection menu, which hands it the text
  directly.

Three approaches were investigated and rejected before this one: background
clipboard reads (impossible), shipping a keyboard (replacing Gboard), and
Shizuku (dies on every reboot, needs a second app). The reasoning is in
[DECISIONS.md](./DECISIONS.md) so it does not get re-derived.

## How it fits together

```
┌────────────────────────────────┐        ┌──────────────────────────────┐
│ ClipboardAccessibilityService  │        │         devclip.db           │
│  · reads the live selection    │        │  one SQLite file, one table  │
│  · reports where the keyboard  │        └──────────────┬───────────────┘
│    is                          │                       │
│  · pastes into a focused field │                       │ reads / writes
└──────────────┬─────────────────┘                       │
               │ SelectionCapture                        │
               ▼                                         ▼
┌────────────────────────────────┐        ┌──────────────────────────────┐
│ OverlayService (views)         │        │ MainActivity (Compose)       │
│  · the bubble and edge handle  │        │  · the clip list, search     │
│  · the floating list           │        │  · editing                   │
│  · the drag-to-hide target     │        │  · settings, setup, backup   │
│  · the notification            │        └──────────────────────────────┘
└──────────────┬─────────────────┘                       ▲
               │ DevClipEvents                           │
               └─────────────────────────────────────────┘
                 "something changed, go and look"
```

- The **accessibility service** feeds the most recent text selection to
  `SelectionCapture`, reports where the on-screen keyboard is (a non-focusable
  window receives no IME insets, so the bubble cannot measure it itself), and
  performs the paste.
- **`OverlayService`** owns every floating window and the state machine behind
  them, and is where a capture actually happens. It draws in ordinary views,
  not Compose: a service-owned window has no lifecycle owner, and Compose
  without one renders nothing.
- **`MainActivity`** is the launcher app, in Jetpack Compose.
- **`DevClipEvents`** tells an open screen to re-read the database. It carries
  prompts, never data; the database is what is true.

## Where things are

```
app/src/main/java/com/devclip/app/
├── DevClipTheme.kt                  # The design system — every surface reads it
├── MainActivity.kt                  # The launcher app: setup, clips, settings
├── ui/
│   ├── Theme.kt                     # DevClipTheme converted for Compose
│   ├── ClipListScreen.kt            # The clip list, search, delete with Undo
│   ├── EditClipSheet.kt
│   ├── SettingsScreen.kt
│   ├── SetupScreen.kt               # The permissions wall
│   ├── OneUiHeader.kt               # The pull-down app bar
│   ├── Snack.kt
│   └── OnResume.kt
├── OverlayService.kt                # Every floating window
├── PopupListView.kt                 # The floating list
├── ResizableFrame.kt                # Drag-any-edge resizing for it
├── EdgeHandleView.kt                # The tucked-away bubble
├── DismissTargetView.kt             # The drag-to-hide target
├── SelectionRingDrawable.kt         # The ring shown while a selection is live
├── OverlayController.kt             # The app's way of asking the service things
├── ClipboardAccessibilityService.kt
├── SelectionCapture.kt              # Reading the live selection
├── ImeWatcher.kt                    # Where the keyboard is
├── Capture.kt                       # What a bubble tap actually does
├── ProcessTextActivity.kt           # "DevClip" in the text-selection menu
├── DevClipDatabaseHelper.kt         # The clips table
├── ClipRepository.kt                # The app's off-main-thread access to it
├── Backup.kt                        # JSON export and merging import
├── DevClipEvents.kt
├── Prefs.kt                         # Every SharedPreferences key
└── BootReceiver.kt
```

Design: [PRODUCT.md](./PRODUCT.md) (what it is for),
[DESIGN.md](./DESIGN.md) (the visual world),
[ONE-UI.md](./ONE-UI.md) (the tokens and conventions).
Working rules: [AGENTS.md](./AGENTS.md).

## Database

One table, `clips`, at `<app files dir>/SQLite/devclip.db` — the path the
React Native version used, kept so no clip saved before the conversion was
orphaned.

| column      | type    | notes                                              |
|-------------|---------|----------------------------------------------------|
| id          | INTEGER | primary key, autoincrement                         |
| title       | TEXT    | nullable — user-set label                          |
| content     | TEXT    | the captured text                                  |
| created_at  | INTEGER | unix ms timestamp; the only ordering key           |
| sort_order  | INTEGER | **unused** — left in place, see below              |

`sort_order` is a leftover from manual reordering, which is gone. Dropping a
column in SQLite means rebuilding the table, which is not worth putting a
user's history through for a column nobody reads. Inserts still fill it.

## Decisions worth knowing about

- **The bubble must never take input focus.** `FLAG_NOT_FOCUSABLE` on every
  overlay window is load-bearing for both capture and paste.
- **Position in fractions, never pixels.** The bubble's position is an edge
  plus a fraction of the way down, so it survives rotation, split screen, a
  foldable opening, and a reboot. Dragging is the only way to move it.
- **The switch is not the service.** Whether the accessibility permission is
  on and whether the service is actually running come apart routinely on
  Samsung. Settings asks `OverlayController.isCaptureWorking()`.
- **Lists read previews.** The first 400 characters of each clip; paste and
  edit read the whole clip back by id.
- **Every gesture has a non-gesture equivalent.** Drag-to-hide is also the
  notification's Hide action; long-press-to-edit is also the pencil on each
  row; the bubble's tap and hold are named TalkBack actions.
- **Say what actually happened.** Silent failure has bitten this codebase
  repeatedly. Prefer a visible message — one TalkBack reads out — over a no-op.

## Limits that cannot be engineered away

- **~1MB Binder transaction buffer, shared across the process.** Everything
  passing between apps crosses it, which caps both reading a huge selection
  out of another app and putting one on the system clipboard. DevClip's own
  database has no such limit — so a very large clip is saved in full and the
  user is told that *Android* may have handed over less than they highlighted.
- **Android 13+ lets users swipe away foreground-service notifications**, and
  can deny `POST_NOTIFICATIONS` outright. That is why the bubble can always be
  brought back from inside the app, not only from the notification.
- **Selection reading is not perfect app to app.** Standard text fields are
  fine; apps that draw their own text may give nothing. This is why the
  capture confirmation shows the first few words of what was saved.
