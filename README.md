# DevClip

A clipboard manager for Android: a floating bubble you can tap from any app,
showing your clipboard history with search, sort, manual reorder, and inline
editing.

**Start here:** [SETUP_GUIDE.md](./SETUP_GUIDE.md) — step-by-step, phone +
GitHub + cloud only, no computer required.

## 1. System architecture

```
┌─────────────────────────────┐        ┌───────────────────────────────┐
│  ClipboardAccessibilityService│  ---->│         devclip.db            │
│  (native Kotlin, always on   │ writes │   (SQLite file, shared by     │
│   once user enables it)      │        │    native + JS)                │
└─────────────────────────────┘        └───────────────┬───────────────┘
                                                          │ reads/writes
                                                          ▼
┌─────────────────────────────┐        ┌───────────────────────────────┐
│  OverlayService (native)     │  hosts │   PopupScreen (React Native)  │
│  - draggable bubble           │◄──────│   - ClipListView               │
│  - WindowManager popup window │        │   - search / sort / reorder   │
└─────────────────────────────┘        │   - edit / delete modal        │
                                         └───────────────────────────────┘
```

- The **Accessibility Service** is a small, always-running native component
  that Android grants clipboard-read access to even in the background (a
  privilege normal apps lost on Android 10+). It writes new clips straight
  into a local SQLite file — it does not need the JS/React Native side to be
  running at all.
- The **JS app** (this is 95% of the code you'll actually edit) opens that
  *same* SQLite file via `expo-sqlite` to display, search, sort, edit, and
  delete clips. Because both sides agree on the exact file path and table
  schema, they always see the same data without needing to talk to each
  other directly.
- The **Overlay Service** draws the draggable bubble and, when tapped, adds
  a second small window that mounts your actual React Native `PopupScreen`
  component — so the "native" popup is really just your RN UI, sized
  smaller and drawn in a floating window instead of a normal full screen.
- **Full App mode** simply launches the normal `MainActivity` (a regular,
  full-screen Activity) instead of resizing the floating window.

This means you only write your list/search/sort/edit UI **once**
(`src/screens`, `src/components`) — it's reused for the small bubble popup,
the expanded popup, and the full app screen.

## 2. File structure

```
DevClip/
├── App.tsx                     # Root component for the full-screen app
├── index.ts                    # Registers BOTH the full app and the
│                                #   floating-popup entry points
├── app.json                    # Expo config: package name, plugin list
├── eas.json                    # Cloud build profiles (dev / preview / prod)
├── src/
│   ├── types/clip.ts            # Clip, SortMode, PopupState types
│   ├── db/database.ts           # All SQLite queries (JS side)
│   ├── store/clipStore.ts       # Zustand store — UI state + DB calls
│   ├── native/OverlayModule.ts  # JS bridge to the native overlay module
│   ├── utils/clipboardCapture.ts# Phase-1 manual clipboard read/write
│   ├── screens/
│   │   ├── PopupScreen.tsx      # Small/Expanded/Full toggle + header
│   │   └── ClipListView.tsx     # The actual list (search, sort, capture)
│   └── components/
│       ├── SearchBar.tsx
│       ├── SortMenu.tsx
│       ├── ClipListItem.tsx     # Tap-to-copy, long-press, reorder arrows
│       ├── EditClipModal.tsx    # Edit title/content, delete
│       └── SettingsPanel.tsx    # Enable background capture / start bubble
└── plugins/
    ├── withDevClipNative.js     # Config plugin: wires everything below
    │                            #   into the Android project automatically
    └── android-src/
        ├── ClipboardAccessibilityService.kt
        ├── DevClipDatabaseHelper.kt
        ├── OverlayService.kt
        ├── OverlayModule.kt
        ├── OverlayPackage.kt
        └── accessibility_service_config.xml
```

Nothing under `android/` is committed to git — `eas build` regenerates it
fresh from `app.json` + `plugins/` every time, via `expo prebuild`. That's
what makes the native pieces safe to keep editing without ever touching a
generated folder by hand.

## 3. Database schema

Single table, `clips`, in a SQLite file at `<app files dir>/SQLite/devclip.db`
(the exact path `expo-sqlite` uses, matched byte-for-byte by the native
`DevClipDatabaseHelper.kt`):

| column      | type    | notes                                   |
|-------------|---------|------------------------------------------|
| id          | INTEGER | primary key, autoincrement               |
| title       | TEXT    | nullable — user-set label                |
| content     | TEXT    | the copied text                          |
| created_at  | INTEGER | unix ms timestamp                        |
| sort_order  | INTEGER | used only when sort mode = "Manual"      |

This is intentionally a single flat table for the MVP. Adding cloud sync
later (see below) would mean adding a `synced_at` / `remote_id` column and a
sync service — it would not require restructuring this table.

## 4. UI architecture & state management

- **Zustand** (`src/store/clipStore.ts`) holds the current clip list, search
  text, and sort mode, and wraps every database operation. Components never
  talk to `db/database.ts` directly — they call store actions
  (`addClip`, `updateClip`, `deleteClip`, `moveUp`, `moveDown`).
- **No navigation library** is used — `PopupScreen` just conditionally
  renders based on a `small | expanded | full` state variable, since the
  three states are really "the same screen, different container size,"
  not different pages.
- **expo-router** is installed for future growth (e.g. if you later add a
  settings page, onboarding flow, etc.) but isn't required by the current
  three-state UI.

## 5. Design decisions worth knowing about

- **New Architecture (Fabric) is disabled** (`newArchEnabled: false` in
  `app.json`). Rendering React Native content inside a raw Android overlay
  window (`OverlayService.kt`) uses the classic `ReactRootView` API, which
  is dramatically simpler under the old architecture. This can be revisited
  later once your bubble UI is stable.
- **Accessibility Service, not a background poller.** Android 10+ blocks
  ordinary background clipboard reads outright; a `setInterval`-style poller
  from a foreground service would simply receive empty results while your
  app isn't in the foreground. The Accessibility Service is the standard,
  documented way real clipboard-manager apps (Clipper, ClipStack, etc.) get
  around this.
- **Duplicate suppression**: the native side skips inserting a clip if it's
  identical to the most recently saved one, so re-copying the same text
  repeatedly doesn't spam the list.

## 6. Adding cloud sync later (not built yet)

Because storage is isolated behind `src/db/database.ts`, adding sync later
mostly means: add a `SyncService` that reads unsynced rows and pushes them to
a backend, plus a `synced_at` column — the UI layer (store, screens,
components) wouldn't need to change at all.
