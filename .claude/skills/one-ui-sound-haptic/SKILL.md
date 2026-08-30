---
name: one-ui-sound-haptic
description: Review or apply Samsung One UI sound and haptic guidance — when to fire feedback, semantic haptic vocabulary, respecting silent mode and system volume, and always providing a visible equivalent. Use when auditing feedback behaviour or when the user runs /one-ui:sound-haptic.
argument-hint: "<file, directory, or interaction to review>"
---

# One UI — Sound & Haptic

Read `TOKENS.md` §11, and `PLATFORMS.md`, before starting.

Sound and haptics are the least-audited part of most codebases and the most
likely to annoy users when wrong. The governing rule is simple: **feedback
confirms, it does not announce.**

## When to fire feedback

Fire on:

- A state change the **user caused** — toggle flipped, item selected, refresh
  triggered, drag snapped, long-press recognised.
- A **completion** — save succeeded, send finished.
- An **error the user needs to notice**, paired with visible text.
- Reaching a **boundary** — end of a scroll, limit of a slider.

Do not fire on:

- Content arriving on its own (a notification is the system's job, not the
  app's haptic).
- Every tap. Blanket haptics on all touches deadens the signal and drains
  battery.
- Passive UI appearing — a screen loading is not an event the user did.
- Each step of a continuous gesture at high frequency. Throttle to meaningful
  detents.

## Use the semantic vocabulary, not raw durations

Every platform ships a named set of feedback types tuned to its hardware.
Calling `vibrate(50)` bypasses that tuning and feels wrong on most devices.

- **Android** — `HapticFeedbackConstants` (`CONFIRM`, `REJECT`,
  `LONG_PRESS`, `CLOCK_TICK`, `GESTURE_START`/`GESTURE_END`,
  `SEGMENT_TICK`), or `VibrationEffect` predefined effects. Samsung devices
  map these to their own haptic engine.
- **iOS** — `UIImpactFeedbackGenerator` (light/medium/heavy/soft/rigid),
  `UISelectionFeedbackGenerator` for scrubbing through options,
  `UINotificationFeedbackGenerator` (success/warning/error). Call `prepare()`
  before a likely event to avoid latency.
- **Web** — `navigator.vibrate()` only. Support is patchy, it does nothing on
  iOS Safari, and it is usually the wrong tool. Treat web haptics as an
  enhancement, never as the carrier of information.

Intensity should match consequence: a selection tick is light, a destructive
confirmation is heavier.

## Sound

- UI sound is **opt-in**, follows the system media/notification volume, and
  respects the ringer or silent switch. A UI sound that plays in silent mode
  is a Blocker.
- Never set a fixed absolute volume.
- Keep cues short (under ~500ms), quiet, and low in the mix.
- Never autoplay sound on page or app load.
- Use a small consistent set of cues, and use the same cue for the same
  meaning throughout.

## The redundancy rule

**Every sound and every haptic must have a visible equivalent.** The user may
be deaf, may have haptics disabled, may be in a noisy environment, or may
simply have the phone on a table. A form that signals validation failure only
by vibration is inaccessible.

Conversely, the visible state must not depend on the feedback firing — do not
gate a UI update on a haptic callback.

## Respecting user settings

- Provide an in-app toggle for sound and haptics if the app makes meaningful
  use of them.
- Honour the system haptic setting, do-not-disturb, and reduced-motion where
  it implies reduced feedback.
- Never re-enable feedback the user turned off, including after an update.

## Checks

| ID | Severity | Check |
|---|---|---|
| `SND-01` | Blocker | Sound plays despite silent mode / ringer switch |
| `SND-02` | Blocker | Haptic or sound is the only signal for an error or state change |
| `SND-03` | Major | Raw vibration durations instead of the platform's semantic constants |
| `SND-04` | Major | Feedback fired on passive events (content arriving, screen loading) |
| `SND-05` | Major | Blanket haptics on every tap or every scroll frame |
| `SND-06` | Major | Fixed absolute volume, or volume ignoring the system stream |
| `SND-07` | Minor | No user-facing toggle for a feedback-heavy app |
| `SND-08` | Minor | Inconsistent cue used for the same meaning in different places |
| `SND-09` | Minor | Sound autoplaying on launch or page load |
| `SND-10` | Minor | Haptic generator not prepared on iOS, causing perceptible latency |
| `SND-11` | Minor | Web code relying on `navigator.vibrate` for meaningful feedback |

## Platform notes

- **Android** — prefer `view.performHapticFeedback(...)`. Requires
  `VIBRATE` permission for `Vibrator` APIs; `performHapticFeedback` does not.
  Route UI sound through `AudioManager.playSoundEffect` or the
  `USAGE_ASSISTANCE_SONIFICATION` attribute so it follows system rules.
- **Compose** — `LocalHapticFeedback.current.performHapticFeedback(...)`.
- **iOS** — configure `AVAudioSession` with `.ambient` for UI sound so it
  mixes and respects the silent switch. Never use `.playback` for UI cues.
- **Web** — audio needs a user gesture to start; respect that rather than
  working around it. Keep audio cues optional and off by default.

## Output

Report as `ID | severity | file:line | trigger | what's wrong | expected`.
Where a codebase has no sound or haptics at all, say so — absence is usually
fine, and only worth flagging where a genuinely tactile interaction (a
picker, a slider with detents, a pull-to-refresh) would benefit.
