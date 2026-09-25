# DevClip — Setup Guide (phone only)

No terminal, no computer. Just github.com, with one step near
the end that is worth a computer if you can reach one — explained honestly
where it comes up.

---

## 1. How builds happen

Every push to `main` builds an installable APK on GitHub Actions and
attaches it to a release tagged `latest`. Nothing to click and nothing to
trigger.

## 2. Getting the app on your phone

Open the repository's **Releases** page in your phone browser and download
the APK from the release tagged `latest`. It is always the most recent
build of `main`.

The direct link:

```
https://github.com/fubulatu-lang/DevClip/releases/latest
```

Android will ask you to allow "install unknown apps" for your browser the
first time. That is expected for an app installed outside the Play Store.

## 3. Signing — the step that decides whether updates hurt

Android refuses to install an update whose signing key differs from the
installed app's. It does not offer to merge them; it makes you uninstall
first, **and uninstalling DevClip deletes your clip history.**

So the key matters more than it sounds.

**Without any setup**, the workflow builds with a throwaway key that Gradle
generates fresh each run. Every build therefore disagrees with the last,
and every install means: export a backup, uninstall, install, import the
backup. It works, but it is tedious and one forgotten export loses
everything.

**With the project keystore stored as a secret**, every build agrees with
every other build, and installing is just installing.

**One catch, once.** The copy on your phone right now was built before the
secrets existed, so it carries one of those throwaway keys. The first
build after the secrets are added will therefore still disagree with it,
and still need an uninstall — export a backup first. Every build after
that one installs straight over the top. It is one more uninstall, not
none, and then never again.

### Setting it up

The keystore you want already exists — EAS signed the original app with
it. On expo.dev, open the project, go to **Credentials → Android**, and
download the keystore file. That page also shows the keystore password,
key alias and key password.

Then add four repository secrets on github.com, under
**Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | the keystore file, base64-encoded |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password from expo.dev |
| `ANDROID_KEY_ALIAS` | key alias from expo.dev |
| `ANDROID_KEY_PASSWORD` | key password from expo.dev |

The awkward one is the first. GitHub secrets hold text, not files, so the
keystore has to be base64-encoded — and that needs something that can run
`base64 devclip.jks`. A computer does it in one command; on the phone,
Termux does the same.

**Do not paste a signing keystore into an online base64 converter.**
Anyone holding that file and its passwords can publish an app that Android
will accept as an update to yours. It is worth waiting until you are near
a computer.

Until the secrets exist, builds still happen and are still installable —
they just carry the warning above, and the release notes say so.

## 4. Making changes

Edit files on github.com, or push from anywhere, and a new APK appears on
the `latest` release a few minutes later. The release notes carry the
commit it was built from, its size and its SHA-256, so you can always tell
which build is on your phone.

## 5. What runs before a build

Two checks run on every pull request, and the first one gates merges:

- **Android compile** — the Kotlin compiles, and every string it
  references exists.
- **One UI conformance** — a scan for hard-coded colours and off-scale
  spacing. Advisory; it reports rather than blocks.

## 6. Turning on the real features

The app walks you through this on first launch, on a setup screen you can
also skip past and come back to. If you skipped it, everything below is in
**Settings**.

1. **Text capture** → turn DevClip on in the Accessibility settings screen
   that opens → come back to the app. Android shows a broad warning screen
   for this; that is expected, and it is what lets DevClip read the text you
   have highlighted.
2. **Floating bubble** → grant "display over other apps" when asked → tap
   **Start**.
3. **Notifications** → say yes. Android only offers this once, and the
   notification is one of the ways to bring the bubble back after you hide it.

## 7. Using it

- **Highlight text anywhere, then tap the bubble.** It is saved, and it goes
  on your clipboard too, so the bubble replaces the Copy button rather than
  sitting beside it. The bubble grows a ring when it can see a selection.
- **Tap the bubble with nothing highlighted** to open your clips.
- **Long press the bubble** to open your clips even when text is highlighted.
- **Drag the bubble** up and down either edge. Drop it in the middle and it
  snaps back to the nearer edge.
- **Drag it into the circle at the bottom centre** to hide it. DevClip keeps
  running — bring it back from the notification, or from Settings.
- **Tap a clip once to arm it, again to paste it** into whatever field you
  were last using. Turn "Confirm before paste" off to paste on one tap.

---

## Troubleshooting

- **Text capture stops working, but Settings says it is granted.** This is
  the most likely thing to go wrong and the hardest to spot, because
  nothing announces it. Samsung unbinds accessibility services from apps
  it decides are idle, and the permission switch keeps reading as on while
  nothing is listening.

  Tell them apart by highlighting text and watching the bubble: a ring
  around it means the service is alive. No ring means it is not, whatever
  Settings claims.

  Fix it by turning the permission off and on again in Android's
  Accessibility settings, then check the ring again. To stop it recurring,
  set DevClip to **Unrestricted** under Settings → Apps → DevClip →
  Battery, and make sure it is not in Sleeping or Deep sleeping apps under
  Settings → Battery → Background usage limits. Android gives apps no way
  to read or change that list, so it has to be done by hand.

- **It also happens after installing a new build.** Reinstalling is when
  Android most often leaves the switch on while the service stops binding.
  Worth checking the ring after every install.

- **A new APK will not install.** Its signing key differs from the
  installed app's — see section 3. Export a backup from Settings first,
  then uninstall and install.

- **A build failed.** Open the run under the repository's **Actions** tab;
  the failing step names the file and line. Kotlin compile errors are
  caught earlier now, on the pull request, by the Android compile check.

- **The bubble is gone and the notification is too.** It may have tucked
  itself into the screen edge — look for a slim handle on the left or
  right edge and touch it. If the tuck delay is set too short for you,
  Settings turns it off.
