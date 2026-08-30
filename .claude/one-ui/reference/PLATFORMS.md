# Platform Detection and Translation

These skills work on four families of codebase. Detect the platform first —
every rule below is expressed in dp, and how you apply it changes per stack.

## Detection

Look for these markers, in order. Stop at the first confident match. A repo
may legitimately match more than one (a monorepo with a web app and a mobile
app) — in that case treat each top-level app directory as its own target and
say so in the report.

| Platform | Markers |
|---|---|
| **Android (Views/XML)** | `build.gradle(.kts)`, `AndroidManifest.xml`, `res/layout/*.xml`, `res/values/*.xml` |
| **Android (Compose)** | above, plus `androidx.compose` in dependencies, `@Composable` functions, `.kt` files with `Modifier` |
| **iOS (SwiftUI)** | `*.xcodeproj` / `Package.swift`, `.swift` files with `View` conformance and `body` |
| **iOS (UIKit)** | `*.xcodeproj`, `.storyboard` / `.xib`, `UIViewController` subclasses |
| **Web** | `package.json`; then narrow: `next.config.*` → Next.js, `vite.config.*` + `.jsx/.tsx` → React, `.vue` → Vue, `svelte.config.js` → Svelte, `angular.json` → Angular |
| **Web styling** | `tailwind.config.*` → Tailwind, `*.module.css` → CSS Modules, `styled-components` / `emotion` in deps → CSS-in-JS, plain `.css`/`.scss` → stylesheets |
| **Cross-platform** | `pubspec.yaml` → Flutter; `react-native` in deps → RN; `capacitor.config.*` → Capacitor |

If detection is ambiguous, ask the user rather than guessing. A wrong platform
guess produces patches that do not apply.

## Where One UI applies fully vs. partially

Be honest about this in every report.

- **Android** — One UI is a native fit. The `tribalfs/oneui-design` library
  provides real components (`ToolbarLayout`, `DrawerLayout`, `SemToast`,
  `TipPopup`, `RoundedLinearLayout`, `BottomTabLayout`). Recommend the library
  where the project can take the dependency; otherwise reproduce the tokens.
- **iOS** — One UI's *principles* (viewing/interaction split, generous type,
  pill buttons, role-based colour, restrained depth) transfer cleanly. Its
  *components* do not, and should not be forced. Never recommend replacing a
  `UINavigationBar` with a Samsung app bar clone on iOS; instead apply the
  large-title behaviour iOS already has, tuned to One UI's type scale and
  spacing. Say plainly where a rule is being adapted rather than applied.
- **Web** — One UI translates well as a token system: CSS custom properties
  for colour roles, the 24px keyline, the 589/960 breakpoints, the pill
  radius, the real bezier curves. Component-level mimicry (drawing a Samsung
  app bar in HTML) is usually a mistake unless the product is deliberately a
  Galaxy-companion experience.
- **Flutter / React Native** — treat as Android-leaning. Tokens apply
  directly; components map to the framework's own primitives.

## Token translation

### Android (Views)
Write tokens into `res/values/dimens.xml`, `colors.xml`, `themes.xml`. Use
`sp` for text, `dp` for everything else. Use `values-night/` for dark, and
`values-sw600dp/` for large screens. Reference colours by `?attr/` or
`@color/` role name, never inline hex in layouts.

### Android (Compose)
Define a `OneUiTheme` with `Typography`, `ColorScheme` and a custom
`Dimens`/`Shapes` object provided via `CompositionLocal`. Use `.dp` / `.sp`.
Read `isSystemInDarkTheme()`.

### SwiftUI
Put tokens in an `enum OneUI { enum Spacing / Radius / Duration }` and colours
in an asset catalog with Any/Dark appearances so they resolve automatically.
Use `.font(.system(size:))` only if Dynamic Type is preserved via
`@ScaledMetric`; prefer semantic text styles mapped to the One UI scale.

### UIKit
Same asset catalog approach for colour. Use `UIFontMetrics` for scaling.
Layout constants in a single `OneUIMetrics` struct, never scattered literals.

### Web (CSS custom properties)
This is the preferred shape for web:

```css
:root {
  --oui-keyline: 24px;
  --oui-radius-md: 12px;
  --oui-radius-pill: 26px;
  --oui-text-body: 1.0625rem;   /* 17px */
  --oui-text-title: 1.125rem;   /* 18px */
  --oui-ease-standard: cubic-bezier(0.4, 0, 0.2, 1);
  --oui-ease-decelerate: cubic-bezier(0.22, 0.25, 0, 1);
  --oui-duration-standard: 167ms;
  --oui-bg: #FFFFFF;
  --oui-text-primary: #252525;
}
@media (prefers-color-scheme: dark) {
  :root { --oui-bg: #000000; --oui-text-primary: #FAFAFA; }
}
```

### Tailwind
Extend the theme rather than scattering arbitrary values:

```js
theme: {
  extend: {
    spacing:      { keyline: '24px' },
    borderRadius: { 'oui': '12px', 'oui-pill': '26px' },
    fontSize:     { 'oui-body': ['1.0625rem', '1.5'] },
    screens:      { 'oui-md': '589px', 'oui-lg': '960px' },
    transitionTimingFunction: { 'oui': 'cubic-bezier(0.4,0,0.2,1)' },
  }
}
```
Flag `w-[23px]`-style arbitrary values in the audit — off-scale magic numbers
are the most common web violation.

## What never to do

- Do not add a dependency the project cannot build with. Check the existing
  dependency list and minimum SDK / browser targets first.
- Do not convert a design system wholesale in one patch. Sequence it: tokens →
  primitives → screens.
- Do not claim One UI conformance for a platform where you have adapted rather
  than applied a rule. Mark it as adapted.
