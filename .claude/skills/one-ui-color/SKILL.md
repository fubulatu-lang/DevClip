---
name: one-ui-color
description: Review or apply the Samsung One UI colour system — semantic role-based colour, light and dark themes, functional green/orange/red, user-controlled accent and contrast. Use when checking hard-coded colours, missing dark mode, or theme structure, or when the user runs /one-ui:color.
argument-hint: "<file, directory, or theme to review>"
---

# One UI — Color

Read `TOKENS.md` §5, and `PLATFORMS.md`, before starting.

## Colour is a role, not a value

This is the central discipline. One UI does not hand you a palette to paint
with; it hands you a set of **semantic roles** that resolve differently in
light and dark, and differently again when the user changes their theme
palette. Component code never names a colour — it names a role.

The roles:

| Role | Purpose |
|---|---|
| `background` | The window behind everything |
| `surface` | Cards, sheets, raised containers |
| `text-primary` | Body and headings — `#252525` light / `#FAFAFA` dark |
| `text-secondary` | Supporting text — `#3B3B3B`/`#505050` light / `#E5E5E5` dark |
| `text-tertiary` | Disabled, placeholder — `#8C8C8C` light / `#808080` dark |
| `icon-primary` | Aliases `text-primary` — icons are text-coloured by default |
| `divider` | List and section separators |
| `outline` | Borders on outlined controls |
| `accent` | Selection, active state — **user-controlled** |
| `scrim` | `#33000000` over content behind a modal layer |
| `selected-bg` | Low-alpha row/item selection |

**Functional colours** — the only semantic accents, never decorative:

- `functional-green` — success, positive, on
- `functional-orange` — warning, caution, degraded
- `functional-red` — error, destructive

## Accent belongs to the user

On One UI the accent is extracted from the user's wallpaper and theme palette.
A conformant app treats accent as a **runtime value**, not a brand constant.

This is the rule most likely to conflict with a real product's brand
requirements, and that conflict is legitimate. Do not silently override a
brand. Report it as a deliberate divergence and let the user decide: full One
UI behaviour (follow system accent), hybrid (brand for identity surfaces,
system accent for selection state), or brand-only (documented divergence).

On web the closest equivalents are `prefers-color-scheme`,
`prefers-contrast`, and `AccentColor`/`Highlight` system colours.

## Dark mode is required

Samsung recommends dark mode so the interface stays comfortable in low light.
One UI dark uses near-black backgrounds — good for AMOLED and for reducing
glare — with `#FAFAFA` rather than pure white text, to avoid halation.

Dark mode is not an inverted light theme. Elevation inverts: in light, raised
surfaces are *lighter* than the background; in dark, raised surfaces are
*lighter* too, achieved with tone rather than shadow. Saturated brand colours
usually need desaturating for dark.

## Contrast

Contrast is a colour concern as much as an accessibility one:

- Body text: **4.5:1**
- Large text (>= 18sp regular, >= 14sp bold): **3:1**
- UI component boundaries and meaningful graphics: **3:1**

Check both themes. A palette that passes in light and fails in dark is a
failure.

Never use colour alone to convey meaning — pair it with an icon, text or
shape. Roughly 1 in 12 men has a colour vision deficiency.

## Checks

| ID | Severity | Check |
|---|---|---|
| `CLR-01` | Blocker | Body text contrast below 4.5:1 in either theme |
| `CLR-02` | Blocker | No dark theme at all |
| `CLR-03` | Blocker | Colour is the only carrier of meaning (status, error, selection) |
| `CLR-04` | Major | Hard-coded hex/RGB in component code instead of a role token |
| `CLR-05` | Major | Dark theme is a mechanical inversion — same saturation, wrong elevation direction |
| `CLR-06` | Major | Functional green/orange/red used decoratively |
| `CLR-07` | Major | Accent hard-coded where system/user accent is available |
| `CLR-08` | Major | UI component boundary contrast below 3:1 |
| `CLR-09` | Minor | Pure `#FFFFFF` text on dark (should be `#FAFAFA`) |
| `CLR-10` | Minor | Palette larger than the role set — many near-duplicate greys |
| `CLR-11` | Minor | Theme switch not following the system setting by default |
| `CLR-12` | Minor | No `prefers-contrast` / high-contrast handling |

## Platform notes

- **Android Views** — `colors.xml` + `values-night/colors.xml`, referenced via
  `?attr/` theme attributes. Point aliases at the `sesl_*` roles if the One UI
  library is present.
- **Compose** — a `ColorScheme` plus a custom `OneUiColors` for roles Material
  lacks (functional trio, scrim, selected-bg). Read `isSystemInDarkTheme()`.
- **iOS** — asset catalog colour sets with Any/Dark (and High Contrast)
  appearances. They resolve automatically; never branch on
  `traitCollection.userInterfaceStyle` in view code.
- **Web** — CSS custom properties on `:root`, overridden inside
  `@media (prefers-color-scheme: dark)`. Support a manual override via a
  `data-theme` attribute that defaults to `system`. Flag every literal hex in
  component CSS.

## Output

Report as `ID | severity | file:line | current colour | role it should use |
measured contrast`. Compute real contrast ratios rather than estimating. When
fixing, define roles first, then replace literals, then verify both themes.
