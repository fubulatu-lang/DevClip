# DevClip — Setup Guide (GitHub + Expo Website Only)

No terminal, no Codespaces, no Expo Go app. Just github.com and expo.dev,
with one unavoidable exception explained at the bottom.

---

## 1. Push the code to GitHub

1. Create a repo named `DevClip` on github.com.
2. Upload every file/folder from the unzipped project (drag-and-drop
   works on github.com's "upload files" page, or use the GitHub mobile app).
3. Make sure `.eas/workflows/build.yml` made it into the repo — this is
   the file that makes builds automatic.

## 2. Connect the repo to your Expo project

1. On expo.dev, open your **DevClip** project.
2. Go to **Project settings → GitHub → Connect**.
3. Authorize the Expo GitHub App and pick your `DevClip` repo.

That's it for wiring — you do **not** need to set up "Build triggers" on
that page anymore. The `.eas/workflows/build.yml` file already committed
to your repo does that job instead, and it travels with your code instead
of living only in dashboard settings.

## 3. The one unavoidable manual step: Android signing credentials

EAS Build needs to know how your app is going to be signed. There is
currently no "generate a fresh keystore" button on the Expo website — the
dashboard's Credentials page only accepts uploading a keystore file you
already have, which you don't. Generating one from nothing requires a
single command, run once, ever, from a command line:

```bash
eas credentials --platform android
```

Practically, this means either:
- Opening a **GitHub Codespace** on your repo just this once to run that
  one line (Codespaces is still browser-only — no computer needed), or
- Asking anyone with any terminal (a friend, a library computer) to run
  that one command against your Expo login — it only takes a few seconds
  and doesn't need your project files, just `npm install -g eas-cli && eas login && eas credentials --platform android`.

When prompted: profile → **development**, then choose to generate a new
keystore. After this one-time step, EAS stores the keystore permanently on
its own servers — every future push-triggered build reuses it
automatically. You will never need to touch a terminal again after this.

## 4. Trigger a build

Push any commit to `main` (even a trivial one, like editing the README on
github.com's web editor). Expo detects the push via the GitHub App and
runs `.eas/workflows/build.yml` automatically — no dashboard click needed.

Watch progress on expo.dev under your project's **Workflows** or **Builds**
page.

## 5. Install the app on your phone

When the build finishes, open its page on expo.dev from your phone browser
and tap **Install**. This downloads and installs the DevClip APK directly —
you do **not** need the Expo Go app; this is a standalone custom build,
not something that runs inside Expo Go.

You may need to allow "install unknown apps" for your browser once,
the first time Android asks.

## 6. Making changes later

Edit files directly on github.com (or re-upload updated files) and push to
`main`. The workflow file re-triggers a build automatically every time.

## 7. Turning on the real features

Open the installed DevClip app → **Full App** tab → **Settings**:
1. **Enable** next to "Background capture" → turn on DevClip in the
   Accessibility settings screen that opens → come back to the app.
2. **Start** next to "Floating bubble" → grant "display over other apps"
   when asked → tap **Start** again.

---

## Troubleshooting

- **Build fails on credentials again** — the one-time step in section 3
  wasn't completed, or was done for the wrong profile. Re-run
  `eas credentials --platform android` and pick **development** again.
- **Any other build error** — paste the log from the build's page on
  expo.dev into chat with me. Native Android build errors are normal on
  a first pass and usually take one or two small fixes.
- **OEM battery optimization kills the accessibility service** — exempt
  DevClip from battery optimization in your phone's app settings.
