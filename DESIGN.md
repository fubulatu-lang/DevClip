# Design

The visual world DevClip is built in. **Token values live in [`ONE-UI.md`](ONE-UI.md)**, and the code that holds them is `app/src/main/java/com/devclip/app/DevClipTheme.kt`. They are not repeated here: one source of truth, or the two drift. This file says what the world *is* and why.

## World

**Samsung One UI, in monochrome.**

Not a One UI pastiche applied to a generic app: the structural ideas do the work. The viewing area is what you read; the interaction area is what you touch, and it sits within thumb reach. Pull down at the top of a screen and the title grows into the space above while the controls come down to meet the thumb. Depth is a tone shift and a 12dp radius, never a blurred shadow. Type is deliberately large (17sp body) because this is checked dozens of times a day from a small window, often in daylight, and legibility beats expression.

Black on white, white on black. The light page is true white and cards step *down* to a near-white grey; the dark page is true black — what One UI does, and what an OLED panel wants — and cards step *up* from it. There is no accent. "Selected" and "primary" are ink at full strength against a surface that has stepped back, and must also differ in fill, weight or position, because a monochrome system has no hue to lean on and colour alone never carries state.

The three functional colours — success, caution, error — are the only hues. One UI requires them to be distinguishable, so they survive the monochrome decision. They are never decorative.

**This replaced two earlier worlds.** First "Soft Structuralism" (near-white canvas, indigo accent, Manrope, diffused shadows), then One UI in the app icon's slate and blue. Both are anti-references now: an indigo or a Samsung blue on a button, a large blurred shadow, or small type are evidence of a world that was left, not options.

## Mode

**Operate.** The visitor completes a task: find a clip, paste it, get out. Scanability, consistency and native expectations outrank expression. Brand lives in precise details — the pill on the one primary action, the icon on the bubble, the easing on a settle — not in decoration.

Two moments are *read* rather than operated: the setup wall, the only place `DISPLAY` (34sp) appears in a page; and the app bar pulled fully open, where the title grows to `HERO` (56sp) because it is the only thing in an area the user deliberately opened.

## Surfaces

| Surface | Built in | Shape | Depth of function |
|---|---|---|---|
| **Floating list** | Views, `PopupListView` in a window `OverlayService` owns | Tethered to the bubble, 320×460dp by default, resizable from any edge (220–560 × 160–900dp) | **Paste only.** No search, no editing, no long press — a gesture is never the only route to anything |
| **Full app** | Compose, `MainActivity` | Whole screen from the launcher | Search, editing, settings, backup |
| **Bubble, edge handle, hide target** | Views and drawables in `OverlayService` | Floating over whatever app is open | Capture, open the list, hide |

They are not one screen at two sizes. The floating list is views on purpose: it draws into a window a service owns, where Compose needs lifecycle owners wired by hand and fails by rendering nothing. It runs its type 15% below the full app's roles — proportional to the user's font scale, not a fixed small number, with 12sp as the floor — so someone running large text still gets large text there, just more compact.

## Adaptivity

Layout is driven by **window size class**, never a device model, so a phone in landscape, a tablet in split view, a folding phone opening, and the small overlay window each get the shape that fits the width they have. `rememberWindowLayout()` in `ui/Theme.kt` is the one place that decides:

- **Side margin** follows One UI's `AdaptiveCoordinatorLayout`: the 24dp keyline below 589dp, 5% of the width from 589dp, 12.5% from 960dp — the percentages only when the window is at least 412dp tall, so a phone on its side stays on the keyline. Never less than 24dp.
- **Clip columns**: two from 589dp wide, whatever the height.
- **The pull-down app bar** opens to 44% of the window height, between 300 and 460dp, but never past half a short window.

## Floating over other apps

The bubble, its selection ring, the edge handle and the drag-to-hide target sit on a background DevClip does not control, so light and dark cannot govern them. They pair a white with a black instead — a dark stroke outside a white one — so that whichever the app underneath swallows, the other survives. Those fixed colours are named in `DevClipTheme.Overlay` rather than written where they are drawn. The hide target turns the error colour when the bubble is over it, and grows, so the colour is never the only change.

## Accessibility is part of the design

- Every surface is named and every action labelled for TalkBack, including the bubble, which has no visible text of its own.
- A labelled control is one focus stop: a switch row toggles as a whole; a slider says its label and its value in the units on screen; choice chips are a radio group that says which is selected.
- Section titles in Settings are headings.
- Messages that report what happened are live regions, so they are heard as well as seen.
- Anything holding a number that grows with the font — the row badges — has a minimum size, never a fixed one.
- Every animation checks the system's animator scale and holds still when it is off.

## What this world refuses

- **Colour literals.** Nothing outside `DevClipTheme.kt`, apart from `res/values/colors.xml`, which paints the window before any Kotlin runs and must agree with it. CI's One UI scan reports a stray value; it is advisory, so a finding is a prompt to look rather than a gate.
- **An accent.** Not the icon's blue, not a wallpaper colour. Emphasis is weight, fill and position.
- **Hand-picked sizes.** Type, spacing, radius and icon size come from roles, not per-screen judgement.
- **Shadow as depth.** Cards carry none. Only the bubble, which genuinely floats, has elevation, and little of it.
- **Uppercase micro-labels**, letter-spaced eyebrows, and any type below 12sp.
- **Dialogs for news.** A snack reports what happened; a dialog is for a decision that cannot be taken back, and "Delete all clips" is the only one. No dialog at all in the floating window: an Android dialog needs a foreground Activity, which that window has none of by design, so one there would never appear.
- **Gesture-only functions.** Long press on a clip has the pencil. Drag-to-hide has the notification's Hide action and the Status row in Settings. The bubble's tap and hold are accessibility actions. The sliders have increment and decrement.
- **Silence as an outcome.** The empty overlay, the dead Capture button, the dialog that never appeared: every one looked identical to a tap that was never registered. Something visible — and audible to TalkBack — always says what happened.

## Deliberate divergences

- **No accent.** One UI takes one from the wallpaper; DevClip has none.
- **The tertiary grey is for disabled only.** One UI publishes `#8C8C8C` as tertiary text; at 3.14:1 on the light surface it fails WCAG AA, so DevClip uses it only for disabled states and the faintest readable text is the published secondary `#505050`.
- **Toasts from the services.** The overlay service has no Activity to hold a snack, and the windows it would draw one in have often just been torn down.

Each is recorded with its reason in `ONE-UI.md`.
