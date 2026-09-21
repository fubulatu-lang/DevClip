# DevClip

A native Android app. Kotlin throughout — Jetpack Compose for the launcher
app, ordinary views for the floating bubble and the list it opens.

## It used to be React Native

It is not any more, and nothing should reintroduce it. The floating list was
a React surface, and a surface draws nothing without a live React instance
behind it — nothing starts one but an Activity, so after a reboot the bubble
opened an empty window until the app had been opened once. That is why the
UI is native: not preference, a bug that could not be fixed any other way.

Earlier revisions of this file told you to read the Expo SDK docs before
writing any code. There is no Expo here now.

## Where things are

- `app/src/main/java/com/devclip/app/` — everything.
  - `ui/` — Compose: the theme, the clip list, settings, the edit sheet.
  - `OverlayService.kt` — the bubble, the floating list, the drag-to-hide
    target. Views, not Compose, and deliberately so: it draws into windows a
    service owns, where Compose needs lifecycle owners wired by hand and
    fails by rendering nothing.
  - `DevClipTheme.kt` — the design system. Every surface reads it, including
    the Compose theme, which converts rather than restates it.
- `.claude/one-ui/reference/TOKENS.md` — where the design values come from.

## The design system

Samsung One UI, in monochrome. Black on white, white on black, no accent.
The three functional colours — success, caution, error — are the only hues,
because One UI requires them to be distinguishable and forbids colour being
the only thing that distinguishes them. They are never decorative.

Spacing, radii, type and motion follow the published One UI values. A 4dp
button radius or a 13sp body size is the clearest sign something has drifted
back towards Material.

## Every change that ships bumps the version

Both numbers in `app/build.gradle.kts`, in the same commit as the change
itself — never as a follow-up, because a follow-up is a thing that gets
forgotten and then the APK on the phone and the APK on the release page
share a name while being different software.

- `versionName` is what the user reads. Bump the **minor** for a new
  feature, the **patch** for a fix or an adjustment to one that exists.
- `versionCode` goes up by one every single time, whichever the above was.
  It is the only number Android compares when deciding whether an install
  is an update, and it must never repeat or go backwards.

The workflow reads `versionName` out of that file to name the APK, so the
name on the release page cannot disagree with what is inside it. Settings
shows the installed version at the bottom of the list, read from the
package manager. Those two are how the user answers "is the thing I
installed the thing I downloaded", and they are only worth anything if the
number actually moves.

## Two things that are easy to get wrong

**The accessibility permission switch and the accessibility service running
are different facts.** They come apart routinely on Samsung. Ask
`OverlayController.isCaptureWorking()`, which checks the service instance,
not `isAccessibilityEnabled()`, which checks the switch. Reporting the switch
is how the app once spent weeks insisting everything was fine while nothing
was listening.

**The bubble floats over other people's apps.** Anything drawn on it has to
be legible against a background nobody controls — which is why the selection
ring is a dark stroke outside a white one rather than any single colour.
