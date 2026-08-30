---
name: one-ui-structure
description: Review or apply Samsung One UI structural principles — the viewing area / interaction area split, screen hierarchy, navigation depth, visual depth and elevation. Use when checking whether a screen is organised the One UI way, when a layout puts primary actions out of thumb reach, or when the user runs /one-ui:structure.
argument-hint: "<file, directory, or screen to review>"
---

# One UI — Structure

Structure is the first thing One UI gets right and the first thing most ports
get wrong. It is about *where things live on a screen* and *how deep the
hierarchy goes*, not about styling.

Read `TOKENS.md` §8 and §9, and `PLATFORMS.md`, before starting.

## The core idea: viewing area and interaction area

One UI divides the screen horizontally into two zones:

- **Viewing area** — the top portion. Content the user reads and recognises at
  a glance. Large title, generous whitespace, low interactive density.
- **Interaction area** — the lower portion. Everything the user touches,
  grouped in logical order, within comfortable thumb reach.

This exists because phones got tall and thumbs did not get longer. Samsung's
stated goal is that buttons stay "within easy reach, even on large screen
devices".

Practical consequences:

- A large/collapsing title at the top, which shrinks into the app bar as the
  user scrolls. The expanded title is *content*, not chrome.
- Primary and destructive actions go at the **bottom** on phone widths. A
  "Save" button that only exists in the top app bar is a structural violation.
- The top of the screen is for orientation and content; the bottom is for
  commitment.
- Do not fill the viewing area with dense controls. If a screen has no
  breathing room at the top, it is not One UI-shaped.

## Visual depth

One UI expresses depth through **rounded containers, background tone shift and
scrim** — not through drop shadows. Cards separate from the background by a
subtle surface tone difference plus a 12dp corner radius. Modal layers dim
what's behind them with a 20% black scrim (`#33000000`).

Large blurred shadows, multi-level `elevation` ladders and `box-shadow`
stacks read as Material Design, not One UI.

Depth ordering, shallowest to deepest: background → surface/card → floating
bar / FAB → bottom sheet → dialog → toast.

## Hierarchy and navigation depth

- Keep user journeys short. Samsung's first principle is "focus on the task at
  hand", which in structural terms means fewer screens between intent and
  completion.
- Aim for a maximum of three levels of navigation depth before a task
  completes. Deeper than that, look for a flattening opportunity (inline
  expansion, bottom sheet, or a settings sub-page that could be a single
  screen).
- One primary action per screen. If a screen has three equally-weighted
  primary buttons, the hierarchy has failed.
- Related settings and controls are grouped into containers with a shared
  heading, not listed flat.

## Checks

Each check has an ID, a severity, and what to look for.

| ID | Severity | Check |
|---|---|---|
| `STR-01` | Blocker | Primary/destructive action reachable only from the top of the screen on phone-width layouts |
| `STR-02` | Major | No viewing/interaction distinction — controls distributed evenly top to bottom with no top breathing room |
| `STR-03` | Major | No large or collapsing title on primary content screens |
| `STR-04` | Major | Depth conveyed by drop shadow rather than surface tone + radius + scrim |
| `STR-05` | Major | More than three navigation levels required to complete a core task |
| `STR-06` | Minor | Multiple competing primary actions on one screen |
| `STR-07` | Minor | Flat list of settings with no grouping into titled containers |
| `STR-08` | Minor | Modal layers without a scrim, or with a scrim outside 15–25% black |
| `STR-09` | Minor | Z-order inconsistent with the One UI depth ladder |

## Platform notes

- **Android Views** — `ToolbarLayout` / `CollapsingToolbarLayout` from the One
  UI design library gives the viewing/interaction split directly. Bottom
  actions belong in `BottomFloatingLayout` or `FloatingActionBar`.
- **Compose** — a `LargeTopAppBar` with `TopAppBarScrollBehavior` is the right
  primitive; put commitments in a `bottomBar`.
- **SwiftUI** — `.navigationBarTitleDisplayMode(.large)` already matches the
  viewing-area idea. Put primary actions in a `.safeAreaInset(edge: .bottom)`
  rather than only in a toolbar.
- **Web** — a sticky header that shrinks on scroll, plus a sticky bottom
  action bar under 589px width. On desktop widths the bottom-reach argument
  weakens; note this as adapted rather than applied.

## Output

When auditing, report findings as a table of `ID | severity | file:line |
what's wrong | what One UI expects`. When fixing, restructure in this order:
title/app bar first, then action placement, then depth, then grouping. Do not
touch colour or motion from this skill.
