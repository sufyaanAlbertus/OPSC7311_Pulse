# Pulse — Developer notes

**Just want to try the app?** You don't need anything on this page. Install the APK using the steps in the [main README](../README.md#how-to-install-the-app).

---

## Run the project in Android Studio

1. Download the project:
   - on GitHub, click **Code → Download ZIP** and unzip it, **or**
   - clone it with GitHub Desktop or `git clone`.
2. Open Android Studio and go to **File → Open**.
3. Select the **`Pulse_Project_Android_Studio_Files/Pulse`** folder.
   (This is the folder with `app`, `gradle` and `settings.gradle.kts` inside it. Don't open the top folder or the `app` folder.)
4. Wait for Gradle to finish syncing. The first time can take a few minutes.
5. Choose a phone or emulator at the top and press **Run**.

That's it. You don't need to set up Firebase or download anything else, because the repo already includes:

- the Gradle wrapper, so the right Gradle version is used automatically
- `app/google-services.json`, which connects the app to our Firebase project
- the fonts the app uses

Android Studio creates a `local.properties` file by itself the first time you open the project. We don't upload this file because it's different on every computer.

---

## Make an APK

1. In Android Studio go to **Build → Generate App Bundles or APKs → Generate APKs**.
   (On older versions it's called **Build → Build APK(s)**.)
2. When it's done, click **locate** in the pop-up.
3. The file is `app/build/outputs/apk/debug/app-debug.apk`.

You usually don't need to do this, because GitHub builds the APK for us every time we push.

---

## Run the tests

In Android Studio, right-click the `app/src/test` folder and choose **Run Tests**.

Or from a terminal:

```
cd Pulse_Project_Android_Studio_Files/Pulse
gradlew testDebugUnitTest
```

There are 35 tests. GitHub runs them automatically on every push.

---

## What we used

| Tool | Version |
|---|---|
| Android Studio | Koala or newer |
| Java (JDK) | 17, comes with Android Studio |
| Android SDK | 36 (Android Studio offers to install it if it's missing) |
| Kotlin | 1.9.24 |
| Android Gradle Plugin / Gradle | 8.7.3 / 8.9 |
| Firebase BoM | 33.7.0 |
| Phone | Android 8.0 or newer |

Don't update the Firebase BoM past 33.7.0. Newer versions need a newer version of Kotlin.

---

## How the code is organised

```
app/src/main/java/com/pulse/budget/
├── data/model/        the data classes (Expense, Badge, goals ...)
├── data/repository/   AuthRepository (login) and PulseRepository (reads and writes to Firestore)
├── viewmodel/         the ViewModels that the screens use
├── ui/screens/        one file for each screen
├── ui/components/     charts, the period picker and other shared pieces
├── ui/theme/          colours, fonts and shapes
└── util/              calculations for goals, recurring expenses, badges and input checks
```

---

## Using your own Firebase project (optional)

Only do this if you want the app to save to a different Firebase project.

1. In the [Firebase console](https://console.firebase.google.com), make a new project and add an Android app with the package name `com.pulse.budget`.
2. Download its `google-services.json` and put it in the `app` folder, replacing ours.
3. Go to **Authentication → Sign-in method** and turn on **Email/Password**.
4. Go to **Firestore Database**, create a database in production mode, then copy everything from [`firestore.rules`](../Pulse_Project_Android_Studio_Files/Pulse/firestore.rules) into the **Rules** tab and click **Publish**.

---

## If something goes wrong

| Problem | What to do |
|---|---|
| "google-services.json is missing" | Check that `app/google-services.json` is there |
| "SDK location not found" | Open the project in Android Studio once and it will fix this itself |
| "Task not found" or the project won't sync | You opened the wrong folder. Open `Pulse_Project_Android_Studio_Files/Pulse` |
| "Incompatible version of Kotlin" | Make sure the Firebase BoM is still 33.7.0 |
| Any other strange Gradle error | **File → Invalidate Caches → Invalidate and Restart** |
