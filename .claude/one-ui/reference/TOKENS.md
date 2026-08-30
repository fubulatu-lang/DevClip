# One UI Token Reference

Every value here is traceable to either the Samsung One UI design guidelines
(developer.samsung.com/one-ui) or the `tribalfs/oneui-design` Android library
resources. Nothing here is invented. Where a value is a reasonable derivation
rather than a published constant, it is marked **(derived)**.

Base unit is the Android `dp`. Mapping to other platforms:

| Platform | Unit | Conversion from dp |
|---|---|---|
| Android Views / Compose | `dp` / `sp` | 1:1 |
| iOS (UIKit / SwiftUI) | `pt` | 1:1 |
| Web (CSS) | `px`, or `rem` at 16px root | 1dp = 1px; 1rem = 16dp |

---

## 1. Spacing

One UI uses a 2dp-resolution scale, not a strict 8pt grid. Observed values in
the library, in order of frequency:

```
2, 4, 6, 8, 10, 12, 16, 18, 20, 24
```

**Canonical spacing tokens**

| Token | Value | Used for |
|---|---|---|
| `space-xs` | 4dp | Icon-to-badge, hairline offsets |
| `space-sm` | 8dp | Icon-to-label, chip gaps |
| `space-md` | 12dp | Inner control padding |
| `space-lg` | 16dp | Card inner padding (vertical) |
| `space-xl` | 20dp | Section vertical rhythm |
| `space-2xl` | 24dp | **Screen side keyline** and card inner padding (horizontal) |

**The 24dp keyline is the single most important spacing rule in One UI.**
Samsung states: place information and interactive components with margins of
at least 24dp on both the left and right sides, to avoid conflicts with curved
screen edges and to stay clear of the Reject and Grip touch-blocking zones.

## 2. Responsive breakpoints

From `AdaptiveCoordinatorLayout` in the One UI design library — this is the
real adaptive-margin algorithm One UI apps use:

| Screen width | Side margin | Name |
|---|---|---|
| `< 589dp` | 0% (24dp keyline only) | Compact |
| `589dp – 959dp` (and height > 411dp) | 5% of screen width | Medium |
| `>= 960dp` (and height <= 1919dp) | 12.5% of screen width | Expanded |

Content width switches to full-bleed (`match_parent`) at `>= 589dp`.

Two further published thresholds:

- **600dp** — Samsung's stated cutoff for "use a large screen layout".
- **600–959dp** — a navigation drawer occupies 40% of screen width.

**Web equivalent** (derived, same numbers as px):

```css
/* compact  */ @media (max-width: 588px)  { --oui-gutter: 24px; }
/* medium   */ @media (min-width: 589px)  { --oui-gutter: 5vw; }
/* expanded */ @media (min-width: 960px)  { --oui-gutter: 12.5vw; }
```

**iOS equivalent** (derived): map Compact to `.compact` horizontal size class,
Medium/Expanded to `.regular`. Use `600pt` as the split-view threshold.

## 3. Typography

Values are from library dimens. One UI's type scale is notably larger than
Material's — this is deliberate and is a defining characteristic of the system.

| Role | Size | Weight | Notes |
|---|---|---|---|
| Expanded app bar title | 34sp **(derived from `oui_des_ail_app_label_text_size`)** | Regular | Collapses to 17–18sp |
| Section / card title | 18sp | Medium | `oui_des_relative_link_title_text_size` |
| Body, list primary, button | 17sp | Regular | `oui_des_relative_link_text_size`, button style |
| Secondary / supporting | 15sp | Regular | `oui_des_tip_popup_balloon_message_text_size` |
| Caption / metadata | 13sp | Regular | `oui_des_ail_sub_text_size` |
| Micro label | 12sp | Regular | `oui_des_grid_menu_dialog_item_text_size` |

Default family is **SamsungOne / Samsung Sans**. On non-Samsung platforms use
the closest available humanist sans (system font is acceptable and preferred
over a bad substitute).

