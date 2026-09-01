# One UI Conformance Report

> **Historical.** This is a point-in-time report from 2026-08-30. Its seven
> Blockers were closed at the time, and the interface it describes has since
> changed substantially: the expanded overlay sheet, the sort chips, the
> reorder arrows and the `<Modal>` edit sheet it audits no longer exist, and
> the floating list, numbered rows, size slider, permissions wall and
> drag-to-hide target it does not mention are new. Read it as a record of what
> was found and fixed, not as a description of the current UI. `PRODUCT.md`,
> `DESIGN.md` and `ONE-UI.md` are the live documents; re-run `/one-ui:audit`
> for a current score.

| | |
|---|---|
| **Project** | DevClip |
| **Platform** | React Native (Expo 57 / RN 0.86) — Android-leaning cross-platform |
| **Date** | 2026-08-30 |
| **Scope** | 14 files across `src/screens/`, `src/components/`, `src/theme/`, root `App.tsx`; excluded `src/db/`, `src/store/`, `src/native/`, `src/utils/`, `src/types/`, `plugins/`, `assets/`, generated files |
| **Method** | Manual code review of all UI-bearing files |

> **Platform note.** DevClip is a floating-overlay clipboard utility; it is never a standard full-screen activity. Some One UI structural rules (large title, bottom primary action) are adapted for the popup form factor and are noted as such rather than flagged as hard failures. Rules that apply regardless of form factor — spacing keyline, contrast, accessibility, motion — are applied in full.

## Overall: 54/100 — Partially conformant

Multiple Blockers are present across Layout (24dp keyline), Color (contrast), Motion (reduce-motion), and Accessibility (four distinct Blockers). The cap of 74 is academic — the weighted mean is already 54.

| # | Area | Status | Score | Blocker | Major | Minor |
|---|---|---|---|---|---|---|
| 1 | Structure | Findings | 60 | 0 | 2 | 2 |
| 2 | Layout | Findings | 50 | 1 | 2 | 2 |
| 3 | Components | Findings | 78 | 0 | 1 | 2 |
| 4 | Color | Findings | 52 | 1 | 2 | 1 |
| 5 | Iconography | Findings | 82 | 0 | 1 | 2 |
| 6 | Motion | Findings | 55 | 1 | 1 | 2 |
| 7 | Sound & Haptic | N/A | — | — | — | — |
| 8 | Writing | Findings | 78 | 0 | 1 | 3 |
| 9 | Accessibility | Findings | 18 | 4 | 4 | 2 |

---

## Blockers

### LAY-01 · Primary horizontal margin below the 24dp One UI keyline
**Where:** `src/theme/theme.ts:77` (`spacing.md = 14`); used as `marginHorizontal` in `ClipListView.tsx` (captureBtn, errorBanner), `SearchBar.tsx` (wrap), `ClipListItem.tsx` (card), `PopupScreen.tsx` (hero padding).
**What:** Every major interactive surface uses 14dp side margins. The One UI keyline is 24dp on both sides — below that, content conflicts with the curved-edge Reject and Grip zones and may be unreliable to touch.
**Expected:** `spacing.md` raised to 24dp for screen-edge margins, or a separate `keyline` token at 24dp used wherever content is inset from the edge.
**Fix:** Add `keyline: 24` to the spacing map; replace `marginHorizontal: spacing.md` with `marginHorizontal: spacing.keyline` at the screen-edge positions.

---

