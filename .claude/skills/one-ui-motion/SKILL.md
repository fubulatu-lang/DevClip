---
name: one-ui-motion
description: Review or apply Samsung One UI motion — the real easing curves, duration scale, transition choreography, gesture-driven and predictive-back motion, and reduce-motion compliance. Use when animations feel wrong for One UI or when the user runs /one-ui:motion.
argument-hint: "<file, directory, or animation to review>"
---

# One UI — Motion

Read `TOKENS.md` §6, and `PLATFORMS.md`, before starting.

One UI motion has a specific character: **quick, decelerating, and physically
plausible**. It is not bouncy by default and it is not slow. Samsung's stated
aim is that motion strings actions together "without interruptions" — motion
exists to preserve continuity, never to entertain.

## The real curves

These are the actual cubic beziers from the One UI design library. They port
directly to CSS `cubic-bezier()` and Core Animation timing functions.

| Name | Bezier | Use |
|---|---|---|
| `standard` | `0.4, 0, 0.2, 1` | Screen and container transitions |
| `sine-in-out-33` | `0.33, 0, 0.67, 1` | Symmetric ease, state changes |
| `sine-in-out-60` | `0.33, 0, 0.4, 1` | Slightly decelerated |
| `emphasized-decelerate` | `0.22, 0.25, 0, 1` | Elements entering the screen |
| `back-gesture` | `0.1, 0.1, 0, 1` | Predictive back progress |
| `drawer-settle` | `0, 0, 0, 1` | Drawers and panels settling |
| `overshoot` | overshoot | Playful confirmation, used sparingly |
| `elastic-50` | SESL elastic | Tip popups and attention cues only |

**Ease-out dominates.** Things entering decelerate into place; things leaving
can accelerate away. A symmetric `ease-in-out` on an entering element is a
common mismatch.

`linear` is only correct for progress indicators and continuous loops.

## The duration scale

| Token | Value | Use |
|---|---|---|
| `instant` | 100ms | Ripple, checkbox flip, hover |
| `short` | 120ms | Micro-feedback |
| `standard` | 167ms | Most UI transitions |
| `medium` | 200ms | Reveal, expand, collapse |
| `emphasized` | 260ms | App bar collapse, larger containers |
| `long` | 400ms | Full-screen or shared-element transitions |
| `extended` | 500ms | Onboarding and complex choreography |

Nothing routine exceeds 500ms. Durations like `300ms` and `1000ms` are the
usual imported-from-elsewhere smell — `300ms` is Material's default and
`1000ms` is almost always too slow to feel responsive.

Scale duration with distance and size: a small chip animating 20dp should not
take as long as a sheet travelling the screen height.

## Choreography

- **Continuity over spectacle.** When a card becomes a screen, the card should
  visibly become the screen. Cross-fading two unrelated states loses the
  thread.
- **One focal point.** Do not animate six things at once with equal weight.
  Stagger related items by roughly 20–30ms, and cap the stagger so a long list
  does not take a second to appear.
- **Motion follows the gesture.** Drawer, sheet and back animations should
  track the finger 1:1 and settle with `drawer-settle` on release. A gesture
  that plays a fixed-duration animation regardless of drag position feels
  broken.
- **Predictive back** — where the platform supports it, the outgoing screen
  should respond to back-gesture progress with the `back-gesture` curve.
- Animate `transform` and `opacity` only, on web. Animating `width`,
  `height`, `top` or `left` causes layout thrash and dropped frames.

## Reduce motion

Every animation must have a reduced path. This is not optional and it is not
"disable all animation" — the correct reduced behaviour is usually a
cross-fade or an instant state change, never a broken layout.

- **Web** — `@media (prefers-reduced-motion: reduce)`
- **iOS** — `UIAccessibility.isReduceMotionEnabled` / `@Environment(\.accessibilityReduceMotion)`
- **Android** — `Settings.Global.ANIMATOR_DURATION_SCALE` and
  `TRANSITION_ANIMATION_SCALE`

Also: no auto-playing motion longer than 5 seconds without a pause control,
and nothing flashing more than three times per second.

## Checks

| ID | Severity | Check |
|---|---|---|
| `MOT-01` | Blocker | No reduce-motion handling anywhere in the codebase |
| `MOT-02` | Blocker | Flashing content above 3Hz, or unstoppable motion over 5s |
| `MOT-03` | Major | Durations off the scale — especially 300ms defaults or anything over 500ms |
| `MOT-04` | Major | Default/linear easing on UI transitions instead of a One UI curve |
| `MOT-05` | Major | Symmetric ease-in-out on entering elements (should decelerate) |
| `MOT-06` | Major | Gesture-driven surfaces playing fixed animations instead of tracking the finger |
| `MOT-07` | Major | Animating layout properties (`width`/`height`/`top`/`left`) on web |
| `MOT-08` | Minor | Unstaggered mass animation, or stagger long enough to feel slow |
| `MOT-09` | Minor | Transitions that cross-fade where a shared element would preserve continuity |
| `MOT-10` | Minor | Bounce/elastic used on routine transitions |
| `MOT-11` | Minor | No predictive-back handling on platforms that support it |

## Platform notes

- **Android Views** — `PathInterpolator(0.4f, 0f, 0.2f, 1f)` etc., or the One
  UI library's `CachedInterpolatorFactory`. `MotionLayout` or
  `TransitionManager` for choreography. `OnBackAnimationCallback` for
  predictive back.
- **Compose** — `CubicBezierEasing(0.4f, 0f, 0.2f, 1f)`, `tween(167, easing =
  ...)`, `SharedTransitionLayout` for continuity, `AnimatedVisibility` with
  matched enter/exit.
- **SwiftUI** — `Animation.timingCurve(0.4, 0, 0.2, 1, duration: 0.167)`,
  `matchedGeometryEffect` for continuity, `.transaction` to strip animation
  under reduce-motion.
- **Web** — CSS custom properties for curves and durations; prefer CSS
  transitions and the Web Animations API over JS tweening. `transform` and
  `opacity` only.

## Output

Report as `ID | severity | file:line | current duration/easing | expected`.
When fixing, define the curve and duration tokens first, then replace call
sites, then add the reduce-motion path.
