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
