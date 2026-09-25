# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

A single primary user: the developer/owner, using DevClip on their own phone. Not built for distribution to other users at this stage. They're someone who copies and pastes constantly throughout the day and wants clipboard history available instantly from any app, without switching context.

## Product Purpose

DevClip saves the text you have highlighted, anywhere on the phone, when you tap its floating bubble. No switching apps, no pressing a Capture button. It keeps that history in a list you can reach from any app, and tapping a clip performs a real paste directly into whatever text field was last focused in the app underneath, with a "it's on your clipboard" fallback when there is no field to paste into.

## Positioning

Most clipboard managers on Android either require the app to be in the foreground or only support copy, not paste. Both limits come from the same place: since Android 10 an app without window focus gets null from the clipboard, and an accessibility service is granted no exemption. DevClip sidesteps the clipboard entirely for capture — it reads the live text selection out of the accessibility node tree — and uses the clipboard only on the way out, where writing from the background *is* allowed. The same non-focusable overlay window that makes this possible is what lets a paste go into the last-focused field without stealing focus from the app underneath.

Three alternatives were investigated in depth and rejected: background clipboard reads (blocked at the framework level, and the widely-repeated `appops` workaround is a no-op), shipping an IME (the default-keyboard exemption is real and package-wide, but there is one slot and taking it means replacing Gboard), and Shizuku (works, but dies on every reboot for non-root users and needs a second app installed). The reasoning is preserved in `BUILD-PLAN.md` §6.

A selection inside a WebView — Chrome, and every app that renders its text in one — is not in the accessibility tree at all. For those, DevClip is also an entry in Android's own text-selection menu (`ProcessTextActivity`), which the system hands the text to directly.

## Operating Context

- Used from a floating bubble that can be tapped from inside any other Android app. Tap with text selected captures it; tap with nothing selected opens the list; long press always opens the list. With TalkBack on, the bubble is named and both of those are labelled actions.
- The bubble is docked to the left or right edge and remembers where it was left, as an edge plus a fraction — so the position survives rotation, split screen, a foldable opening, and a reboot. Dragging it into a target at the bottom **centre** hides it, which is a gesture that edge-docking makes safe: dragging straight down the rail never reaches the middle.
- Left alone, the bubble can tuck itself into a slim handle on the screen edge. The handle thickens and breathes when a selection goes live, so capture is still one touch away.
- Three service states, not two: stopped, running with the bubble visible, and running with the bubble hidden. Hidden is not a preference and does not survive a reboot.
- Two distinct surfaces, not one UI at two sizes. The **floating list** is ordinary Android views drawn into a window the overlay service owns — tethered to the bubble, paste-only, at a smaller type scale, resizable from any edge. The **launcher app** is Jetpack Compose, full screen, and carries search, editing, settings and backup.
- The floating list is views and not Compose, and the whole app is native and not React Native, for one reason: a surface that needs a host (a React instance, a Compose lifecycle owner) draws nothing until something starts that host, and after a reboot nothing had. The list came up empty until the app was opened once. Views draw when they are added.
- Layout is driven by window size class, not device model: below 589dp the 24dp keyline is the margin; from 589dp it is 5% of the width, from 960dp 12.5%, both only when the window is at least 412dp tall. Clip cards go to two columns from 589dp wide.
- Settings surfaces live status — whether text capture is actually running, not just switched on; the overlay and notification permissions; battery restriction — because Android can silently revoke or suspend any of these later, and re-reads it every time the screen comes back to the front.
- Setup is a wall the user can walk past, with the app then saying plainly what it cannot do. It is shown once after install; a permission revoked later shows up under Status in Settings.

## Capabilities and Constraints

