---
name: one-ui-audit
description: Run a full Samsung One UI conformance audit across a whole codebase and produce a scored report covering all nine areas — structure, layout, components, color, iconography, motion, sound and haptic, writing, and accessibility. Works on web, Android and iOS projects. Use whenever someone asks how well their UI matches One UI, wants a One UI review, conformance check or gap analysis, asks "does my app look like One UI", or runs /one-ui:audit.
argument-hint: "<repo path, directory, or subset of areas>"
---

# One UI — Full Audit

Produce an honest, evidence-backed conformance report. This skill is
**read-only**: it never edits code. If the user wants changes, hand off to
`one-ui-redesign`.

Read `.claude/one-ui/reference/PLATFORMS.md`, `.claude/one-ui/reference/TOKENS.md` and `.claude/one-ui/reference/REPORT.md`
first, then the nine area skills. Each area skill owns its own checklist and check IDs; this skill
runs them all and aggregates.

## Step 0 — Run the deterministic scanner first

`.claude/one-ui/scripts/oneui_scan.py` catches the mechanically checkable violations —
hardcoded colours, uppercase transforms, non-One-UI easing, fixed font sizes,
side padding under 24, unlabelled controls, fixed heights on text containers.

```bash
python3 .claude/one-ui/scripts/oneui_scan.py <path> [--json]
```

It emits the same check IDs this skill uses, so its output drops straight into
the report. Treat it as a starting point, never as the audit: it cannot judge
structure, icon metaphors, motion meaning, or copy quality. Read the code for
those, and verify every scanner finding before reporting it — regexes produce
false positives, and a report full of them is worse than no report.

## Step 1 — Detect and scope

1. Detect the platform using `PLATFORMS.md`. If ambiguous, ask rather than
   guess — a wrong platform guess produces a worthless report.
2. Map the UI surface: where layouts, components, styles, themes, strings and
   assets live. Exclude build output, `node_modules`, generated code and
   vendored dependencies.
3. State the scope up front: how many files, which directories, what was
   excluded and why.

If the codebase is large, sample deliberately rather than skimming everything
shallowly, and say so — for example "all 14 screen composables plus the shared
theme and component packages; excluded tests and generated bindings." An audit
that silently looked at 5% of the code is worse than one that admits its
scope.

## Step 2 — Run the nine areas

| Area | Prefix | Skill |
|---|---|---|
| Structure | `STR` | `one-ui-structure` |
| Layout | `LAY` | `one-ui-layout` |
| Components | `CMP` | `one-ui-components` |
| Color | `CLR` | `one-ui-color` |
| Iconography | `ICN` | `one-ui-iconography` |
| Motion | `MOT` | `one-ui-motion` |
| Sound & Haptic | `SND` | `one-ui-sound-haptic` |
| Writing | `WRT` | `one-ui-writing` |
| Accessibility | `A11Y` | `one-ui-accessibility` |

Every finding needs a **file path and line number**. A finding without a
location is an opinion, not a finding.

Where a measurement is possible, measure it. Compute contrast ratios from the
actual resolved colours in both themes. Read actual `dp`/`px` values. Count
actual durations. Run `axe-core`, Lighthouse, Android Lint or Xcode's
Accessibility Inspector if the environment allows, and cite the output.

## Step 3 — Score

Severity weights:

| Severity | Weight | Meaning |
|---|---|---|
| **Blocker** | 10 | Breaks usability or accessibility for real users |
| **Major** | 4 | Clear, visible divergence from One UI |
| **Minor** | 1 | Polish, consistency, or a small off-scale value |

Per-area bands — state which band an area falls in and why:

| Band | Meaning |
|---|---|
| **90–100** | Conformant. Isolated minor issues. |
| **75–89** | Largely conformant. Majors present but the shape is right. |
| **50–74** | Partially conformant. Recognisable but inconsistently applied. |
| **25–49** | Divergent. Another design system, or none, is in effect. |
| **0–24** | Not conformant. |

