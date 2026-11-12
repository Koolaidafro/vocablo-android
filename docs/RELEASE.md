# Releasing Vocablo

There are two builds of the app:

- **github**: what you install yourself. Every merge to `main` publishes it as a GitHub release, and the app offers the update on its Home screen.
- **play**: for the Play Store, which does its own updates, so this build has no updater.

## Updates from GitHub (once)

1. In the repository, open **Settings → Secrets and variables → Actions → New repository secret**.
2. Name it `VOCABLO_UPDATE_KEYSTORE` and paste the signing key text (a long base64 line) as the value.
3. Run the **Publish update** workflow once from the Actions tab, or merge anything to `main`.
4. On the phone, back up from Me → Back up everything, uninstall any copy you built yourself, install `vocablo.apk` from the newest release, then restore the backup. From then on, updates install over the top and keep your data.

Keep a private copy of that key. Every update must be signed with it, or Android refuses to install it over the old version.

# Releasing to the Play Store

## 1. Make an upload key (once)

```bash
keytool -genkeypair -v -keystore vocablo-upload.jks -keyalg RSA -keysize 4096 -validity 10000 -alias vocablo
```

Keep the `.jks` file and its passwords somewhere safe and **never commit them**. With Play App Signing, Google holds the real signing key and this is only the upload key.

## 2. Build a signed bundle

```bash
export VOCABLO_KEYSTORE=/path/to/vocablo-upload.jks
export VOCABLO_KEYSTORE_PASSWORD=...
export VOCABLO_KEY_ALIAS=vocablo
export VOCABLO_KEY_PASSWORD=...
./gradlew :app:bundlePlayRelease
```

The bundle is `app/build/outputs/bundle/playRelease/app-play-release.aab`. Without those variables the build still works but is unsigned (that is what CI does to check the release build).

Before each Play release, set a higher version code with `export VOCABLO_VERSION_CODE=...` (the version name follows it).

## 3. Play Console checklist

- **App content → Privacy policy:** link to `docs/PRIVACY.md` (for example its GitHub page).
- **Data safety:** no data collected or shared by the developer. Mention that, when the user turns on the AI tutor with their own key, messages are sent to the AI service they chose, and that dictionary lookups send the looked-up word to Wiktionary. Data is encrypted in transit (HTTPS).
- **Ads:** none. **Target audience:** 13 and over is simplest because of the optional AI tutor.
- **Permissions to explain:** notifications (reminders), run at startup (re-book reminders after a restart), internet (dictionary and tutor).
- **Store listing:** screenshots of Home, swipe study, the drill, a game and the tutor; the Tuzo icon is already in the app's adaptive icon.
- Start with an **internal testing** track, install from the Play link, and check reminders, the widget and the tutor on a real phone before moving to production.
