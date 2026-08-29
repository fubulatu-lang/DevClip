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
- Three interchangeable UI sizes — Small (bubble popup), Expanded, Full App — all rendering the same React Native screen/components, just resized.
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
- Existing visual identity ("Soft Structuralism": near-white silver-grey canvas, airy white cards with diffused shadows, one indigo accent; Manrope typeface; `lucide-react-native` icons at thin stroke width; "double-bezel" nested-card structure; segmented pill tab bar; spring press-scale motion via `Pressy.tsx`) is already implemented and documented in README.md — treat as incumbent, not to be reinvented without the user's direction.

## Evidence on Hand

- Full working native + RN implementation already exists (screens, components, store, db, native Android sources).
- No user research, testimonials, or external evidence — single-user personal project.

## Product Principles

- Read instantly in a glance — this is checked dozens of times a day from a small overlay window, often in bright daylight, so legibility beats expressiveness.
- Never steal focus — overlay windows and paste actions must never disrupt the app underneath.
- Never lose a clip — duplicate suppression and safe trimming, but no silent data loss.
- Keep the native/JS boundary thin — one shared SQLite file and schema is the entire contract; don't add hidden coupling.
- Build once, render everywhere — the same list/search/sort/edit UI serves the bubble popup, expanded popup, and full app.
