# One UI in DevClip

DevClip follows the Samsung One UI design system, in monochrome. This file is
the contract: it records the tokens, the conventions and the deliberate
divergences, so the system holds rather than drifting back.

The values live in `app/src/main/java/com/devclip/app/DevClipTheme.kt`. The
Compose theme (`ui/Theme.kt`) converts them into Compose types rather than
restating them, and the floating windows read them directly — so the app, the
bubble and the floating list cannot disagree. Where the numbers come from is
in `.claude/one-ui/reference/TOKENS.md`.

Run a conformance check with `/one-ui:audit`, or the mechanical subset with
`python3 .claude/one-ui/scripts/oneui_scan.py app/src/main` — CI runs the
latter on every pull request, advisory rather than blocking.

---

## The one rule

**Never write a literal.** No colour, font size, spacing number, radius, icon
size or duration in a screen or view. Everything comes from `DevClipTheme` —
in Compose through `Tokens.colors`, `Space`, `Radius` and
`MaterialTheme.typography`; in views through `DevClipTheme.colors(context)`
and the nested objects. If a value you need isn't there, add it to the token
layer, don't inline it.

Two standing exceptions, both in the token layer's orbit:

- `res/values/colors.xml` paints the window before any Kotlin runs, and must
  agree with `bg`.
- `DevClipTheme.Overlay` holds the colours drawn over other apps (below). They
  are fixed on purpose, and named so they are exceptions rather than strays.

## Tokens

### Colour — roles, not a palette

Colour is semantic. `ink` is "primary text", not "near-black" — it resolves
differently in light and dark, and every consumer gets that for free. Light
and dark follow the Settings choice, which defaults to following the system.

| Role | Light | Dark | Use |
|---|---|---|---|
| `bg` | `#FFFFFF` | `#000000` | The page. True white, true black |
| `surface` | `#F7F7F7` | `#121212` | Cards and rows, separated from `bg` by tone |
| `surfaceSunken` | `#EDEDED` | `#1C1C1C` | Wells, badges, unselected chips |
| `ink` | `#252525` | `#FAFAFA` | Primary text and icons |
| `inkSoft` | `#3B3B3B` | `#E5E5E5` | Secondary text |
| `inkFaint` | `#505050` | `#B0B0B0` | Tertiary text, notes, metadata |
| `inkDisabled` | `#8C8C8C` | `#808080` | **Disabled only.** Below AA by design |
| `accent` / `onAccent` | `#252525` / `#FFFFFF` | `#FAFAFA` / `#000000` | Selected and primary. Ink at full strength — no hue |
| `accentSoft` | `#EDEDED` | `#1C1C1C` | A selected background that steps back |
| `success` | `#0F7A4A` | `#4FD18B` | Working, done |
| `warning` | `#A65A00` | `#FFB84D` | Something is wrong and needs the user |
| `danger` | `#C62F26` | `#FF8A80` | Destructive actions, errors |
| `border` / `divider` | 12% / 8% black | 15% / 11% white | Separation |
| `scrim` | 20% black | 20% black | One UI's modal dim |

Measured on `surface`, the text roles run from 14.3:1 (`ink`) to 7.5:1
(`inkFaint`) in light and 18.0:1 to 8.6:1 in dark. The functional colours are
5.0:1 or better as text on `surface` in light and 8.2:1 or better in dark, and
still pass AA on `surfaceSunken`. `inkDisabled` is 3.14:1 in light and is
never used for text anyone needs to read.

**If you change a colour, re-check contrast.** Both directions — as text on a
ground, and as a ground under `onAccent`.

**The functional colours are never decorative, and never alone.** Success is
paired with "Working", "Granted" or "Done"; caution with the words that say
what is wrong; danger with a verb like "Delete". Under Settings › Status the
mark differs in shape as well: a filled dot for working, a filled caution dot
for a problem, and a hollow ring — no colour at all — for something the user
switched off on purpose.

### Floating over other apps — `DevClipTheme.Overlay`

The bubble's selection ring, the edge handle and the drag-to-hide target sit
on a background nobody controls. They use a fixed white and black together —
a dark stroke outside a white one — so whichever the app underneath swallows,
the other survives. The hide target is a 210-alpha near-black circle over a
gradient scrim, and turns light-theme `danger` (and grows) when the bubble is
over it.

### Spacing — and the keyline

`XS 4 · SM 8 · MD 12 · LG 16 · XL 20`, plus **`KEYLINE 24`**.

`KEYLINE` is not a synonym for a large gap. It means "distance from the
screen edge", and One UI requires at least 24dp there to clear curved edges
and the touch-blocking zones a hand wrapped round the phone creates. **Any
component that touches a screen edge uses the keyline — or the adaptive
margin, which is never less.**

### Adaptive margins — `DevClipTheme.Breakpoint`

From One UI's `AdaptiveCoordinatorLayout`, applied by `rememberWindowLayout()`:

| Window width | Side margin | Clip columns |
|---|---|---|
| `< 589dp` | 24dp keyline | 1 |
| `589–959dp` | 5% of width, if the window is ≥ 412dp tall | 2 |
| `≥ 960dp` | 12.5% of width, if the window is ≥ 412dp tall | 2 |

