# Pulse — Developer setup

> **Only want to try the app?** You don't need any of this. Install the APK from the
> [main README](../README.md#-install-and-run-pulse-about-2-minutes-no-android-studio-needed).

## Run from source (standard Android Studio)

1. **Clone** the repository:
   ```
   git clone https://github.com/<your-account>/<this-repo>.git
   ```
2. In Android Studio choose **File → Open** and select the **cloned folder itself**
   (the folder that contains `settings.gradle.kts`, `gradlew` and the `app` folder).
3. Wait for **Gradle sync** to finish (the first time it downloads dependencies, which takes a few minutes).
4. Pick a phone or emulator in the device dropdown and press **Run ▶**.

That's all. The repository already contains everything the build needs:

| Included | Why |
|---|---|
| Gradle wrapper (`gradlew`, `gradle/wrapper/*`) | Android Studio uses the exact Gradle version (8.9) automatically |
| `app/google-services.json` | Connects the app to the Pulse Firebase project, so there's no Firebase setup |
| Fonts in `app/src/main/res/font` | No downloads needed |

`local.properties` is **not** committed on purpose. Android Studio creates it automatically with your own SDK path.

## Environment

| Tool | Version |
|---|---|
| Android Studio | Koala (2024.1) or newer |
| JDK | 17 (bundled with Android Studio) |
| Android SDK | Platform 36 (Android Studio offers to install it on first sync if missing) |
| Kotlin / AGP / Gradle | 1.9.24 / 8.7.3 / 8.9 |
| Compose BOM / Firebase BoM | 2024.12.01 / 33.7.0 |
| Device | Android 8.0 (API 26) or newer |

Keep the Firebase BoM at **33.7.0**. Version 34 and above needs Kotlin 2.x.

## Tests

```
./gradlew testDebugUnitTest        (Windows: gradlew testDebugUnitTest)
```
35 JUnit tests in `app/src/test/java/com/pulse/budget/util/`. They also run automatically in GitHub Actions on every push.

## Build an APK yourself

**Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The file is created at
`app/build/outputs/apk/debug/app-debug.apk`. (You normally don't need to: GitHub Actions publishes
`Pulse.apk` to the Releases page on every push to `main`.)

## Using your own Firebase project (optional)

1. In the [Firebase console](https://console.firebase.google.com), create a project and add an
   Android app with the package name `com.pulse.budget`.
2. Download `google-services.json` and replace `app/google-services.json`.
3. **Authentication → Sign-in method → Email/Password → Enable**. Optionally, under
   **Authentication → Settings → Password policy**, require upper/lower case, numbers and symbols
   with a minimum length of 8, to match the app's own policy.
4. **Firestore Database → Create database** (production mode), then paste the contents of
   [`firestore.rules`](../firestore.rules) into the **Rules** tab and click **Publish**.

## Project structure

```
app/src/main/java/com/pulse/budget/
├── data/model/          Models.kt — data classes + Firestore layout
├── data/repository/     AuthRepository (Firebase Auth), PulseRepository (Firestore reads/writes)
├── viewmodel/           AuthViewModel, ExpenseViewModel, ThemeViewModel
├── ui/screens/          one file per screen
├── ui/components/       charts, period selector, password strength, shared widgets
├── ui/theme/            colours, typography, shapes
└── util/                pure, unit-tested logic (goals, recurrence, gamification, validation)
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `File google-services.json is missing` | Make sure `app/google-services.json` exists (it's committed in this repo) |
| `SDK location not found` | Open the project in Android Studio once; it creates `local.properties` |
| `Module was compiled with an incompatible version of Kotlin` | Keep `firebase-bom:33.7.0` |
| Strange Gradle errors | **File → Invalidate Caches → Invalidate and Restart** |