Text must scale with the user's font-size setting. Never hard-code `px` for
type on web — use `rem`. Never disable Dynamic Type on iOS. Never use `dp`
instead of `sp` on Android.

## 4. Corner radius

| Token | Value | Used for |
|---|---|---|
| `radius-xs` | 4dp | Scanner reticles, hairline chips |
| `radius-sm` | 8dp | List item selection background, tab indicator |
| `radius-md` | 12dp | Cards, floating action bar buttons |
| `radius-lg` | 22dp | Round buttons in preferences |
| `radius-pill` | 26dp | **Standard contained/outlined button** |
| `radius-container` | 26dp | Rounded drawer category container |

One UI buttons are pill-shaped. A 4dp or 8dp radius button is a strong signal
the design is Material, not One UI.

## 5. Color

One UI colour is **role-based, not palette-based**. The library defines
semantic roles that resolve differently in light and dark. Hard-coded hex in
component code is the primary colour violation.

| Role | Light | Dark |
|---|---|---|
| Background | `sesl_round_and_bgcolor_light` | `sesl_round_and_bgcolor_dark` |
| Surface / card | `sesl_background_color_light` | `sesl_background_color_dark` |
| Primary text | `#252525` | `#FAFAFA` |
| Secondary text | `#3B3B3B` / `#505050` | `#E5E5E5` |
| Tertiary / disabled | `#8C8C8C` | `#808080` |
| Divider | `sesl_list_divider_color_light` | `sesl_list_divider_color_dark` |
| Outline button border | `#252525` | `#FAFAFA` |
| Contained button text | `#FAFAFA` | — |
| Scrim / dim | `#33000000` | `#33000000` |
| Selected row background | `#0D010102` | (low-alpha white) |

**Functional colours** — these are the only semantic accents, and they must
not be repurposed for decoration:

| Role | Token |
|---|---|
| Success / positive | `sesl_functional_green` |
| Warning / caution | `sesl_functional_orange` |
| Error / destructive | `sesl_functional_red` |

The accent/primary colour is **user-controlled** on One UI (theme palette
extracted from wallpaper). A One UI-conformant app treats accent as a runtime
value, not a brand constant. On web/iOS the equivalent is respecting
`prefers-color-scheme` and, where available, system accent.

Dark mode is not optional. Samsung: "It's recommended that you provide a dark
mode so that the user can use their smartphone comfortably."

## 6. Motion

Real easing curves from `CachedInterpolatorFactory`. These are cubic beziers
and port directly to CSS `cubic-bezier()` and iOS `CAMediaTimingFunction`.

| Name | Bezier | Use |
|---|---|---|
| `sine-in-out-33` | `0.33, 0, 0.67, 1` | General ease-in-out, symmetric |
| `sine-in-out-60` | `0.33, 0, 0.4, 1` | Slightly decelerated |
| `standard` | `0.4, 0, 0.2, 1` | Screen and story transitions |
| `emphasized-decelerate` | `0.22, 0.25, 0, 1` | Elements entering |
| `back-gesture` | `0.1, 0.1, 0, 1` | Predictive back progress |
| `drawer-settle` | `0, 0, 0, 1` | Drawer / panel settle |
| `overshoot` | `OvershootInterpolator` | Playful confirmation only |
| `elastic-50` | SESL elastic | Tip popups, attention-getters |

**Durations** observed in the library:

| Token | Value | Use |
|---|---|---|
| `duration-instant` | 100ms | Ripple, state flip |
| `duration-short` | 120ms | Micro-feedback |
| `duration-standard` | 167ms | Most UI transitions |
| `duration-medium` | 200ms | Reveal, expand |
| `duration-emphasized` | 260ms | App bar collapse |
| `duration-long` | 400ms | Full-screen / shared element |
| `duration-extended` | 500ms | Onboarding, complex choreography |

Nothing routine should exceed 500ms. `linear` is for progress indicators and
looping animations only.

