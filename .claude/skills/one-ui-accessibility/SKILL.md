---
name: one-ui-accessibility
description: Review or apply Samsung One UI accessibility guidance — screen reader support, focus order, colour and contrast, layout and typography scaling, interaction and control, and content alternatives. Use when auditing accessibility against One UI and WCAG 2.1 AA, or when the user runs /one-ui:accessibility.
argument-hint: "<file, directory, or screen to review>"
---

# One UI — Accessibility

Read `TOKENS.md` §13, and `PLATFORMS.md`, before starting.

Samsung organises accessibility into six areas. This skill follows those, with
WCAG 2.1 AA as the measurable floor. One UI's own design principle — "be
visibly comfortable" — makes accessibility a design concern, not a
remediation step.

## 1. Screen reader

- Every interactive element has an accessible name that says what it does, not
  what it looks like. "Delete photo", not "bin icon", not "button".
- Decorative images and icons are hidden from assistive technology.
- **State is exposed programmatically**: selected, expanded, checked,
  disabled, busy. A visually-highlighted tab that doesn't announce as selected
  is a failure.
- Grouped content is announced as a group — a list row announces once, not
  once per child view.
- Live regions announce dynamic changes (validation, loading completion,
  toasts) without stealing focus.
- Custom controls declare a role. A `<div onClick>` is invisible to TalkBack
  and VoiceOver.

## 2. Focus order

- Focus follows the visual reading order. Where DOM/view order and visual
  order diverge, fix the order — do not patch with `tabindex` values above 0.
- Nothing interactive is unreachable by keyboard or switch control.
- No focus traps except in modals, where the trap is required and must release
  on dismiss.
- Opening a dialog moves focus into it; closing returns focus to the trigger.
- The focus indicator is always visible and meets 3:1 contrast. Never
  `outline: none` without a replacement.
- Focus never lands on hidden or off-screen elements.

## 3. Colour and contrast

| Requirement | Threshold |
|---|---|
| Body text | 4.5:1 |
| Large text (>= 18sp regular / 14sp bold) | 3:1 |
| UI components, focus rings, meaningful graphics | 3:1 |

- Check **both** light and dark themes.
- Colour is never the sole carrier of meaning — pair with icon, text or shape.
- Support the platform high-contrast setting where it exists
  (`prefers-contrast`, Increase Contrast on iOS, High contrast fonts on One UI).

## 4. Layout and typography

- Text must survive **200% scaling** with no loss of content or function. This
  is where most apps fail: fixed-height rows, `maxLines=1` on a label that
  wraps at large sizes, and buttons that clip their text.
- Use scalable units everywhere: `sp` on Android, Dynamic Type on iOS, `rem`
  on web. Never `dp`/`px` for type.
- Content reflows at a 320dp-equivalent width without horizontal scrolling.
- Line length stays readable — roughly 45–75 characters.
- Do not disable user font scaling. Do not cap it at a small multiplier.
- Respect the system's bold-text and display-size settings.

## 5. Interaction and control

- Touch targets: **48dp** (44pt iOS, 44px web minimum). Applies to icon-only
  buttons, close buttons and list row affordances.
- Spacing between adjacent targets so they aren't mis-hit.
- Every gesture-only action has a non-gesture alternative. Swipe-to-delete
  needs a button or menu equivalent.
- No action depends on a timed response unless the user can extend or disable
  the limit.
- Drag-and-drop has a keyboard or menu path.
- Motion respects reduce-motion (see the motion skill).

## 6. Content

- Form fields have persistent visible labels — placeholder-only labelling
  fails, because the label vanishes on input.
- Errors are identified in text, associated with their field, and describe how
  to fix.
- Required fields are marked in text, not by colour or an asterisk alone.
- Headings are real headings, in a logical hierarchy with no skipped levels.
- Language is declared, and marked where it changes mid-content.
- Media has captions; audio-only content has a transcript.
- Link and button text makes sense out of context — not "click here" or "read
  more" repeated.

## Checks

| ID | Severity | Check |
|---|---|---|
| `A11Y-01` | Blocker | Interactive element with no accessible name |
| `A11Y-02` | Blocker | Control unreachable by keyboard or switch |
| `A11Y-03` | Blocker | Body text contrast below 4.5:1 in either theme |
| `A11Y-04` | Blocker | Content lost, clipped or non-functional at 200% text scale |
| `A11Y-05` | Blocker | Colour as the only carrier of meaning |
| `A11Y-06` | Blocker | Gesture-only action with no alternative |
| `A11Y-07` | Major | Selected/expanded/disabled state not exposed programmatically |
| `A11Y-08` | Major | Focus order diverging from visual order |
| `A11Y-09` | Major | Focus indicator removed or below 3:1 contrast |
| `A11Y-10` | Major | Touch target below 48dp / 44pt |
| `A11Y-11` | Major | Non-scalable text units (`dp`, `px`) |
| `A11Y-12` | Major | Placeholder used as the only field label |
| `A11Y-13` | Major | Custom control with no role (e.g. clickable `div`) |
| `A11Y-14` | Major | Focus not moved into, or not restored from, a dialog |
| `A11Y-15` | Minor | Skipped heading levels or headings faked with styled text |
| `A11Y-16` | Minor | Decorative images exposed to assistive technology |
| `A11Y-17` | Minor | Dynamic changes not announced via a live region |
| `A11Y-18` | Minor | Generic link text ("click here", "read more") |
| `A11Y-19` | Minor | Missing captions or transcripts |
| `A11Y-20` | Minor | No high-contrast / `prefers-contrast` handling |

## Verification, not assertion

Never report accessibility conformance from reading code alone where a
measurement is possible.

- Compute real contrast ratios from the actual resolved colours in both themes.
- Where the environment allows, run `axe-core`, Lighthouse, Android Lint
  accessibility checks, or Xcode's Accessibility Inspector, and cite the
  output.
- Where a check genuinely requires a human (screen-reader comprehension,
  whether a label is *meaningful* rather than merely present), say so and list
  it as "needs manual verification" rather than passing it silently.

## Platform notes

- **Android** — `contentDescription`, `importantForAccessibility`,
  `AccessibilityNodeInfo` state, `labelFor`, `accessibilityLiveRegion`,
  `minWidth`/`minHeight` 48dp, `sp` for text.
- **Compose** — `Modifier.semantics { }`, `contentDescription`,
  `stateDescription`, `Role`, `clearAndSetSemantics` for grouping,
  `minimumInteractiveComponentSize()`.
- **iOS** — `accessibilityLabel`, `accessibilityValue`, `accessibilityTraits`,
  `accessibilityElement(children:)`, `UIAccessibility.post(notification:)`,
  Dynamic Type via `UIFontMetrics` / `@ScaledMetric`.
- **Web** — semantic HTML first, ARIA only where HTML falls short. `<button>`
  not `<div role="button">`. `aria-live`, `aria-expanded`, `aria-current`,
  visible `:focus-visible` styles, `rem` units.

## Output

Report as `ID | severity | file:line | issue | WCAG criterion | fix`. Separate
"verified failing", "verified passing" and "needs manual verification" — do
not merge them into a single pass/fail number that overstates confidence.
