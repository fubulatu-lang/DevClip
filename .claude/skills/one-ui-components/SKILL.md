---
name: one-ui-components
description: Review or apply Samsung One UI component patterns — app bar, bottom bar, bottom navigation, buttons, dialogs, lists, search and toasts. Use when checking whether UI components match One UI shape and behaviour, or when the user runs /one-ui:components.
argument-hint: "<component, file, or directory to review>"
---

# One UI — Components

Read `TOKENS.md` §3, §4, §5 and §7, and `PLATFORMS.md`, before starting.

Samsung publishes guidance for eight components. Each has a shape and a
behaviour; getting the shape right and the behaviour wrong is still a failure.

## App bar

- Two states: **expanded** (large title, part of the viewing area) and
  **collapsed** (compact title in the bar). It collapses as content scrolls.
- Expanded title around 34sp, collapsed 17–18sp.
- Actions on the right are icon-only and limited — overflow the rest.
- The title is the screen's identity; do not put the app's name there on
  every screen.

## Bottom bar / bottom navigation

- **Bottom navigation** switches between top-level destinations: 3–5 items,
  persistent, with label plus icon. Selected state uses the accent colour;
  unselected uses secondary text colour.
- **Bottom bar** holds actions for the current screen, not navigation. Do not
  merge the two.
- Both live in the interaction area and must clear the gesture inset.

## Buttons

- **Pill-shaped: 26dp corner radius.** A rounded-rectangle button with an 8dp
  radius is the clearest signal a design is Material, not One UI.
- Label size 17sp.
- Three variants: contained (primary, filled with accent), outlined
  (secondary, 1dp border in primary text colour), text/borderless (tertiary).
- One contained button per screen region.
- Labels are verbs. Minimum touch target 48dp regardless of visual height.
- Destructive actions use the functional red role, and are never the default
  focused option in a dialog.

## Dialog

- Rounded container, title in sentence case, body text 15–17sp.
- Buttons at the bottom, **horizontally arranged with the confirming action on
  the right**; stack vertically only when labels do not fit.
- Verb labels, not "OK"/"Cancel", for consequential choices.
- Dismissible by scrim tap unless the choice is genuinely mandatory.
- On large screens, position near the triggering element rather than dead
  centre.

## Lists

- Row minimum height 48dp; single-line rows commonly ~56dp with 24dp side
  padding.
- Primary text 17sp, secondary 15sp, metadata 13sp.
- Dividers are the exception, not the rule — One UI prefers grouping by
  spacing and rounded containers. Where used, dividers are inset to the text
  keyline, not full-bleed.
- Selection background uses the low-alpha selected role with an 8dp radius —
  not a full-bleed highlight.
- Long lists get an index scroll or fast scroller rather than infinite drag.

## Search

- Search is a mode, not just a field: entering search transforms the app bar
  rather than pushing a new screen.
- Pill-shaped field, leading search icon, trailing clear button once there is
  input.
- Results update as the user types; there is no "search" button to press.
- Provide an empty state and a no-results state with a suggested next action.

## Toasts

- Short, transient, non-blocking, bottom-anchored above the interaction area.
- One line where possible, no title, no more than one action.
- Never use a toast for anything the user must act on or must not miss — that
  is a dialog or an inline message.
- Toasts must not stack. A new toast replaces the current one.

## Checks

| ID | Severity | Check |
|---|---|---|
| `CMP-01` | Blocker | Toast or transient message used for information the user must act on |
| `CMP-02` | Blocker | Destructive action as the default/focused dialog button |
| `CMP-03` | Major | Buttons not pill-shaped (radius materially below 26dp on a standard button) |
| `CMP-04` | Major | No collapsing/expanded app bar on primary content screens |
| `CMP-05` | Major | Bottom navigation mixing destinations with actions |
| `CMP-06` | Major | More than 5 bottom navigation items |
| `CMP-07` | Major | Search implemented as a separate screen rather than an app bar mode |
| `CMP-08` | Major | Multiple contained/primary buttons competing in one region |
| `CMP-09` | Minor | Full-bleed dividers instead of text-keyline-inset dividers |
| `CMP-10` | Minor | Button labels that are not verbs ("OK", "Yes") on consequential actions |
| `CMP-11` | Minor | Dialog buttons stacked when they would fit horizontally |
| `CMP-12` | Minor | List row heights or text sizes off the One UI scale |
| `CMP-13` | Minor | No empty state or no-results state for search |

## Platform notes

- **Android Views** — prefer the real components from `tribalfs/oneui-design`:
  `ToolbarLayout`, `SemSearchView`, `SemToast`, `BottomTabLayout`,
  `RoundedLinearLayout`, `TipPopup`, `FloatingActionBar`. If the project can
  take the dependency, recommend it explicitly with the Gradle coordinate; if
  not, reproduce shape and behaviour with tokens.
- **Compose** — Material 3 components restyled with One UI shape/typography.
  `Shapes(extraLarge = RoundedCornerShape(26.dp))` for buttons.
- **iOS** — do **not** clone Samsung chrome. Apply the *behaviour* (search as
  a mode via `.searchable`, large titles, bottom-anchored primary actions,
  verb-labelled alert buttons) using native components. Mark these as adapted.
- **Web** — build a small primitive set (Button, Dialog, ListRow, SearchField,
  Toast) with the tokens, and replace ad-hoc markup with it. Use `<dialog>` and
  ARIA `role="status"` for toasts.

## Output

Report as `ID | severity | file:line | component | what's wrong | expected`.
When fixing, build or fix the primitive first, then migrate call sites.