Never below the keyline. Columns depend on width alone, so a phone on its
side gets two columns on the keyline.

### Type — bigger than you think

`HERO 56 · DISPLAY 34 · TITLE 18 · BODY 17 · BUTTON 17 · SECONDARY 15 · CAPTION 13 · MICRO 12`, all in sp.

In Compose these are Material's slots: `displaySmall` is `DISPLAY`,
`titleMedium` `TITLE`, `bodyLarge` `BODY`, `labelLarge` `BUTTON`,
`bodyMedium` `SECONDARY`, `bodySmall` `CAPTION`, `labelSmall` `MICRO`. `HERO`
is only the pull-down app bar's title when fully open.

The floating list uses `MiniText`: the same roles at 85%, never below
`MICRO`. Relative, so it follows the user's font scale.

One UI's scale is deliberately larger than Material's. **Body text is 17sp.**
The app uses the **system font**, which resolves to SamsungOne / One UI Sans
on Samsung devices. Weight comes from `FontWeight`, never a family name.

### Radius, icons, touch, motion

- **Radius** — `XS 4 · SM 8 · MD 12 · LG 22 · PILL 26 · CONTAINER 26`.
  Buttons are pills; cards are `MD`; sheets, dialogs and the floating list are
  `CONTAINER`. A 4dp or 8dp button reads as Material.
- **Icons** — `SM 18` (sharing a row with text, and the floating list's
  drawn glyphs), `MD 24` (standalone), `LG 48`, stroke `1.8dp`. Icons take the
  text colour; there is no accent to give them.
- **Touch** — `MIN_TOUCH_TARGET 48`. A control may be drawn smaller; its
  target may not.
- **Badges** — `Badge.MIN 28`, `Badge.MINI_MIN 20` for the floating list.
  Minimums, never fixed sizes: the number inside grows with the font.
- **Motion** — `Easing.STANDARD` (0.4, 0, 0.2, 1), One UI's standard curve.
  Its other curves and its 100–500ms duration scale are in `TOKENS.md`; add
  them to `DevClipTheme` when something animates on them. Nothing routine
  exceeds 500ms.

## Conventions

**Depth is tone, not shadow.** Cards separate by surface tone plus a 12dp
radius and carry no shadow. Only the bubble, which genuinely floats, has
elevation (6dp). Large blurred shadows read as Material.

**Viewing area on top, interaction area within reach.** What the user reads
goes up top; what they touch goes where a thumb is. Pulling down at the top of
a screen opens the app bar and brings its controls down with it — a reach
affordance, not a scroll effect. Setup's buttons sit at the bottom.

**`heightIn(min = …)` and `sizeIn(min…)`, never a fixed height, on anything
containing text**, so it grows at 200% font scale instead of clipping.

**All copy lives in `res/values/strings.xml`.** Sentence case. Verbs on
buttons ("Paste", "Delete all", "Undo") — never "OK" or "Yes". Errors say
what to do next.

**Every animation checks `ANIMATOR_DURATION_SCALE`.** The bubble's settle,
its tap flash, the selection ring's pulse and the edge handle's breath all
hold still when animations are off; the haptic still fires.

**Accessibility is not a later pass.**

- Icon-only controls have a content description that says what they do
  ("Edit clip 3", "Close DevClip").
- A control and its label are one node: switch rows use `toggleable` with
  `Role.Switch`; sliders carry their label and their value in the units on
  screen; choice chips are a `selectableGroup` of `Role.RadioButton`.
- Gestures carry labels: a clip row says "paste" and "edit"; the bubble's
  click and long click are named accessibility actions.
- Status messages are live regions.
- Section titles are headings.
- **A gesture is never the only route to an action.** Long press is a
  shortcut, and there is always a visible control that does the same thing.

**Destruction.** Deleting one clip is immediate and offers Undo. Deleting all
of them is the one decision the app asks about first, in a dialog whose
buttons are the verbs.

## Deliberate divergences

| Divergence | Why it stays |
|---|---|
| No accent at all | One UI derives an accent from the wallpaper. DevClip floats over other apps all day and should not compete with them; the icon is where its colour lives. Contrast is deterministic as a result. |
| `inkDisabled` (published tertiary `#8C8C8C`) used for disabled only | At 3.14:1 it fails WCAG AA as text. `inkFaint` uses the published secondary `#505050` instead. |
| Pull-down app bar built by hand | Material's `LargeTopAppBar` collapses on scroll but cannot open from a list already at the top, which is the One UI gesture. `OneUiHeader` does both. |
| Toasts from the services | The overlay service has no Activity to host a snack, and the windows it would draw one in have often just been torn down. The launcher app uses snacks. |
| Views, not Compose, for the floating windows | A service-owned window has no lifecycle owner, and Compose without one renders nothing. |

## Known gaps

- **Predictive back** — the app handles Back on every screen, but has not
  opted in to the predictive back animation.
- **Expanded height limit** — One UI's expanded margin also stops above
  1919dp of height. DevClip does not apply that ceiling; no device it runs on
  comes near it.
- **Nothing has been verified on a screen reader or at 200% font scale on a
  device.** The semantics above are in the code; a TalkBack pass on the
  phone is what proves them.
