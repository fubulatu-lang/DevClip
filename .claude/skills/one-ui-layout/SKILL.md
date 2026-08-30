---
name: one-ui-layout
description: Review or apply Samsung One UI layout rules — the 24dp keyline, adaptive side margins, the 589/960dp breakpoints, grid behaviour, safe areas, cutouts, foldables and large screens. Use when checking spacing and responsiveness against One UI, or when the user runs /one-ui:layout.
argument-hint: "<file, directory, or screen to review>"
---

# One UI — Layout

Read `TOKENS.md` §1, §2 and §7, and `PLATFORMS.md`, before starting.

## The 24dp keyline

The single most load-bearing layout rule in One UI. Samsung: place information
and interactive components with margins of **at least 24dp** on both the left
and right sides. Two reasons:

1. Curved screen edges distort content placed too close to the side.
2. One UI actively blocks touches in the side margins — the **Reject zone**
   (blocks stray touches in the interaction area) and the **Grip zone**
   (blocks palm and three-finger touches while holding the phone). A control
   placed inside these zones will be unreliable, not just ugly.

So a button at 16dp from the edge is not a slightly-tight button. It is a
button that may not respond.

## Adaptive margins

The real algorithm, from `AdaptiveCoordinatorLayout`:

| Screen width | Side margin |
|---|---|
| `< 589dp` | 0% (the 24dp keyline is the whole margin) |
| `589–959dp`, height > 411dp | 5% of screen width |
| `>= 960dp`, height <= 1919dp | 12.5% of screen width |

Content becomes full-width at `>= 589dp`. Note that these percentages are
*additional* to the keyline, and that content never simply stretches edge to
edge on a tablet — One UI keeps a reading measure.

## Large screens and foldables

- **600dp** is Samsung's stated threshold for switching to a large screen
  layout.
- On large screens: keep navigation permanently visible rather than behind a
  hamburger; increase grid density so more content is visible at once; use
  pop-overs and dual-pane instead of full-screen pushes for brief tasks.
- A drawer occupies **40% of screen width** between 600 and 959dp.
- Pop-ups should appear near the element the user touched, to reduce finger
  travel.
- Consider grip: on a large device, controls may belong on the *side* rather
  than the bottom (One UI's own Camera app does this in landscape).
- Support fold/unfold and rotation without losing state. A configuration
  change that resets a form is a Blocker.

## Safe areas and cutouts

- Content must avoid the camera cutout. One UI apps declare
  `windowLayoutInDisplayCutoutMode=shortEdges` and then lay out around the
  cutout rather than letterboxing.
- Respect system bars, gesture insets, keyboard insets and — on foldables —
  the hinge.
- On web, use `env(safe-area-inset-*)` and `viewport-fit=cover` where the app
  runs installed/standalone.

## Spacing discipline

Use the scale in `TOKENS.md` §1. The audit signal here is **magic numbers**:
`23px`, `p-[13px]`, `padding: 17dp`, `.padding(19)`. Any spacing value not on
the scale needs a justification or a fix.

Vertical rhythm: 20dp between sections, 16dp card padding vertical, 24dp card
padding horizontal.

## Checks

| ID | Severity | Check |
|---|---|---|
| `LAY-01` | Blocker | Interactive element within 24dp of a screen edge |
| `LAY-02` | Blocker | State lost on rotation, fold, or window resize |
| `LAY-03` | Blocker | Content occluded by cutout, system bar, keyboard or hinge |
| `LAY-04` | Major | No responsive behaviour at the 589dp / 960dp breakpoints |
| `LAY-05` | Major | No distinct large-screen layout above 600dp |
| `LAY-06` | Major | Content stretched edge-to-edge on wide screens with no reading measure |
| `LAY-07` | Major | Touch target below 48dp (44pt iOS) |
| `LAY-08` | Minor | Spacing values off the One UI scale (magic numbers) |
| `LAY-09` | Minor | Fixed pixel heights that break when text scales |
| `LAY-10` | Minor | Navigation hidden behind a drawer on a large screen |
| `LAY-11` | Minor | Pop-up appearing far from its trigger on a large screen |

## Platform notes

- **Android Views** — put the keyline in `dimens.xml`, override in
  `values-sw600dp/`. `AdaptiveCoordinatorLayout` implements the margin rule
  for free. Use `WindowInsetsCompat` for all insets, never hard-coded bar
  heights.
- **Compose** — `WindowSizeClass` maps to Compact/Medium/Expanded. Use
  `windowInsetsPadding`. Hoist the margin into a `CompositionLocal`.
- **SwiftUI** — size classes, `.safeAreaPadding()`, `ViewThatFits` for the
  dual-pane switch. `NavigationSplitView` for the large-screen navigation rule.
- **Web** — CSS custom property for the keyline, media queries at 589px and
  960px, `clamp()` for the reading measure, `env(safe-area-inset-*)` for
  notches. Flag arbitrary Tailwind values.

## Output

Report as `ID | severity | file:line | actual value | expected value`. When
fixing, define the token first, then replace call sites — never scatter the
literal 24 across files.
