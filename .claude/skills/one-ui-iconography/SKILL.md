---
name: one-ui-iconography
description: Review or apply Samsung One UI iconography rules — icon grid and sizing, background/container shape, symbol construction, stroke consistency, icon colour and adaptive launcher icons. Use when auditing an icon set for One UI conformance, or when the user runs /one-ui:iconography.
argument-hint: "<icon set, file, or directory to review>"
---

# One UI — Iconography

Read `TOKENS.md` §10, and `PLATFORMS.md`, before starting.

Samsung splits this into three: **background**, **symbol** and **icon colour**.

## Background (the container)

- Launcher and app icons sit on a consistent container shape with a consistent
  optical size, so a row of icons reads as one family.
- Keep the symbol inside the **inner 66% safe zone** of the adaptive icon
  canvas. Anything outside can be clipped by the user's chosen mask.
- The container is not decoration — it is what makes different icons look like
  they belong to one system. Do not vary container shape per icon.
- Avoid photographic or gradient-heavy containers; they fail at small sizes
  and under theming.

## Symbol

- Default in-UI symbol size is **24dp**, on a 24dp grid with the drawn area
  inset from the edges so icons of different silhouettes look optically equal.
- **Uniform stroke weight across the whole set.** Mixing 1.5dp and 2dp strokes,
  or mixing filled and outlined styles in the same context, is the most common
  and most visible icon violation.
- One metaphor per concept, used consistently. If "delete" is a bin in one
  place it is a bin everywhere — not an X somewhere else.
- Simplify aggressively. An icon that needs more than a moment to parse at
  24dp is too detailed.
- Selected state is normally the *filled* variant of the same symbol, not a
  different symbol.

## Icon colour

- Icons are **text-coloured by default**: `icon-primary` is an alias of
  `text-primary`. Icons are not accent-coloured just because they are icons.
- Accent colour is reserved for **selected or active** state.
- Functional green/orange/red apply to status icons only.
- Disabled icons use `text-tertiary`, not reduced opacity on a coloured icon.
- Icons must be tintable — ship them as vectors with a single fill/stroke that
  inherits colour, not as pre-coloured raster assets. A PNG icon set is a
  finding in itself, because it cannot follow the theme.

## Accessibility of icons

- Every icon that carries meaning needs a text alternative (content
  description, `aria-label`, accessibility label).
- Every purely decorative icon must be **hidden** from assistive technology
  (`importantForAccessibility="no"`, `aria-hidden="true"`,
  `.accessibilityHidden(true)`).
- An icon-only button needs a label and a 48dp target even if the glyph is
  24dp.
- Icon-only meaning without a text label is a comprehension risk — prefer icon
  plus label in navigation and toolbars where space allows.

## Checks

| ID | Severity | Check |
|---|---|---|
| `ICN-01` | Blocker | Meaningful icon with no text alternative |
| `ICN-02` | Major | Icon-only control with a touch target below 48dp |
| `ICN-03` | Major | Raster (PNG/JPG) icons where vector is possible — cannot be tinted or themed |
| `ICN-04` | Major | Mixed stroke weights or mixed filled/outlined styles within one context |
| `ICN-05` | Major | Icons hard-coded to a colour instead of inheriting the icon role |
| `ICN-06` | Major | Symbol outside the 66% adaptive icon safe zone |
| `ICN-07` | Minor | Icon sizes off the scale (not 24dp, or a documented multiple) |
| `ICN-08` | Minor | Same concept represented by different symbols in different places |
| `ICN-09` | Minor | Decorative icons exposed to screen readers |
| `ICN-10` | Minor | Accent colour used for non-selected icons |
| `ICN-11` | Minor | Icons from multiple libraries mixed in one product |

## Platform notes

- **Android** — vector drawables with `android:tint="?attr/..."`. Adaptive
  icons via `ic_launcher.xml` with foreground/background layers and a
  monochrome layer for themed icons. `contentDescription` on every meaningful
  `ImageView`; `null` explicitly on decorative ones.
- **Compose** — `Icon(painter, contentDescription)`; pass `null` for
  decorative. Tint via `LocalContentColor`.
- **iOS** — SF Symbols where a matching metaphor exists (they scale with
  Dynamic Type and inherit tint for free). Custom symbols as template-rendered
  PDFs/SVGs. `.accessibilityLabel` / `.accessibilityHidden(true)`.
- **Web** — inline SVG with `fill="currentColor"` so icons inherit text
  colour. `aria-hidden="true"` plus a visually-hidden label, or
  `role="img"` + `aria-label`. Flag `<img src="icon.png">`.

## Output

Report as `ID | severity | file/asset | what's wrong | expected`. Where the
finding is set-wide (mixed stroke weights), report it once with a
representative sample rather than once per file.