Overall score is the **weighted mean** across areas:

| Area | Weight | Why |
|---|---|---|
| Accessibility | 2.0 | Failures here harm real people |
| Layout | 1.5 | Load-bearing for the whole system |
| Structure | 1.5 | Determines whether a screen reads as One UI at all |
| Components | 1.25 | Most visible surface |
| Color | 1.25 | Theming and contrast depend on it |
| Motion | 1.0 | |
| Writing | 1.0 | |
| Iconography | 0.75 | |
| Sound & Haptic | 0.5 | Often legitimately absent |

**Any Blocker caps the overall grade at 74**, regardless of the arithmetic.
Say so explicitly when it happens.

If an area genuinely does not apply — no sound or haptics anywhere, no icon
set of the project's own — mark it **N/A** and drop it from the weighted mean.
Do not score an absent area as 100, and do not score it as 0.

## Step 4 — Report

**A run always ends in a written report.** Follow `.claude/one-ui/reference/REPORT.md` — it is
the contract, not a suggestion. Three things it requires:

- **All nine areas appear**, every time, each with a status of `Pass`,
  `Findings`, `N/A` or `Not assessed`. Never silently omit an area; the reader
  can't distinguish "clean" from "skipped" unless you say which it was.
- **The file is written** to `ONE-UI-AUDIT.md` in the repo root (unless the
  user names another path) even when the codebase is clean, and even when the
  run is cut short — in that case write what you have and mark the remainder
  `Not assessed`.
- **Chat gets the summary, the file gets the detail.** Overall score and band,
  blocker count, the per-area table, the file path. Don't paste the whole
  report into the conversation.

The full template is in `.claude/one-ui/reference/REPORT.md`. Its shape:

```markdown
# One UI Conformance Audit

**Project:** <name>   **Platform:** <detected>   **Date:** <date>
**Scope:** <n files across ...; excluded ...>
**Overall: <score>/100 — <band>**   <if capped, say why>

## Scores

| Area | Score | Blockers | Major | Minor |
|---|---|---|---|---|
| Structure | 72 | 0 | 4 | 6 |
| ... | | | | |

## Blockers
Every blocker in full, with location and fix.

## Findings by area

### Structure — 72/100
| ID | Sev | Location | Finding | Expected |
|---|---|---|---|---|
| STR-01 | Blocker | ui/EditScreen.kt:88 | Save action only in the top app bar | Primary action in the interaction area |

## What this codebase already does well
Name specifics. An audit that lists only faults is not trustworthy.

## Divergences worth keeping
Places the code departs from One UI for good reason — brand accent, iOS
platform convention, an existing in-house design system. List them so the
redesign step doesn't flatten them.

## Needs manual verification
Checks that cannot be settled by reading code.

## Recommended sequence
1. Blockers
2. Tokens (color, spacing, radius, motion) — highest leverage, lowest risk
3. Shared primitives
4. Screen-level structure and layout
5. Copy and icons
```

## Honesty rules

These matter more than the score.

- **Never infer a pass.** If a file was not checked, it is unchecked, not
  passing. Keep "verified passing", "verified failing" and "not assessed"
  separate throughout.
- **Never invent line numbers.** Cite what was actually read.
- **Say when One UI is being adapted rather than applied.** On iOS and desktop
  web several rules are translations. Marking an adapted rule as a straight
  failure is misleading; so is marking it a clean pass.
- **Do not score generously to be encouraging.** A soft audit wastes the
  user's time.
- **Flag legitimate reasons to diverge.** Brand accent, an existing design
  system, native iOS conventions, and accessibility requirements that exceed
  One UI's own guidance are all valid. Note them rather than logging them as
  defects.

## Single-area runs

If the user runs a single-area command (`/one-ui:color`), run only that
skill's checklist and produce the same report shape scoped to that area. Do
not silently expand scope.
