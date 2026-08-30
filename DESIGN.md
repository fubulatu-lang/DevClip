# Design

The visual world DevClip is built in. **Token values live in [`ONE-UI.md`](ONE-UI.md)** and are not repeated here — one source of truth, or the two drift. This file says what the world *is* and why; that file says what the numbers are.

## World

**Samsung One UI, in the app icon's colours.**

Not a One UI pastiche applied to a generic app: the structural ideas do the work. The viewing area is what you read, the interaction area is what you touch and it sits within thumb reach. Depth is a tone shift and a 12dp radius, never a blurred shadow. Type is deliberately large — 17sp body, where the app previously ran at 13sp — because this is checked dozens of times a day from a small window, often in daylight, and legibility beats expression.

The palette is derived from the app icon rather than chosen beside it: its slate `#345065` and its blue `#3498DB`. Both are used **darkened**, because taken literally the icon blue is 3.15:1 under white text and would fail AA everywhere it matters.

**This replaced a previous world** — "Soft Structuralism": near-white silver-grey canvas, airy white cards with diffused shadows, one indigo accent, Manrope. That is now an anti-reference, not a fallback. Large blurred shadows, small type, and the indigo are all evidence of the old world, not options.

## Mode

**Operate.** The visitor completes a task: find a clip, paste it, get out. Scanability, consistency and native expectations outrank expression. Brand lives in precise details — the accent on a single primary action, the icon on the bubble, the easing on a press — not in decoration.

The one exception is onboarding, which is the only screen a user reads rather than operates, and is the only place the 34sp display size appears.

## Surfaces

| Surface | Root | Shape | Depth of function |
|---|---|---|---|
| **Mini** | `OverlayApp` | Tethered to the bubble, ~300×344dp | **Paste only.** No search, no sort, no edit, and no long press either — a gesture is never the only route to anything |
| **Expanded** | `OverlayApp` | Half-height sheet, bottom, full width | Search, sort, editing |
| **Full app** | `App` | Whole screen from the launcher | Everything, plus settings |

Mini and full are not one screen at two sizes. They are separate React roots with different jobs, and `index.ts` registers them separately.

## Adaptivity

Layout is driven by **window size class**, never a device model, so a phone in landscape, a tablet in split view, a folding phone opening, and the small overlay window each get the shape that fits the width they have. The rule is One UI's own, from `AdaptiveCoordinatorLayout` — see `ONE-UI.md` for the thresholds.

## What this world refuses

- **Colour literals.** Nothing outside `src/theme/theme.ts`. CI fails on a stray hex.
- **Hand-picked sizes.** Type, spacing, radius and icon size come from roles, not per-screen judgement.
- **Shadow as depth.** Cards carry none; only genuinely floating surfaces get elevation, and little of it.
- **Uppercase micro-labels**, letter-spaced eyebrows, and any type below 12sp.
- **Dialogs for news.** A dialog interrupts a decision; a snackbar reports what happened.
- **Gesture-only functions.** Long press is a shortcut, never the only way.

## Deliberate divergences

The accent is pinned rather than wallpaper-derived; two tokens are darker than the published One UI values because those fail WCAG AA; the press compression is kept from the old world but re-timed onto the One UI curve. Each is recorded with its reason in `ONE-UI.md`.