All motion must respect the reduce-motion setting: `prefers-reduced-motion` on
web, `UIAccessibility.isReduceMotionEnabled` on iOS,
`Settings.Global.ANIMATOR_DURATION_SCALE` / `TRANSITION_ANIMATION_SCALE` on
Android.

## 7. Touch targets

| Platform | Minimum |
|---|---|
| Android / One UI | 48dp × 48dp |
| iOS | 44pt × 44pt |
| Web | 44px × 44px (48px preferred) |

A control may be visually smaller than its target, but the target itself must
meet the minimum.

## 8. Structure — viewing area / interaction area

The defining structural idea of One UI. The screen is split:

- **Viewing area** (top): what the user reads. Large title, generous
  whitespace, centre- or start-aligned. Recognisable at a glance.
- **Interaction area** (bottom): what the user touches. Controls grouped in a
  logical order, within thumb reach.

On a collapsing app bar, the expanded title lives in the viewing area and
collapses into the app bar as content scrolls. Primary actions belong at the
bottom — never only at the top — on phone-width layouts.

## 9. Elevation and visual depth

One UI conveys depth with **rounded containers, background tone shift and
scrim** rather than heavy drop shadows. Card separation comes from a subtle
surface tone difference plus a 12dp radius. Large blurred shadows read as
Material, not One UI.

Scrim over content: `#33000000` (20% black).

## 10. Iconography

- Symbols sit on a consistent optical grid with generous padding inside the
  keyline; default symbol size is **24dp** (`oui_des_grid_menu_dialog_item_icon_size`).
- Stroke weight is uniform across an icon set. Mixing filled and outlined
  styles in the same context is a violation.
- Icon colour follows `primary_icon_color`, which is an alias of primary text
  colour — icons are text-coloured by default, not accent-coloured. Accent is
  reserved for selected/active states.
- Adaptive icon safe zone: keep the symbol within the inner 66% of the
  launcher icon canvas.
- Every icon that carries meaning needs a text alternative; every icon that is
  decorative must be hidden from assistive technology.

## 11. Sound and haptic

- Haptic feedback confirms an action; it never announces one. Fire on
  completion or on a state change the user caused, not on arrival of content.
- Use the platform's semantic haptic vocabulary rather than raw vibration
  durations: `HapticFeedbackConstants` on Android,
  `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator` on iOS,
  `navigator.vibrate` on web (sparingly — support is patchy and it is often
  the wrong choice).
- Sound is opt-in and must respect the ringer/silent switch and the system
  media volume. Never play UI sound at a fixed volume.
- Every sound or haptic cue must have a visible equivalent, because the user
  may have both disabled.
- Never use haptics for error states alone — pair with visible text.

## 12. Writing

Samsung's three writing principles:

1. **Focused and purposeful** — lead with what matters to the user, cut
   everything else. One idea per sentence.
2. **Simple and human** — plain conversational language, no jargon, no
   internal system vocabulary leaking into the UI.
3. **Empowering and engaging** — tell the user what they *can* do. Frame
   errors around the next action, not the failure.

Mechanics: sentence case for everything except proper nouns. No terminal
period on single-sentence labels, buttons or toasts. Buttons are verbs
("Delete", "Turn on"), never "OK"/"Yes"/"No" for consequential actions.
Second person ("your files"), active voice, present tense.

## 13. Accessibility

| Requirement | Threshold |
|---|---|
| Body text contrast | 4.5:1 |
| Large text (>= 18sp regular / 14sp bold) | 3:1 |
| UI component / graphical boundary | 3:1 |
| Touch target | 48dp (44pt iOS) |
| Text scaling | Must survive 200% without loss of content or function |

Plus: a logical and stable focus order; every control reachable by keyboard or
switch; screen-reader labels on every interactive element; state (selected,
expanded, disabled) exposed programmatically, not just visually; never colour
alone to convey meaning; content reflows at 320dp width equivalent.
