# Report Contract

The report is the deliverable. A run that analyses well and reports loosely has
failed.

Three rules, in order of importance:

1. **All nine areas appear in every report.** Always. An area with no findings
   gets a row saying so. An area that doesn't apply gets `N/A` with a reason.
   An area you didn't get to gets `Not assessed`. Never silently omit one —
   the reader cannot tell the difference between "clean" and "skipped", so you
   must tell them.
2. **The file is always written**, even when the codebase is perfect, even
   when the run is cut short. Default path `ONE-UI-AUDIT.md` in the repo root.
   If the run is partial, write what you have and mark the rest
   `Not assessed`.
3. **Chat gets the summary, the file gets the detail.** Don't paste the whole
   report into the conversation. Headline score, blocker count, the table, the
   file path.

## Status vocabulary

Use exactly these. They are not interchangeable.

| Status | Means |
|---|---|
| `Pass` | Checked, and found conformant |
| `Findings` | Checked, and issues found — score and list them |
| `N/A` | The area genuinely doesn't apply (no audio in the product, no icon set of its own) |
| `Not assessed` | Not checked — out of scope, out of time, or needs a human |

`N/A` areas drop out of the weighted mean. `Not assessed` areas do **not**
count as passing and must be named in the summary.

## Template

```markdown
# One UI Conformance Report

| | |
|---|---|
| **Project** | <name> |
| **Platform** | <detected, e.g. Web (Next.js + Tailwind)> |
| **Date** | <YYYY-MM-DD> |
| **Scope** | <n> files across <dirs>; excluded <what and why> |
| **Method** | scanner + manual review of <what> |

## Overall: <score>/100 — <band>

<If a blocker capped the grade, say so here in one sentence.>
<If any area is Not assessed, say so here in one sentence.>

| # | Area | Status | Score | Blocker | Major | Minor |
|---|---|---|---|---|---|---|
| 1 | Structure | Findings | 72 | 0 | 4 | 6 |
| 2 | Layout | Findings | 64 | 1 | 5 | 3 |
| 3 | Components | Findings | 70 | 0 | 5 | 4 |
| 4 | Color | Findings | 55 | 1 | 6 | 2 |
| 5 | Iconography | Pass | 94 | 0 | 0 | 2 |
| 6 | Motion | Findings | 61 | 1 | 3 | 4 |
| 7 | Sound & Haptic | N/A | — | — | — | — |
| 8 | Writing | Findings | 78 | 0 | 3 | 7 |
| 9 | Accessibility | Findings | 48 | 3 | 8 | 5 |

## Blockers

Every blocker in full. These are the only findings reproduced at length.

### A11Y-01 · Interactive element with no accessible name
**Where:** `src/components/IconButton.tsx:34`
**What:** The close button renders an SVG with no label, so screen readers
announce it as "button".
**Expected:** An accessible name describing the action.
**Fix:** `aria-label="Close"` on the button, `aria-hidden="true"` on the SVG.

## Findings by area

### 1. Structure — 72/100 · Findings

| ID | Sev | Location | Finding | Expected |
|---|---|---|---|---|
| STR-01 | Blocker | `EditScreen.tsx:88` | Save only in the top bar | Primary action in the interaction area |

<Repeat for all nine areas. For a clean area:>

### 5. Iconography — 94/100 · Pass
Icons are inline SVG with `currentColor`, consistent 24px grid, uniform
1.5px stroke. Two minor findings below; nothing structural.

<For an N/A area:>

### 7. Sound & Haptic — N/A
No audio or haptic APIs used anywhere in the codebase. Excluded from the
weighted mean. Worth revisiting if the picker in `Settings` gains detents.

## What this codebase already does well

Specific, with locations. A report that lists only faults isn't trustworthy and
won't be acted on.

## Divergences worth keeping

Departures from One UI that are deliberate and correct. List them so a later
redesign doesn't flatten them.

| Where | Divergence | Why it should stay |
|---|---|---|
| `theme.css` | Brand accent `#6C4EF5` instead of system accent | Brand requirement; One UI's user-accent rule conflicts with it |

## Needs manual verification

Things code review cannot settle.

- Screen reader pass on the checkout flow — labels are present, but whether
  they're *meaningful* needs a human with VoiceOver.
- Visual check of dark theme on an AMOLED device.

## Recommended sequence

1. Blockers — <count>, listed above
2. Tokens — colour roles, spacing, radii, motion. Highest leverage, lowest risk.
3. Shared primitives — Button, Dialog, ListRow, Toast
4. Screen-level structure and layout
5. Copy and icons

Run `/one-ui:redesign` to work through this with patches for approval.
```

## Scoring reminder

Blocker 10 · Major 4 · Minor 1. Weighted mean across areas, weights in
`one-ui-audit/SKILL.md`. **Any blocker caps the overall at 74.** Round to a
whole number and don't imply precision the method doesn't have — 72 and 74 are
the same finding, so lead with the band.
