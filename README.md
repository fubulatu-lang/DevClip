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
│   ├── theme/
│   │   ├── theme.ts             # Colors, spacing, radii, shadows, font names
│   │   └── fonts.ts             # Loads the Manrope font family
│   ├── db/database.ts           # All SQLite queries (JS side)
│   ├── store/clipStore.ts       # Zustand store — UI state + DB calls
│   ├── native/OverlayModule.ts  # JS bridge to the native overlay module
│   ├── utils/clipboardCapture.ts# Phase-1 manual clipboard read/write
│   ├── screens/
│   │   ├── PopupScreen.tsx      # Small/Expanded/Full toggle + header
│   │   └── ClipListView.tsx     # The actual list (search, sort, capture)
│   └── components/
│       ├── Pressy.tsx           # Spring press-scale wrapper for buttons/cards
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

## 6. Visual design

DevClip uses a **"Soft Structuralism"** look: a near-white silver-grey
canvas, airy floating white cards with diffused (never harsh) shadows, and
one restrained indigo accent color — chosen because this is a utility
people glance at dozens of times a day from a small overlay window, so it
needs to read instantly in bright daylight rather than compete for
attention like a marketing site would.

- **Typography** — Manrope (loaded via `@expo-google-fonts/manrope`,
  `src/theme/fonts.ts`), a geometric grotesk with real weight range
  (400–800), instead of the platform default system font.
- **Icons** — `lucide-react-native`, always drawn with `strokeWidth={1.5}`
  or thinner instead of the default heavy 2px stroke, for a lighter,
  precise line quality.
- **Design tokens** — centralized in `src/theme/theme.ts` (colors, spacing
  scale, radii, shadows) so every screen pulls from the same palette
  instead of hardcoding hex values inline.
- **"Double-bezel" cards** — the floating popup (`PopupScreen.tsx`) and the
  edit sheet (`EditClipModal.tsx`) both use a nested outer-shell / inner-core
  structure (a faint outer tint + radius, containing a distinct white card
  with its own smaller radius) rather than sitting flat on the background.
- **Segmented pill tab bar** for Small / Expanded / Full App, with the
  active tab riding on its own white pill + soft shadow, instead of
  underline-style tabs.
- **Motion** — buttons and cards use `src/components/Pressy.tsx`, a spring-
  physics press-scale (compresses to 96% on touch) instead of the default
  instant opacity flash, for a more physical feel.

To change the palette or scale, only `src/theme/theme.ts` needs editing —
every component reads from it rather than hardcoding values.

## 7. Interface & Settings

**Hero bar** (top of every screen, in any of the 3 states): app name, a
**Bubble on/off** pill, the **Small / Expanded / Full App** switcher, and a
gear icon that opens Settings as its own screen — reachable from any state,
not just Full App.

**Tap a clip → real paste, not just copy.** DevClip sets the clipboard
*and* asks the Accessibility Service to perform a paste directly into
whatever text field you were last using in the other app (it finds the
focused field system-wide and calls its native paste action — your overlay
windows are non-focusable, so the field underneath never loses focus). If
no field is focused or it doesn't support paste, DevClip falls back to
"copied — paste manually" so you're never left with nothing.

**Settings screen:**

| Setting | What it does |
|---|---|
| Permissions status | Live green/red for accessibility, overlay, and notifications — so you can tell if the OS silently revoked one later |
| Theme | Light / Dark / Auto (follows system) |
| Background capture | Opens Accessibility settings to enable/manage |
| Notifications | Requests the real system permission dialog |
| Floating bubble | Starts/stops the bubble (asks overlay permission first) |
| Bubble size | Small / Medium / Large — takes effect immediately, even while the bubble is showing |
| Auto-start after reboot | Restarts the bubble automatically after the phone restarts, via a native `BootReceiver` |
| Confirm before paste | Turn off to paste on tap with no dialog |
| Keep at most | Auto-trims oldest clips beyond 100 / 500 / 1000 / unlimited |
| Export backup | Writes a JSON snapshot and opens the OS share sheet — live capture always stays on fixed internal storage (required for the background service to keep writing to it), this is a point-in-time export, not a sync destination |
| Clear all clips | Wipes the database |

First launch shows a one-time **onboarding screen** that requests the
notification permission automatically (the one permission Android lets an
app trigger a real system dialog for) and walks through enabling
background capture and the bubble. Because background capture now also
powers auto-paste, its Accessibility permission screen is broader than a
plain "read clipboard" request — this is expected, not a bug.

**Manual sort** snapshots whatever order you were just looking at (e.g.
Newest first) the moment you switch to Manual, instead of jumping back to
original insertion order.

## 8. Adding cloud sync later (not built yet)

Because storage is isolated behind `src/db/database.ts`, adding sync later
mostly means: add a `SyncService` that reads unsynced rows and pushes them to
a backend, plus a `synced_at` column — the UI layer (store, screens,
components) wouldn't need to change at all.