### CLR-01 · `inkFaint` text fails 4.5:1 contrast in light mode
**Where:** `src/theme/theme.ts:47` (`inkFaint: '#9C9CA3'`); applied in `ClipListView.tsx:67` (empty-state text, 12.5sp), `ClipListItem.tsx:43` (date metadata, 10sp), `PopupScreen.tsx` (inactive tab labels, 11sp), `SettingsScreen.tsx` (note text, 12sp).
**What:** `#9C9CA3` on `#F4F4F6` = **approximately 2.48:1** — far below the 4.5:1 WCAG AA threshold for small text. The empty-state message is the primary content a user sees when the list is empty; it must be readable.
**Expected:** Body and informational text at 4.5:1 or above. For secondary metadata that can be treated as large text (≥ 18sp regular), 3:1 is the floor.
**Fix:** Darken `inkFaint` in light mode to approximately `#767676` (#767676 on #F4F4F6 ≈ 4.6:1), or use `inkSoft` (#5A5A63, ≈6.0:1) for text the user actually needs to read.

---

### MOT-01 · No reduce-motion handling
**Where:** `src/components/Pressy.tsx:48` — `Animated.spring` fires on every button press throughout the app.
**What:** The scale-spring animation plays unconditionally. There is no check for `AccessibilityInfo.isReduceMotionEnabled` (Android: `ANIMATOR_DURATION_SCALE`, iOS: `UIAccessibility.isReduceMotionEnabled`). Users who need reduced motion get no alternative.
**Expected:** Read `AccessibilityInfo.isReduceMotionEnabled()` on mount and subscribe to changes; skip or replace the spring with an instant state change when true.
**Fix:**
```tsx
import { AccessibilityInfo } from 'react-native';
// inside Pressy:
const [reduceMotion, setReduceMotion] = useState(false);
useEffect(() => {
  AccessibilityInfo.isReduceMotionEnabled().then(setReduceMotion);
  const sub = AccessibilityInfo.addEventListener('reduceMotionChanged', setReduceMotion);
  return () => sub.remove();
}, []);
// then: if (reduceMotion) return; // skip spring
```

---

### A11Y-01a · `ThreeWayPill` size options "S"/"M"/"L" have no useful accessible name
**Where:** `src/screens/SettingsScreen.tsx` — `ThreeWayPill` for Bubble size with labels "S", "M", "L"; no `accessibilityLabel` on the `Pressy` wrappers.
**What:** TalkBack announces "S", "M", "L" with no context. A screen reader user cannot tell what "S" selects.
**Expected:** Each option carries a full accessible name: e.g. `accessibilityLabel="Bubble size: Small"`.
**Fix:** Add `accessibilityLabel={`Bubble size: ${opt.label}`}` (or equivalent descriptive label) to each `Pressy` in `ThreeWayPill`, and `accessibilityState={{ selected: active }}`.

---

### A11Y-03 · Same contrast failure as CLR-01
**Where:** Same as CLR-01 above.
**WCAG criterion:** 1.4.3 Contrast (Minimum).
**Fix:** Same as CLR-01.

---

### A11Y-04 · Fixed-height containers clip text at 200% font scale
**Where:** `src/screens/PopupScreen.tsx:94` (`gearBtn` 44×44px), `PopupScreen.tsx:131` (`tab minHeight: 38`), `src/components/SortMenu.tsx:24` (`chip minHeight: 32`).
**What:** At system font scale 200%, the text inside these fixed containers is clipped or invisible. `minHeight` prevents the container growing with text; there is no `flexWrap` or dynamic sizing fallback.
**Expected:** Containers grow to fit their content. Either remove fixed heights (rely on padding) or use `minHeight` only — which is correct — but also ensure all children can grow by not capping with `height`.
**Fix:** Audit every `height:` and `minHeight:` that contains a `Text` child. Remove `height:` and keep `minHeight:` so the container can grow. Add `flexShrink: 0` if needed to prevent collapse.

---

### A11Y-06 · Long-press is the only path to edit or delete a clip
**Where:** `src/components/ClipListItem.tsx:65` — `onLongPress={() => onLongPress(clip)}`.
**What:** There is no button, menu item, or swipe-to-reveal action that reaches the edit/delete sheet except a long-press gesture. Users on switch control or who cannot perform a long press cannot edit or delete any clip.
**Expected:** An alternative — a row menu button, a swipe action via `react-native-gesture-handler`, or an explicit "Edit" button visible or accessible on the card.
**Fix:** Add a small "..." or edit icon button to the card row (accessible, 48dp target) that opens the same `EditClipModal`. The long-press can remain as a shortcut.

---

## Findings by area

### 1. Structure — 60/100 · Findings

> **Popup-form-factor note.** The app runs as a floating overlay 300–360dp wide. The large-title and bottom-action-bar rules are noted as adapted rather than applied, because a 360dp-wide overlay with a 640dp height is not a standard phone screen. STR-01 and STR-02 below are genuine issues even in the popup context; STR-03 is adapted.

| ID | Sev | Location | Finding | Expected |
|---|---|---|---|---|
| STR-01 | Blocker (adapted) | `PopupScreen.tsx:78` | "Capture current clipboard" — the app's primary action — sits at the top of the content area, above the search bar | Primary action in the interaction area (bottom); even in a popup, it should be the first thing the thumb reaches, not the first thing the eye does |
| STR-02 | Major | `PopupScreen.tsx:80–146` | Header zone mixes app identity, view-mode tabs, settings nav, and a bubble toggle; no clear viewing vs. interaction split | Separate what the user reads (app name, clip list) from what they touch (tabs, bubble, settings) |
| STR-03 | Major (adapted) | `PopupScreen.tsx:100` | Fixed 17sp "DevClip" title; no large or collapsing app bar | On a full-screen layout: large collapsing title. In popup context: intentional adaptation — noted, not scored as a hard failure |
| STR-06 | Minor | `PopupScreen.tsx:108–145` | Header row presents settings gear, bubble toggle, and three view-mode tabs as co-equal controls | One primary header action; secondary actions in overflow or a separate settings surface |

---

### 2. Layout — 50/100 · Findings

| ID | Sev | Location | Finding | Expected |
|---|---|---|---|---|
| LAY-01 | **Blocker** | `theme.ts:77`; applied in `ClipListView.tsx`, `SearchBar.tsx`, `ClipListItem.tsx`, `PopupScreen.tsx` | `spacing.md = 14` used as primary screen-edge margin; 14dp < 24dp keyline | 24dp minimum from each screen edge |
| LAY-07 | Major | `SortMenu.tsx:24` (chip 32dp), `PopupScreen.tsx:131` (tab 38dp), `SettingsScreen.tsx` (pill ~28dp) | Multiple touch targets below 48dp; only SortMenu chips have `hitSlop: 4` (net 40dp — still below 48dp) | All interactive elements ≥ 48dp touch target; use `hitSlop` to achieve this without changing visual size |
| LAY-05 | Major | `PopupScreen.tsx:56` (Full App mode) | "Full App" mode sends `width: -1, height: -1` (match_parent) but renders the same single-column layout; no adapted layout for the expanded canvas | On large / full-screen form, widen content, show more clips per row, or adapt navigation |
| LAY-08 | Minor | `theme.ts:73–81` | Spacing tokens `md: 14`, `xl: 28`, `xxl: 40` fall outside the One UI 2dp-resolution scale (4, 8, 12, 16, 20, 24) | Align to scale: md → 12 or 16, xl → 28 is close to 24 (use 24), xxl → 40 is acceptable |
| LAY-09 | Minor | `PopupScreen.tsx:94` (gearBtn 44px), `PopupScreen.tsx:131` (tab minHeight 38px) | Fixed pixel heights on containers with text children; will clip at 200% scale | `minHeight` only (not `height`); let padding drive normal height |

---

### 3. Components — 78/100 · Findings

| ID | Sev | Location | Finding | Expected |
|---|---|---|---|---|
| CMP-04 | Major | `PopupScreen.tsx:100` | No collapsing/expanded app bar; header is a fixed-height zone that does not respond to scroll | Collapsing `LargeTopAppBar` equivalent; adapted for popup as a header that shrinks when content scrolls (optional given popup constraints) |
| CMP-10 | Minor | `PopupScreen.tsx:119` (`bubbleBtnText`), `SettingsScreen.tsx:134,147` | "On" / "Off" / "Bubble on" / "Bubble off" used as button labels for toggles | Labels should describe the action ("Turn on" / "Turn off") or use an accessible switch role with state rather than a button |
| CMP-13 | Minor | `ClipListView.tsx:99` | `ListEmptyComponent` says "No clips yet" even when the list is empty due to an active search filter | Distinguish "empty list" from "no search results": show "No clips match '{search}'" when `search` is non-empty |

---

### 4. Color — 52/100 · Findings

| ID | Sev | Location | Current | Expected |
|---|---|---|---|---|
| CLR-01 | **Blocker** | `theme.ts:47`; `ClipListView.tsx:67`, `ClipListItem.tsx:43`, `PopupScreen.tsx` tabs | `inkFaint: '#9C9CA3'` on `bg: '#F4F4F6'` ≈ 2.48:1 | Body text ≥ 4.5:1; metadata ≥ 3:1 |
| CLR-04 | Major | `App.tsx:30,32`; `Pressy.tsx:61`; `SettingsPanel.tsx` (all styles) | `#3D4CF0`, `#F4F4F6`, `rgba(128,128,128,0.15)`, `#4a6cf7`, `#888`, `#333`, `#ddd` hardcoded in component files | All colours referenced via theme tokens; `SettingsPanel.tsx` should be migrated to `useTheme()` |
| CLR-07 | Major | `theme.ts:38` | `accent: '#3D4CF0'` hard-coded brand constant | Treat accent as a runtime value; on Android this means reading `WallpaperColors` or providing a user-selectable palette in Settings. Document the brand lock as an intentional divergence if the brand requires it |
| CLR-09 | Minor | `theme.ts:57` | Dark-mode `ink: '#F2F2F4'` vs One UI `#FAFAFA` | Use `#FAFAFA` for primary dark-mode text to reduce halation on AMOLED; `#F2F2F4` is close but slightly warm-grey |

---

### 5. Iconography — 82/100 · Findings

All icons are from `lucide-react-native` (vector, single-color, tintable). No raster icons; no mixed libraries.

| ID | Sev | Location | Finding | Expected |
|---|---|---|---|---|
| ICN-05 | Major | `SettingsPanel.tsx:58` | `backgroundColor: '#4a6cf7'` on the icon button; entire component bypasses the theme | Migrate `SettingsPanel` to `useTheme()` or remove it (it appears to be superseded by `SettingsScreen`) |
| ICN-04 | Minor | `OnboardingScreen.tsx:28` (`ArrowRight strokeWidth={2}`), `EditClipModal.tsx:~check` (`Check strokeWidth={2}`), vs. majority at 1.5/1.75 | Mixed stroke weights: 1.5, 1.75, 2 in the same product | Pick one weight (1.5 or 1.75) and apply uniformly; use 2 only for emphasis icons that need to be heavier |
| ICN-09 | Minor | `ClipListItem.tsx` `iconWrap` (Copy icon inside a tappable card) | Decorative icon container has no `accessibilityElementsHidden` / `importantForAccessibility="no"` | Hide purely decorative icons from AT |

---

### 6. Motion — 55/100 · Findings

| ID | Sev | Location | Current | Expected |
|---|---|---|---|---|
| MOT-01 | **Blocker** | `Pressy.tsx:48` | Spring animation fires on every press with no reduce-motion check | Check `AccessibilityInfo.isReduceMotionEnabled`; skip or replace animation when true |
| MOT-04 | Major | `Pressy.tsx:49` | `Animated.spring({ speed: 40, bounciness: 6 })` — physics-based, no One UI easing curves defined anywhere | Define easing tokens (`standard: 0.4,0,0.2,1`; `decelerate: 0.22,0.25,0,1`) even if only for future use; press feedback can use `Easing.bezier(0.4,0,0.2,1)` in `timing` |
| MOT-10 | Minor | `Pressy.tsx:49` | `bounciness: 6` introduces slight overshoot on every routine button press | Reserve overshoot for confirmation moments (success state, onboarding completion); routine presses should use `bounciness: 0` or a very low value |
| MOT-11 | Minor | — | No predictive-back gesture handling on RN 0.86 | Implement `BackHandler` with progress tracking or the RN predictive-back API when stable |

---

### 7. Sound & Haptic — N/A

No audio or haptic APIs used anywhere in the codebase. The app relies on system touch feedback (Android ripple, system button sounds). This is appropriate for a clipboard utility. No findings.

Worth revisiting if detents are added to the view-size switcher or if paste confirmation gains a tactile cue.

---

### 8. Writing — 78/100 · Findings

| ID | Sev | Location | Current copy | Suggested rewrite |
|---|---|---|---|---|
| WRT-03 | Major | All component files | All user-visible strings are hardcoded in `.tsx` files | Move to a `strings.ts` resource file; eventually adopt i18n (react-i18next or expo-localization) |
| WRT-04 | Minor | `PopupScreen.tsx:32` | Tab label "Full App" — Title Case | "Full app" |
| WRT-10 | Minor | `OnboardingScreen.tsx:79` | "Three quick permissions and you're set." | "Grant three permissions and you're ready to go." or simply "Grant these permissions to finish setup." |
| WRT-13 | Minor | `SettingsScreen.tsx` handleClearAll, `EditClipModal.tsx` handleDelete | "This cannot be undone." (passive) | "You can't undo this." |

---

### 9. Accessibility — 18/100 · Findings

| ID | Sev | Location | Issue | WCAG | Fix |
|---|---|---|---|---|---|
| A11Y-01 | **Blocker** | `SettingsScreen.tsx` `ThreeWayPill` (bubble size "S"/"M"/"L") | Pill options have no `accessibilityLabel`; TalkBack reads meaningless single letters | 4.1.2 | `accessibilityLabel="Bubble size: Small"` etc. on each `Pressy` |
| A11Y-03 | **Blocker** | `theme.ts:47`; `ClipListView.tsx:67`, `ClipListItem.tsx:43` | `inkFaint` contrast ≈ 2.48:1 on light background | 1.4.3 | Same fix as CLR-01 |
| A11Y-04 | **Blocker** | `PopupScreen.tsx:94,131`; `SortMenu.tsx:24` | Fixed-height containers clip text at 200% scale | 1.4.4 | Use `minHeight` + padding only; verify with Accessibility Inspector at 200% scale |
| A11Y-06 | **Blocker** | `ClipListItem.tsx:65` | Long-press is the only path to edit/delete | 2.1.1 | Add an accessible edit/menu affordance to each list card |
| A11Y-07 | Major | `SettingsScreen.tsx:134,147` | autoStartOnBoot and confirmBeforePaste toggles expose no `accessibilityState` | 4.1.2 | `accessibilityState={{ checked: autoStartOnBoot }}` + `accessibilityRole="switch"` |
| A11Y-10 | Major | `SortMenu.tsx:24` (32dp), `PopupScreen.tsx:131` (38dp), `SettingsScreen.tsx` pills (~28dp) | Touch targets below 48dp | 2.5.5 | Expand with `hitSlop` to reach 48dp total, or increase the visual size |
| A11Y-11 | Major | `theme.ts`, all `fontSize:` values | Font sizes are hardcoded numbers with no verified Dynamic Type accommodation | 1.4.4 | Confirm RN's default font scaling is active (check `allowFontScaling` not set to false anywhere); verify at 200% with Accessibility Inspector |
| A11Y-12 | Major | `SearchBar.tsx:23` | Placeholder "Search title or content" is the only label; disappears on input | 3.3.2 | Add a persistent visible label above the field, or confirm `accessibilityLabel` is announced by the OS when the field is focused (verify manually) |
| A11Y-16 | Minor | `ClipListItem.tsx` iconWrap | Decorative Copy icon inside a tappable card not hidden from AT | 1.1.1 | Add `importantForAccessibility="no"` to the icon container |
| A11Y-17 | Minor | `ClipListView.tsx` | `accessibilityLiveRegion="polite"` on the container View is correct, but toast/alert messaging does not use a live region | 4.1.3 | Verify error banner and success alerts are announced without stealing focus |

---

## What this codebase already does well

- **Accessible press targets on primary surfaces.** `Pressy` forwards all accessibility props and the main interactive elements — clip cards, settings gear, bubble toggle, dialog confirmation buttons — all have `accessibilityLabel`, `accessibilityRole`, and `accessibilityState` where appropriate.
- **Theme system is well-shaped.** `ThemeContext` uses `useColorScheme()` to follow the system, supports a user override ("Light"/"Dark"/"Auto"), exposes a typed `ThemeColors` interface, and is consumed consistently across all modern components. The foundation for One UI token adoption is solid.
- **Dark mode is implemented and correct in direction.** Near-black backgrounds (`#0E0E11`) are good for AMOLED; surface tone separates cards from background by tone rather than shadow (the `surface`/`bg` split is correct One UI thinking).
- **Button labels are verbs.** Alert dialogs — paste confirm, clear all, delete — use verb labels ("Paste", "Clear all", "Delete") rather than "OK"/"Yes". Correct One UI writing.
- **Settings screen is well-grouped.** "Permissions", "Appearance", "Capture", "Storage" sections with visual card containers — correct One UI grouping pattern.
- **Pill-shaped buttons throughout.** `radii.pill = 999` is used consistently on action buttons, matching the One UI 26dp pill requirement.
- **Icon accessibility is largely correct.** Most icon-only buttons carry `accessibilityLabel`; the `handle` in `EditClipModal` has `importantForAccessibility="no"`.
- **Error states have next actions.** "Could not paste automatically, so it's on your clipboard — paste it manually." is a good One UI error pattern.
- **Empty state is an invitation.** "No clips yet / Copy something, then tap Capture above." follows the One UI writing principle.

---

## Divergences worth keeping

| Where | Divergence | Why it should stay |
|---|---|---|
| `theme.ts` | Brand accent `#3D4CF0` instead of system/user accent | Product identity; acceptable as a documented divergence. Consider offering system-accent opt-in as a future settings toggle rather than forcing it now |
| `Pressy.tsx` | Spring physics press animation instead of bezier timing | The "physical" press metaphor is a deliberate design choice and distinguishes DevClip from a generic clipboard tool. Fix the reduce-motion gap, not the spring itself |
| `OnboardingScreen.tsx` | Full-screen setup flow rather than a One UI permission sheet | Android permission flows are system-presented; the custom onboarding screen bridges them well and is appropriate for a tool with non-obvious permissions |
| `PopupScreen.tsx` | Flat header with tabs rather than a large-title collapsing app bar | The popup window (300–360dp wide) is too narrow for a One UI large title to be meaningful. The current compact header is a reasonable adaptation |

---

## Needs manual verification

- **Contrast in dark mode.** `inkFaint` (#75757E on #0E0E11) computes close to the 4.5:1 threshold; verify with a colour contrast tool using the exact resolved values.
- **Font scaling at 200%.** Verify all screens on an Android device with font scale at 200% — especially the header tab bar and sort chips. Use Accessibility Inspector or Developer Options.
- **TalkBack flow through ClipListView.** Are clip cards announced usefully? Is the error banner read when it appears?
- **Predictive back.** On Android 14+ with gesture nav, check whether the back gesture on the Settings screen (which uses `BackHandler`) feels native or skips the system animation.
- **`SettingsPanel.tsx` liveness.** This component appears to be a legacy component replaced by `SettingsScreen`. If it is no longer rendered anywhere, remove it to eliminate the unthemed dead code. If it is still rendered, migrate it to `useTheme()` immediately.

---

## Recommended sequence

1. **Blockers** — 7 total (LAY-01, CLR-01, MOT-01, A11Y-01, A11Y-03, A11Y-04, A11Y-06). Address in this order: contrast fix first (affects every screen), then keyline (affects all layout), then accessibility (long-press, size labels, fixed heights), then reduce-motion.
2. **Token cleanup** — Raise `spacing.md` to 24dp (`keyline`); align `xl`/`xxl` to the One UI scale; darken `inkFaint`. All other tokens are structurally sound.
3. **`SettingsPanel.tsx`** — Delete or migrate. It bypasses the theme entirely and is a likely dead code path.
4. **Touch targets** — Apply `hitSlop` consistently to reach 48dp on sort chips, tabs, and action pills. No visual change needed.
5. **Motion tokens** — Define One UI easing constants. Patch the reduce-motion gap first, then optionally convert the spring to a `timing` animation for more predictable One UI choreography.
6. **Accessible state on toggles** — Add `accessibilityRole="switch"` and `accessibilityState={{ checked }}` to On/Off controls.
7. **Search no-results state** — A one-line conditional on `search` in `ClipListView`.
8. **Copy to strings resource** — Low urgency but important for long-term maintainability and eventual localisation.

Run `/one-ui:redesign` to work through token patches and component fixes with diffs for approval.
