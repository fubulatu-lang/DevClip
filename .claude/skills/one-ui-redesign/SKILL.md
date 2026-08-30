---
name: one-ui-redesign
description: Redesign an existing codebase to match Samsung One UI. First runs a full nine-area conformance audit, then works through the findings proposing concrete patches for approval before anything is written. Covers web, Android and iOS. Use whenever someone asks to redesign, convert, migrate or restyle their UI to One UI, to make their app look like a Samsung app, to fix what a One UI audit found, or runs /one-ui:redesign.
argument-hint: "<repo path, or areas to redesign>"
---

# One UI — Re-design

Audit first, then change code — **with approval at every step. Never write a
file the user has not approved.**

Read `.claude/one-ui/reference/PLATFORMS.md` and `.claude/one-ui/reference/TOKENS.md`, then work with all
nine area skills.

## Phase 1 — Audit

Run `one-ui-audit` in full. Present the report and stop. Do not begin
proposing changes in the same breath as delivering findings — the user needs
to see the picture before deciding what to do about it.

Then ask two things:

1. **Scope** — everything, blockers only, or specific areas?
2. **Divergences** — confirm which of the "worth keeping" items stay. Brand
   accent is the usual one. Do not overwrite a brand colour because a design
   system said so.

## Phase 2 — Plan

Propose an ordered plan and get agreement before touching anything. The order
is not arbitrary; it is lowest-risk-highest-leverage first, and each stage
depends on the one before it.

1. **Blockers** — accessibility and usability failures. These get fixed
   whatever else is agreed.
2. **Tokens** — colour roles, spacing scale, radii, type scale, motion curves
   and durations. One file or module. Nothing visual changes yet if the values
   match what was already there; where they differ, this is where the whole
   product shifts at once.
3. **Primitives** — Button, ListRow, Dialog, SearchField, Toast, app bar. Fix
   the component once, and every screen using it improves.
4. **Structure and layout** — per screen: viewing/interaction split, action
   placement, keyline, breakpoints, large-screen behaviour.
5. **Icons** — set-wide consistency, tintable vectors, labels.
6. **Copy** — strings extracted, sentence case, verb labels, error framing.
7. **Motion** — apply the curves and durations, add reduce-motion paths.
8. **Sound and haptic** — only if the product uses them.

Skipping straight to step 4 is the common failure. Screens fixed before tokens
exist will need redoing.

## Phase 3 — Patch, one stage at a time

For each stage:

1. **Show the diff before writing it.** Full proposed content for new files,
   a unified diff for edits.
2. **Explain what changes visually.** "Buttons go from 8dp to 26dp radius —
   every button in the app becomes a pill" is what the user needs to hear, not
   a list of edited lines.
3. **Name the risk.** What might break, what needs visual checking, what has
   no test coverage.
4. **Wait for approval.** Then write, then confirm what was written.
5. **Verify.** Build or typecheck if possible. Re-run the relevant checks.
   Report honestly if something failed.

Never batch several stages into one approval. Never write a file and then
mention it afterwards.

## Rules for the patches themselves

- **Preserve behaviour.** This is a design migration, not a refactor. Do not
  rename props, restructure state, change APIs, or "improve" logic while
  passing through. If something is genuinely broken, note it separately and
  let the user decide.
- **Tokens, never literals.** A patch that replaces `#3478F6` with `#252525`
  has not fixed anything. Replace it with a role reference.
- **Do not add dependencies without asking.** For Android, the
  `tribalfs/oneui-design` library is the real thing and usually the right
  answer — but it is a dependency decision, with a minimum SDK and a build
  impact. Propose it, explain the tradeoff, and let the user choose. Note that
  it also pulls in the modified SESL androidx and material modules.
- **Keep patches small and reviewable.** A 2,000-line diff will not be read,
  and an unread diff is not an approved one.
- **Do not convert iOS into a Samsung skin.** Apply the principles using
  native components. Say when a change is an adaptation.
- **Do not touch tests, CI, or unrelated files.**

## Phase 4 — Close out

When the agreed scope is done:

- Re-run the audit and show before/after scores per area.
- List what was deliberately not done, and why.
- List what still needs human eyes — visual review on real devices, screen
  reader passes, anything marked "needs manual verification".
- Write the tokens and conventions into a short `ONE-UI.md` so the next person
  keeps the system rather than drifting back.

## If the user wants to skip the audit

They may say "just make it One UI". Push back once, briefly: without the audit
there is no baseline, no scope agreement, and no way to show progress. If they
still want to go straight to changes, do the token stage first and work
outward from there — but do not skip the approval gates. Those are not
negotiable, because the user asked for patches to approve, not for their
codebase to be rewritten.
