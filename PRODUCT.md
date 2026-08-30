# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

A single primary user: the developer/owner, using DevClip on their own phone. Not built for distribution to other users at this stage. They're someone who copies and pastes constantly throughout the day and wants clipboard history available instantly from any app, without switching context.

## Product Purpose

DevClip is a clipboard manager for Android: a floating bubble, tappable from any app, that shows clipboard history with search, sort, manual reorder, and inline editing. Tapping a clip doesn't just copy it — it performs a real paste directly into whatever text field was last focused in the app underneath, via an Accessibility Service, with a "copied — paste manually" fallback when no field is focused.

## Positioning

Most clipboard managers on Android either require the app to be in the foreground or only support copy, not paste. DevClip's mechanism — an always-on Accessibility Service (not a background poller, which Android 10+ blocks) writing straight into a shared SQLite file, paired with a non-focusable overlay window that can trigger a system-wide paste into the last-focused field — lets it read and act on the clipboard from the background and paste without stealing focus from the app underneath.

## Operating Context

- Used from a floating bubble overlay that can be tapped from inside any other Android app.
- Two distinct surfaces, not one UI at three sizes. The overlay is its own React root: **mini**, tethered to the bubble and paste-only, and **expanded**, a half-height sheet across the bottom with search, sort and editing. The launcher app is the full screen and carries no size controls.
- Layout is driven by window size class, not device model: below 589dp the 24dp keyline is the margin, from 589dp it is 5% of the width, from 960dp 12.5%, and clip cards go to two columns from 589dp.
- Settings screen surfaces live permission status (accessibility, overlay, notifications) since Android can silently revoke these later.
- Onboarding is a one-time first-launch flow: requests notification permission, walks through enabling background capture and the bubble.

## Capabilities and Constraints

- Native Kotlin `ClipboardAccessibilityService` writes clips directly to a local SQLite file (`devclip.db`); the JS/React Native side reads/writes the *same* file via `expo-sqlite` — no IPC between them, they just agree on file path and schema.
- Single flat `clips` table (id, title, content, created_at, sort_order) — intentional MVP simplicity.
- Duplicate suppression: identical back-to-back copies aren't re-inserted.
- Manual sort snapshots current order at the moment the user switches to Manual mode, rather than reverting to original insertion order.
- New Architecture (Fabric) is deliberately disabled — rendering RN inside a raw Android overlay window uses the simpler classic `ReactRootView` API.
- No cloud sync yet. Export is a point-in-time JSON snapshot via the OS share sheet, not a sync destination — live capture always stays on fixed internal storage since the background service depends on that fixed path.
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
- Right surface, right depth — mini is for pasting and nothing else, expanded and full carry search, sort and editing. Shared components, not one screen stretched to three sizes.
- No gesture is the only way to do anything — every action reachable by long press has a visible control too.
- Never write a literal — colour, type, spacing, radius, icon size and motion all come from the token layer; CI fails the build if a colour escapes it.
