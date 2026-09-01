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

## Operating Context

- Used from a floating bubble that can be tapped from inside any other Android app. Tap with text selected captures it; tap with nothing selected opens the list; long press always opens the list.
- The bubble is docked to the left or right edge and remembers where it was left, as an edge plus a fraction — so the position survives rotation, split screen, a foldable opening, and a reboot. Dragging it into a target at the bottom **centre** hides it, which is a gesture that edge-docking makes safe: dragging straight down the rail never reaches the middle.
- Three service states, not two: stopped, running with the bubble visible, and running with the bubble hidden. Hidden is not a preference and does not survive a reboot.
- Two distinct surfaces, not one UI at two sizes. The overlay is its own React root — a list tethered to the bubble, paste-only, at a smaller type scale. The launcher app is the full screen and carries search, editing and settings.
- Layout is driven by window size class, not device model: below 589dp the 24dp keyline is the margin, from 589dp it is 5% of the width, from 960dp 12.5%, and clip cards go to two columns from 589dp.
- Settings surfaces live permission status (text capture, overlay, notifications) since Android can silently revoke these later.
- Setup is a wall the user can walk past, with the app then saying plainly what it cannot do. It comes back if a permission is revoked.

## Capabilities and Constraints

- Capture reads the live text selection from the accessibility node tree, with a cache of the most recent selection-changed event as a fallback. Both paths are built, because the whole design rests on an assumption that cannot be verified without a device: that tapping the bubble does not clear the user's selection.
- The clipboard is written *after* the database, never read for capture. Writing from the background is allowed; reading is not.
- Native writes clips directly to a local SQLite file (`devclip.db`); the JS side reads and writes the *same* file via `expo-sqlite` — no IPC, they agree on file path and schema. A small `DeviceEventEmitter` channel carries "something changed, go and look" one way, native to JS, and is allowed to be dropped when no React instance exists.
- Single flat `clips` table (id, title, content, created_at, sort_order). `sort_order` is unused since manual reordering was removed and is left in place deliberately.
- One order, newest first, app-wide. Rows are numbered by position.
- Duplicate suppression against the top clip only: catches the double tap, allows a deliberate recapture later.
- Password fields are skipped explicitly rather than saved as a row of bullets.
- The clip limit is enforced at capture time as well as in the app, because capture happens with the app closed.
- No cloud sync. Export is a point-in-time JSON snapshot with timestamps via the OS share sheet; import merges and skips anything already stored, so importing twice does nothing the second time.
- Hard platform ceiling: Android's ~1MB Binder transaction buffer, shared across the process, caps what can be read out of another app or put on the clipboard. DevClip's own database has no such limit, so oversized clips are saved in full and the user is told that *Android* truncated, not DevClip.
- Android only — no iOS build planned; the `ios` key in `app.json` is Expo scaffolding, not a target platform.
- `android/` is never committed — regenerated fresh from `app.json` + `plugins/` via `expo prebuild` on every `eas build`, which is what keeps the native pieces safe to hand-edit.

## Brand Commitments

- Name: DevClip. Package: `com.devclip.app`.
- Visual identity is **Samsung One UI, in the app icon's colours**, replacing the former "Soft Structuralism" (near-white canvas, indigo accent, Manrope, diffused shadows). The full system is recorded in `ONE-UI.md` and is the contract: role-based colour derived from the icon's slate `#345065` and blue `#3498DB` (both darkened to meet WCAG AA), the system font, a 17sp body, the 24dp keyline, pill buttons, depth by surface tone rather than shadow, and the real One UI easing curves. Treat `ONE-UI.md` as incumbent; do not reinvent it without the user's direction.
- The accent is pinned rather than derived from the wallpaper. One UI would take it from the system; DevClip fixes it for brand identity and deterministic contrast. Recorded as a deliberate divergence.

## Evidence on Hand

- Full working native + RN implementation already exists (screens, components, store, db, native Android sources).
- No user research, testimonials, or external evidence — single-user personal project.

## Product Principles

- Read instantly in a glance — this is checked dozens of times a day from a small overlay window, often in bright daylight, so legibility beats expressiveness.
- Never steal focus — overlay windows and paste actions must never disrupt the app underneath.
- Never lose a clip — duplicate suppression and safe trimming, but no silent data loss.
- Keep the native/JS boundary thin — one shared SQLite file and schema is the entire contract; don't add hidden coupling.
- Right surface, right depth — the floating list is for pasting and nothing else; the full app carries search, editing and settings. Shared components, not one screen stretched to two sizes.
- Never read the clipboard to capture. The whole design exists to avoid it, and every route back to it is a dead end that has already been walked.
- Never take input focus. `FLAG_NOT_FOCUSABLE` on the overlay windows is what makes both capture and paste possible.
- Say what actually happened. Silent failure has bitten this codebase repeatedly; prefer a visible message over a no-op.
- No gesture is the only way to do anything — every action reachable by long press has a visible control too.
- Never write a literal — colour, type, spacing, radius, icon size and motion all come from the token layer. CI's One UI scan reports colours that escape it; it is advisory and does not fail the build, so a finding is a prompt to look. The one standing exception is `ErrorBoundary.tsx`, which renders when the theme may be what threw and so cannot read a token.
