---
description: "Run the full One UI review — all nine areas — and write one report"
argument-hint: "<path, defaults to repo root>"
---

Use the `one-ui-audit` skill and run **all nine areas** end to end:
Structure, Layout, Components, Color, Iconography, Motion, Sound & Haptic,
Writing, Accessibility.

Target: $ARGUMENTS  (if empty, the current repository root)

This is the full-review entry point. It is not a scoped run — do not narrow to
a subset of areas unless the user explicitly asks mid-run.

Required:

1. Read `.claude/one-ui/reference/PLATFORMS.md` and detect the platform. Ask if ambiguous.
2. Read `.claude/one-ui/reference/TOKENS.md` and `.claude/one-ui/reference/REPORT.md`.
3. Run `.claude/one-ui/scripts/oneui_scan.py` for the mechanical pass, then review by hand
   for everything the scanner can't judge.
4. Score every area, and **write `ONE-UI-AUDIT.md`** following the template in
   `.claude/one-ui/reference/REPORT.md` exactly.

All nine areas must appear in the report with a status of `Pass`, `Findings`,
`N/A` or `Not assessed`. Never omit one.

Finish by telling the user, in chat only: the overall score and band, the
blocker count, the per-area table, and the report's file path. Keep the detail
in the file.
