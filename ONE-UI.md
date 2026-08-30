# One UI in DevClip

DevClip follows the Samsung One UI design system. This file is the contract:
it records the tokens, the conventions and the deliberate divergences, so the
system holds rather than drifting back.

The baseline audit that started this work is in `ONE-UI-AUDIT.md`. Re-run a
conformance check any time with `/one-ui:audit`, or the mechanical subset with
`python3 .claude/one-ui/scripts/oneui_scan.py src`.

---

## The one rule

**Never write a literal.** No hex, no font size, no spacing number, no icon
size, no duration in a component file. Everything comes from
`src/theme/theme.ts` through `useTheme()`. If a value you need isn't there,
add it to the token layer — don't inline it.

As of the redesign, no colour literal exists anywhere outside `theme.ts`, and
no font size or icon size exists outside it either. Keep it that way.

## Tokens

All in `src/theme/theme.ts`, reached via `useTheme()`.

### Colour — roles, not a palette

Colour is semantic. `ink` is "primary text", not "near-black" — it resolves
differently in light and dark, and every consumer gets that for free.

| Role | Use |
|---|---|
| `bg` / `surface` / `surfaceSunken` | Page ground, card, inset control |
| `ink` / `inkSoft` / `inkFaint` | Primary, secondary, tertiary text — all ≥ 4.5:1 |
| `inkDisabled` | **Disabled states only.** 3.22:1, deliberately below AA |
| `accent` / `accentPressed` / `accentSoft` / `onAccent` | Primary action; `onAccent` is what sits on top of an accent fill |
| `danger` / `success` / `warning` (+ `*Soft`) | The three One UI functional colours. **Never decorative** |
| `divider` / `border` / `borderStrong` | Separation |
| `scrim` | Modal dim, One UI's 20% black |

Every text pair is verified against WCAG 2.1 AA. Two values deliberately
diverge from the published One UI tokens because the published ones fail:
`inkFaint` is `#6E6E6E` rather than `#8C8C8C` (3.22:1), and `accent` is
`#0072DE` rather than `#0381FE` (3.42:1 under white button text).

**If you change a colour, re-check contrast.** Both directions — as text on a
ground, and as a ground under `onAccent`.

### Spacing — and the keyline

`xs 4 · sm 8 · md 12 · lg 16 · xl 20 · xxl 24`, plus **`keyline: 24`**.

`keyline` is not a synonym for `xxl`. It means "distance from the screen
edge", and One UI requires 24dp minimum there to clear curved edges and the
Reject/Grip touch-blocking zones. Conflating the two is what caused the
original keyline violation. **Any component that touches a screen edge uses
`spacing.keyline` for that margin.**

### Type — bigger than you think

`display 34 · title 18 · body 17 · button 17 · secondary 15 · caption 13 · micro 12`

Spread a role, don't pick a number: `{ ...text.body, color: colors.ink }`.

One UI's scale is deliberately larger than Material's — that is a defining
characteristic, not an accident. **Body text is 17sp.** The pre-redesign app
ran at 10–13sp almost everywhere and read as a different design system
entirely.

The app uses the **system font**, which resolves to SamsungOne / One UI Sans
on Samsung devices. Weight comes from `fontWeight`, never a family name.

### Radius, icons, motion

- **Radius** — `xs 4 · sm 8 · md 12 · lg 22 · container 26 · pill 999`.
  Buttons are pills. A 4dp or 8dp radius button reads as Material.
- **Icons** — `icon.sm 18` (inside a compact container), `icon.md 24` (the
  One UI default), `icon.lg 48` (illustration), `icon.stroke 1.5` **for
  everything**. Symbols take the *text* colour; accent is for state only.
- **Motion** — `easing.standard` etc. are the real One UI beziers;
  `duration.*` runs 100–500ms. Nothing routine exceeds 500ms.

## Conventions

**Depth is tone, not shadow.** Cards separate by surface colour plus a 12dp
radius and carry no shadow at all. `shadow.floating` exists for genuinely
floating surfaces and is deliberately tiny. Large blurred shadows read as
Material.

**Structure: viewing area on top, interaction area at the bottom.** What the
user reads goes up top; what they touch goes within thumb reach. Capture lives
in a pinned bottom action bar for exactly this reason — don't move a primary
action back into the header.

**Every touch target is 48dp.** A control may *look* smaller: a 40dp chip with
`hitSlop: {top: 4, bottom: 4}` is correct, a 40dp chip without it is not.

**`minHeight`, never `height`, on anything containing text**, so it grows at
200% font scale instead of clipping.

**All copy lives in `src/strings.ts`.** Sentence case. Verbs on buttons
("Paste", "Clear all") — never "OK"/"Yes". Errors say what to do next.

**Every animation consults `useReduceMotion()`.** What respecting it means
varies: a transform is skipped, a screen transition becomes an instant cut.

**Accessibility is not a later pass.** Icon-only controls need an
`accessibilityLabel`; toggles need `accessibilityRole="switch"` and
`accessibilityState={{ checked }}`; segmented options need a real name, not an
abbreviation — TalkBack must say "Bubble size: Small", never "S". Decorative
icons are hidden with `importantForAccessibility="no"`. **A gesture is never
the only route to an action** — long press is a shortcut, and there is always
a button that does the same thing.

## Deliberate divergences

| Divergence | Why it stays |
|---|---|
| Fixed Samsung-blue accent | One UI derives accent from the wallpaper at runtime. DevClip pins it for brand identity and deterministic contrast. A system-accent opt-in would be the conformant version. |
| `inkFaint` / `accent` darker than the published tokens | The published values fail WCAG AA. Accessibility wins. |
| Physical press compression | Kept from the original design, but re-timed onto the One UI standard curve and now guarded by reduce-motion. |
| Flat header instead of a large collapsing title | The popup is 300–360dp wide. A One UI large title needs room it doesn't have. `text.display` is used on the full-screen onboarding, where it fits. |
| Full-screen onboarding, not a permission sheet | Android permission flows are system-presented; the custom screen bridges them. |

## Known gaps

- **`STR-02` / `STR-06`** — the header still presents the settings gear, the
  bubble toggle and three view tabs as co-equal. Separating them properly
  means lifting bubble state out of `PopupScreen`, which is a refactor, not a
  restyle.
- **`LAY-05`** — "Full app" mode sends `match_parent` but renders the same
  single-column layout. No 589/960dp adaptation yet.
- **`MOT-11`** — no predictive back. React Native 0.86 has no stable API for
  driving back-gesture progress.
- **`A11Y-12`** — the search field's placeholder is its only visible label.
  Needs a manual check that TalkBack announces the `accessibilityLabel` on
  focus.

## Needs human eyes

Static analysis and typecheck cannot settle these. **None of this has run on a
real device yet.**

1. **Nested `Pressable`** — the clip card's "more options" button sits inside
   the card's own `Pressy`. Confirm tapping ⋮ does not also paste.
2. **Vertical fit at 300×400dp** — header + search + chips + a pinned bottom
   bar leaves little room for cards. If it is too tight, hide the bubble
   toggle or the sort chips at that size.
3. **200% font scale** on every screen, especially the tab bar and sort chips.
4. **TalkBack pass** — card announcements, the segmented groups, the error
   banner's live region.
5. **Dark mode on AMOLED** — the ground is now true black.
6. **Onboarding heading at 34sp** wraps in a narrow popup. Intentional; may
   read as too large.
