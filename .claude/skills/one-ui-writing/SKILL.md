---
name: one-ui-writing
description: Review or apply Samsung One UI writing guidance — focused and purposeful, simple and human, empowering and engaging. Covers UI copy, button labels, error messages, empty states, sentence case and voice. Use when auditing interface copy or when the user runs /one-ui:writing.
argument-hint: "<file, directory, or copy to review>"
---

# One UI — Writing

Read `TOKENS.md` §12, and `PLATFORMS.md`, before starting.

Samsung publishes three writing principles. They are more prescriptive than
they sound.

## 1. Focused and purposeful

Lead with what matters to the user, then cut. Every word in an interface costs
the user attention.

- One idea per sentence. One sentence per label where possible.
- Front-load the outcome: "Photos deleted" before "You have successfully
  completed the deletion of the selected photos".
- Remove hedging ("please", "kindly", "you may want to"), filler ("simply",
  "just", "easily") and self-congratulation ("powerful", "seamless").
- Do not explain the system to the user. "Syncing with server" is the app's
  concern; "Saving your changes" is the user's.

## 2. Simple and human

Plain, conversational language. Write the way you would explain it to someone
standing next to you.

- No jargon and no internal vocabulary. If the codebase calls it an "entity"
  or a "payload", the user does not.
- No error codes as the primary message. A code can appear as secondary detail
  for support, never as the headline.
- Second person: "your files", not "the user's files".
- Active voice, present tense: "Wi-Fi is off", not "Wi-Fi has been disabled by
  the system".
- Contractions are fine and usually better ("can't" over "cannot").
- Sentence case for everything — labels, buttons, titles, menu items — except
  proper nouns. **Title Case Is A Material And iOS Convention, Not A One UI
  One.**

## 3. Empowering and engaging

Tell the user what they *can* do. Frame around the next action, not the
failure.

- Errors: say what happened, then what to do. "Couldn't upload. Check your
  connection and try again." Not "Error: upload failed."
- Never blame the user. "That password doesn't match" is better than "You
  entered an invalid password."
- Empty states are opportunities, not apologies: "Nothing here yet. Add your
  first note." Not "No data available."
- Permission requests explain the benefit before the ask.
- Destructive confirmations state the consequence plainly and whether it can
  be undone.

## Mechanics

| Rule | Do | Don't |
|---|---|---|
| Case | Sentence case | Title Case |
| Terminal period | None on single-sentence labels, buttons, toasts | "Saved." |
| Buttons | Verbs: "Delete", "Turn on", "Send" | "OK", "Yes", "Submit" |
| Numbers | Numerals: "3 items" | "three items" |
| Dates | Locale-aware formatting | Hard-coded "MM/DD/YYYY" |
| Truncation | Design for the longest realistic string | Ellipsis mid-word |
| Ampersands | "and" in body copy | "&" outside tight labels |

"OK" is acceptable only to dismiss a purely informational message. For any
choice with a consequence, both buttons name their action.

## Localisation

- No string concatenation to build sentences — word order differs by language.
  Use full parameterised strings.
- Plurals via the platform's plural system, not `if (n == 1)`.
- Budget for 30–40% expansion in German, Finnish and Russian.
- No text baked into images.
- Never hard-code strings in component code; every user-visible string lives
  in the resource layer.

## Checks

| ID | Severity | Check |
|---|---|---|
| `WRT-01` | Blocker | Raw error codes or stack traces shown to the user as the primary message |
| `WRT-02` | Blocker | Destructive confirmation that doesn't state the consequence |
| `WRT-03` | Major | Hard-coded user-visible strings in component code |
| `WRT-04` | Major | Title Case on labels, buttons or headings |
| `WRT-05` | Major | "OK"/"Yes"/"No" on consequential actions instead of verbs |
| `WRT-06` | Major | Errors that state the failure without a next action |
| `WRT-07` | Major | Sentences built by concatenation (breaks localisation) |
| `WRT-08` | Major | Manual singular/plural branching instead of plural resources |
| `WRT-09` | Minor | Internal/system vocabulary leaking into the UI |
| `WRT-10` | Minor | Filler and hedging words ("simply", "just", "please") |
| `WRT-11` | Minor | Empty states phrased as absence rather than invitation |
| `WRT-12` | Minor | Terminal periods on single-sentence labels |
| `WRT-13` | Minor | Passive voice or third person where second person reads better |
| `WRT-14` | Minor | No headroom for text expansion in fixed-width labels |

## Platform notes

- **Android** — `strings.xml`, `<plurals>`, `getQuantityString`. Flag literals
  in layouts and Kotlin/Java. Lint's `HardcodedText` catches some of this.
- **Compose** — `stringResource(R.string.x)`; flag literal `Text("...")` with
  user-visible copy.
- **iOS** — String Catalogs (`.xcstrings`) or `.strings`; `String.localized`;
  automatic grammar agreement where available.
- **Web** — an i18n library (i18next, FormatJS, `next-intl`) with ICU
  MessageFormat for plurals and interpolation. Flag literal JSX text.

## Output

Report as `ID | severity | file:line | current copy | suggested rewrite`.
Always supply the rewrite — a copy finding without a replacement is not
actionable. Preserve the user's product voice and terminology; One UI writing
rules govern structure and tone, not brand vocabulary.
