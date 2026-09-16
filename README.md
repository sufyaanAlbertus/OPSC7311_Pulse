# OPSC7311 — Pulse: Personal Budget Tracker App

Android app (Kotlin / Jetpack Compose) submitted for OPSC7311.

## Group members

| Name              | Student Number |
|-------------------|-----------------|
| Sechaba Mokoena   | ST10453270      |
| Sufyaan Albertus  | ST10436103      |
| Latita Mvunelo    | ST10093388      |
| Sandile Duba      | ST10262263      |
| Rea Moloi         | ST10443585      |


## Demo video

`[VIDEO LINK GOES HERE]`

---

## Startup guide

Follow these steps in order on a fresh clone/download of this repo.

1. **Install prerequisites** (skip any you already have):
   - Android Studio (Koala or newer)
   - Android SDK Platform **34** and **36** via Tools > SDK Manager > SDK Platforms
   - Android SDK Command-line Tools (latest) via Tools > SDK Manager > SDK Tools

2. **Open the project correctly.** Clone/download the repo, then in Android Studio go
   **File > Open** and navigate into: `Pulse_Project_Android_Studio_Files/Pulse` — the folder
   that directly contains `app/`, `gradle/`, `gradlew`, `settings.gradle.kts`, and this
   `README.md` as siblings. Select that `Pulse` folder itself and click Open — not
   `Pulse_Project_Android_Studio_Files`, and not the `app` folder inside `Pulse`. Opening the
   wrong level is the single most common cause of "Task not found in project" sync errors.

3. **Let Gradle sync.** The project ships with a real Gradle wrapper (`gradlew`, `gradlew.bat`,
   `gradle/wrapper/gradle-wrapper.jar`), so no separate Gradle install is needed. First sync
   downloads Gradle 8.9 and all dependencies — this can take a few minutes.

4. **Run the app.** Once sync finishes, either:
   - Press the green **Run** button in Android Studio with an emulator or device selected, **or**
   - Build the APK manually and install it by hand:
     1. **Build > Build App Bundle(s) / APK(s) > Build APK(s)** and wait for the build to finish.
     2. The output APK lands at `Pulse/app/build/outputs/apk/debug/app-debug.apk`.
     3. Start an emulator from **Device Manager** and wait until it's fully booted to the home screen.
     4. Drag `app-debug.apk` from File Explorer straight onto the running emulator window — the
        emulator installs it automatically and it'll appear in the app drawer as **Pulse**.

---

## Opening the project — quick reference
Targets compileSdk 36 / minSdk 34, AGP 8.7.3, Gradle 8.9, Kotlin 1.9.24, Compose BOM 2024.12.01.
minSdk is set to 34 to match the SDK platforms installed on the dev machine this was built on
(API 34 and API 36 only) 

## Design reference
Dark `#0A0D10`/`#10151A`/`#232B32`, light `#F4F6F8`/`#FFFFFF`/`#D2D8DE`, accent blue `#3D8FE0`,
gamification violet `#9B6FE8`, error red `#E1524C`, Space Grotesk for labels, IBM Plex Mono for
all numeric readouts, 0dp corners everywhere, 1dp borders instead of shadows.
