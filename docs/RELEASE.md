# Releasing Vocablo

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
./gradlew :app:bundleRelease
```

The bundle is `app/build/outputs/bundle/release/app-release.aab`. Without those variables the build still works but is unsigned (that is what CI does to check the release build).

Before each release, raise `versionCode` (and usually `versionName`) in `app/build.gradle.kts`.

## 3. Play Console checklist

- **App content → Privacy policy:** link to `docs/PRIVACY.md` (for example its GitHub page).
- **Data safety:** no data collected or shared by the developer. Mention that, when the user turns on the AI tutor with their own key, messages are sent to the AI service they chose, and that dictionary lookups send the looked-up word to Wiktionary. Data is encrypted in transit (HTTPS).
- **Ads:** none. **Target audience:** 13 and over is simplest because of the optional AI tutor.
- **Permissions to explain:** notifications (reminders), run at startup (re-book reminders after a restart), internet (dictionary and tutor).
- **Store listing:** screenshots of Home, swipe study, the drill, a game and the tutor; the Tuzo icon is already in the app's adaptive icon.
- Start with an **internal testing** track, install from the Play link, and check reminders, the widget and the tutor on a real phone before moving to production.
