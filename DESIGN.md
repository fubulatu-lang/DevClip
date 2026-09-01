# Design

The visual world DevClip is built in. **Token values live in [`ONE-UI.md`](ONE-UI.md)** and are not repeated here — one source of truth, or the two drift. This file says what the world *is* and why; that file says what the numbers are.

## World

**Samsung One UI, in the app icon's colours.**

Not a One UI pastiche applied to a generic app: the structural ideas do the work. The viewing area is what you read, the interaction area is what you touch and it sits within thumb reach. Depth is a tone shift and a 12dp radius, never a blurred shadow. Type is deliberately large — 17sp body, where the app previously ran at 13sp — because this is checked dozens of times a day from a small window, often in daylight, and legibility beats expression.

The palette is derived from the app icon rather than chosen beside it: its slate `#345065` and its blue `#3498DB`. Both are used **darkened**, because taken literally the icon blue is 3.15:1 under white text and would fail AA everywhere it matters.

**This replaced a previous world** — "Soft Structuralism": near-white silver-grey canvas, airy white cards with diffused shadows, one indigo accent, Manrope. That is now an anti-reference, not a fallback. Large blurred shadows, small type, and the indigo are all evidence of the old world, not options.

## Mode

**Operate.** The visitor completes a task: find a clip, paste it, get out. Scanability, consistency and native expectations outrank expression. Brand lives in precise details — the accent on a single primary action, the icon on the bubble, the easing on a press — not in decoration.

The one exception is the setup wall, which is the only screen a user reads rather than operates, and is the only place the 34sp display size appears.

## Surfaces

| Surface | Root | Shape | Depth of function |
|---|---|---|---|
| **Floating list** | `OverlayApp` | Tethered to the bubble, 320×460dp | **Paste only.** No search, no editing, and no long press either — a gesture is never the only route to anything |
| **Full app** | `App` | Whole screen from the launcher | Search, editing, settings, backup |

They are not one screen at two sizes. They are separate React roots with different jobs, and `index.ts` registers them separately.

**A third shape used to exist** — "expanded", a half-height sheet across the bottom carrying search and sort. It is gone. It duplicated the full app in a worse window and owned the hardest geometry in `OverlayService`; cutting it left the tethered list as the only floating surface, which is why that is now sized to be worth opening on its own rather than to be the smaller of two options.

The floating list runs its type 15% below the full app's tokens — *relative* to the user's font scale, not a fixed small number, so someone running large text still gets large text here, proportionally more compact.

## Adaptivity

Layout is driven by **window size class**, never a device model, so a phone in landscape, a tablet in split view, a folding phone opening, and the small overlay window each get the shape that fits the width they have. The rule is One UI's own, from `AdaptiveCoordinatorLayout` — see `ONE-UI.md` for the thresholds.

## What this world refuses

- **Colour literals.** Nothing outside `src/theme/theme.ts`. CI's One UI scan reports a stray hex; it is advisory, so a finding is a prompt to look rather than a gate. One file is a standing exception: `ErrorBoundary.tsx` renders when the theme may be the thing that threw, so it cannot read a token.
- **Hand-picked sizes.** Type, spacing, radius and icon size come from roles, not per-screen judgement.
- **Shadow as depth.** Cards carry none; only genuinely floating surfaces get elevation, and little of it.
- **Uppercase micro-labels**, letter-spaced eyebrows, and any type below 12sp.
- **Dialogs for news.** A dialog interrupts a decision; a snackbar reports what happened. And no dialog at all in the floating window — an Android dialog needs a foreground Activity, which that window has none of by design, so one there is not merely cramped, it never appears.
- **Gesture-only functions.** Long press is a shortcut, never the only way. Drag-to-hide has the notification's Hide action and a switch in Settings; the slider has increment and decrement actions.
- **Silence as an outcome.** The empty overlay, the dead Capture button, the dialog that never appeared: every one looked identical to a tap that was never registered. Something visible always says what happened.

## Deliberate divergences

The accent is pinned rather than wallpaper-derived; two tokens are darker than the published One UI values because those fail WCAG AA; the press compression is kept from the old world but re-timed onto the One UI curve. Each is recorded with its reason in `ONE-UI.md`.
