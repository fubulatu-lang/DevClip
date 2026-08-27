# DevClip — Setup Guide (Phone-Only, Cloud-Only)

This project is split into two phases:

- **Phase 1 — works today, no native build needed.** The clip list, search,
  sort, manual reorder, edit/delete, and a "Capture current clipboard"
  button. Runs in plain **Expo Go**.
- **Phase 2 — the floating bubble + true background clipboard capture.**
  Requires a **custom dev client**, built in the cloud by **EAS Build**
  (still no computer needed — you just install the resulting APK on your
  phone instead of using the Expo Go app).

Do Phase 1 first to make sure everything works, then move to Phase 2.

---

## 0. Accounts you'll need (all free)

1. A **GitHub** account — github.com
2. An **Expo** account — expo.dev (this is what EAS Build uses)

---

## 1. Get the code onto GitHub

1. On github.com, tap **+ → New repository**. Name it `DevClip`. Keep it
   **Private** or Public, your choice. Do **not** initialize with a README
   (we already have one).
2. On your phone, unzip the file you downloaded from this chat.
3. Easiest phone-only way to upload it: use GitHub's web uploader.
   - Open your new empty repo → **"uploading an existing file"** link.
   - Upload the files/folders from the unzipped project. If GitHub's mobile
     web uploader struggles with many nested folders at once, use the
     **GitHub mobile app** instead, or open a Codespace on the *empty* repo
     first (step 2) and drag files into it from there — Codespaces' file
     explorer accepts folder uploads reliably even on mobile.

---

## 2. Open a cloud dev environment (GitHub Codespaces)

1. In your GitHub repo, tap **Code → Codespaces → Create codespace on main**.
2. Wait for it to boot — you get a full VS Code editor + Linux terminal, in
   your browser, running in the cloud.
3. If you uploaded the zip contents into the Codespace instead of via the
   web uploader: drag-and-drop the unzipped folder onto the Explorer panel,
   then in the terminal run `git add -A && git commit -m "initial" && git push`.

From here on, every command below is typed into the Codespaces **terminal**.

---

## 3. Install dependencies

```bash
npm install
```

---

## 4. Phase 1: run it in Expo Go

1. Install the **Expo Go** app from the Play Store on your phone.
2. In the Codespace terminal:
   ```bash
   npx expo start --tunnel
   ```
   `--tunnel` is important — it lets your phone reach the Codespace over the
   internet instead of needing to be on the same Wi-Fi network.
3. Scan the QR code shown in the terminal (or open the forwarded URL) using
   the Expo Go app.
4. You should see DevClip. Try:
   - Copy some text anywhere on your phone.
   - Tap **"+ Capture current clipboard"** — it should appear in the list.
   - Search, change sort order, tap an item to copy it back, long-press to
     edit/delete.
5. The **Small / Expanded / Full App** buttons and the **Settings** section
   (visible in Full App mode) will show a note explaining that the floating
   bubble needs Phase 2 — that's expected right now.

If Phase 1 works, your code, database, and UI logic are all solid. Phase 2
only adds the native bubble/background-capture layer on top.

---

## 5. Phase 2: build the custom dev client with EAS

1. Install the EAS command line tool and log in:
   ```bash
   npm install -g eas-cli
   eas login
   ```
   (enter your expo.dev account email/password)
2. Link this project to your Expo account:
   ```bash
   eas build:configure
   ```
   Accept the defaults — this project already has an `eas.json` with a
   `development` profile set up.
3. Start the cloud build:
   ```bash
   eas build --profile development --platform android
   ```
   This uploads your code to Expo's build servers, which run
   `expo prebuild` (this is what turns `plugins/withDevClipNative.js` into
   real Android manifest entries and copies the Kotlin files in), then
   compiles it with Gradle — all in the cloud. Takes roughly 10–20 minutes.
4. When it finishes, the terminal prints a link (and QR code). Open that
   link on your phone and tap **Install** to download the APK directly —
   no computer needed. You may need to allow "install unknown apps" for
   your browser once.
5. Open the newly installed **DevClip** app icon (this is your custom dev
   client — different from Expo Go).

### Connecting Metro to your dev client

1. Back in the Codespace terminal:
   ```bash
   npx expo start --dev-client --tunnel
   ```
2. On your phone, open the DevClip dev client app — it should connect
   automatically, or show a screen to paste/scan the dev server URL.

### Turning on the real Phase 2 features

1. In the app, switch to **Full App** mode to see the Settings panel.
2. Tap **Enable** next to "Background capture". This opens
   Android's Accessibility settings — find **DevClip** in the list and turn
   it on. Android will show a permission warning dialog; this is standard
   for any accessibility-based app. Come back to DevClip afterward.
3. Tap **Start** next to "Floating bubble". The first time, this opens the
   "display over other apps" permission screen — turn it on for DevClip,
   then come back and tap **Start** again.
4. You should now see a small blue circle floating over your screen. Drag it
   anywhere; tap it (without dragging) to open the popup.
5. Copy text in any other app — it should now appear in DevClip's list
   automatically, with no need to open the app or tap Capture.

---

## 6. Iterating

Whenever you change JS/TS code: just save the file and reload the app (shake
the phone → Reload, or it auto-refreshes) — no rebuild needed.

Whenever you change anything under `plugins/android-src/*.kt`,
`plugins/withDevClipNative.js`, or `app.json`'s `android`/`plugins` section,
you need a new native build:
```bash
eas build --profile development --platform android
```

## 7. Building a real release (later)

When you're ready for a version to share beyond your own device:
```bash
eas build --profile production --platform android
```
This produces an `.aab` file suitable for uploading to the Play Store. Play
Store review for accessibility-service apps is stricter — you'll need to
justify the permission in your store listing (this is normal for clipboard
manager apps; e.g. explain it's used only to detect copy events, not read
screen content).

---

## Troubleshooting

- **Build fails referencing `ReactRootView` or Fabric** — make sure
  `"newArchEnabled": false` is still present in `app.json`. The overlay
  popup code uses the classic (non-Fabric) React Native view-hosting APIs,
  which is by far the simplest way to render RN content inside a raw
  Android overlay window today.
- **Accessibility toggle doesn't "stick" / turns itself off** — some
  Android OEM skins (Xiaomi/MIUI, some Samsung versions) aggressively kill
  accessibility services to save battery. Look for a battery-optimization or
  "autostart" exemption setting for DevClip.
- **Bubble doesn't appear after tapping Start** — double check the overlay
  permission was actually granted (Settings → Apps → DevClip → "Display over
  other apps").
- Paste any EAS build error into chat with me and I'll help you fix it —
  native Android build errors are normal on the first pass of any project
  like this and usually take one or two small corrections.