- Capture reads the live text selection from the accessibility node tree, with a cache of the most recent selection-changed event as a fallback. Both paths are built, because the whole design rests on an assumption that cannot be verified without a device: that tapping the bubble does not clear the user's selection.
- The clipboard is written *after* the database, never read for capture. Writing from the background is allowed; reading is not.
- One SQLite file, `<filesDir>/SQLite/devclip.db` — the path expo-sqlite used, kept so no clip saved before the conversion is orphaned. The services write to it from capture; the launcher app reads and writes it through `ClipRepository`, off the main thread. The floating list reads it on the main thread deliberately: a bounded read in answer to a tap, with a finger waiting.
- `DevClipEvents` carries "something changed, go and look" from the services to any screen that is open. It is allowed to be dropped; the database is what is true.
- Single flat `clips` table (id, title, content, created_at, sort_order). `sort_order` is unused since manual reordering was removed and is left in place deliberately.
- One order, newest first, app-wide. Rows are numbered by position.
- Lists read previews — the first 400 characters of each clip — and a paste or an edit reads the whole clip back by id. A single clip can be most of a megabyte; the lists show three lines of it.
- Duplicate suppression against the top clip only: catches the double tap, allows a deliberate recapture later.
- Password fields are skipped explicitly rather than saved as a row of bullets.
- The clip limit is enforced at capture time as well as in the app, because capture happens with the app closed.
- Deleting one clip is immediate and offers Undo, which restores it with its original id and timestamp. Deleting all clips asks first, because nothing can take it back.
- No cloud sync. Export is a point-in-time JSON snapshot with timestamps via the system file picker; import merges and skips anything already stored, so importing twice does nothing the second time.
- Hard platform ceiling: Android's ~1MB Binder transaction buffer, shared across the process, caps what can be read out of another app or put on the clipboard. DevClip's own database has no such limit, so oversized clips are saved in full and the user is told that *Android* truncated, not DevClip.
- Android only. The Android project is the repository: `app/` is committed and built directly by Gradle. There is no Expo, no prebuild and no iOS target.
- Every change that ships bumps `versionName` and `versionCode` in `app/build.gradle.kts`, in the same commit. The release APK is named from `versionName`, and Settings shows the installed version at the bottom, so the two can be compared.

## Brand Commitments

- Name: DevClip. Package: `com.devclip.app`.
- Visual identity is **Samsung One UI, in monochrome**: black on white, white on black, no accent. The system is recorded in `ONE-UI.md` and implemented in `DevClipTheme.kt`: the system font, a 17sp body, the 24dp keyline, pill buttons, depth by surface tone rather than shadow, and the real One UI easing curves. Treat both as incumbent; do not reinvent them without the user's direction.
- The only hues are the three functional colours — success, caution, error — because One UI requires them to be distinguishable. They mark status and destruction and are never decorative, and colour is never the only thing carrying their meaning.
- The app icon (slate `#345065`, blue `#3498DB`) is where the colour lives. It is not a palette for the interface. A clipboard manager floats over other people's apps all day and has no business competing with them for attention.
- This is a deliberate divergence: One UI would take an accent from the wallpaper. DevClip has none, so there is nothing to derive and contrast is deterministic.

## Evidence on Hand

- Full working native implementation: Compose screens, view-based overlay, accessibility service, SQLite store, backup.
- No user research, testimonials, or external evidence — single-user personal project.
- Nothing has been verified by automated UI tests or screenshots; the only device in the loop is the owner's Samsung phone.

## Product Principles

- Read instantly in a glance — this is checked dozens of times a day from a small overlay window, often in bright daylight, so legibility beats expressiveness.
- Never steal focus — overlay windows and paste actions must never disrupt the app underneath.
- Never lose a clip — duplicate suppression and safe trimming, Undo after a delete, a question before deleting everything, and no silent data loss.
- Keep the database the contract — the services and the app share one SQLite file and schema, and an event is only ever a prompt to re-read it. Don't add hidden coupling.
- Right surface, right depth — the floating list is for pasting and nothing else; the full app carries search, editing and settings. Shared tokens, not one screen stretched to two sizes.
- Never read the clipboard to capture. The whole design exists to avoid it, and every route back to it is a dead end that has already been walked.
- Never take input focus. `FLAG_NOT_FOCUSABLE` on the overlay windows is what makes both capture and paste possible.
- Say what actually happened. Silent failure has bitten this codebase repeatedly; prefer a visible message over a no-op, and make it a message TalkBack reads out too.
- No gesture is the only way to do anything — every action reachable by long press or drag has a visible control too, and a labelled accessibility action.
- The permission switch is not the service running. Ask `OverlayController.isCaptureWorking()`, never `isAccessibilityEnabled()`, when saying whether capture works.
- Never write a literal — colour, type, spacing, radius, icon size and motion all come from `DevClipTheme.kt`. The colours drawn over other apps, which no theme can govern, are named there too, in `DevClipTheme.Overlay`. CI's One UI scan reports literals that escape it; it is advisory and does not fail the build, so a finding is a prompt to look.
