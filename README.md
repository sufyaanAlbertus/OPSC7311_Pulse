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

2. **Open the project correctly.** Clone the repo, then in Android Studio go **File > Open** and
   navigate to: `<repo-name>/Pulse` — the folder that directly contains `app/`, `gradle/`,
   `gradlew`, `settings.gradle.kts`, and this `README.md` as siblings. Select that folder and
   click Open — not its parent, and not the `app` folder itself. Opening the wrong level is the
   single most common cause of "Task not found in project" sync errors.

3. **Let Gradle sync.** The project ships with a real Gradle wrapper (`gradlew`, `gradlew.bat`,
   `gradle/wrapper/gradle-wrapper.jar`), so no separate Gradle install is needed. First sync
   downloads Gradle 8.9 and all dependencies — this can take a few minutes.

---

## Opening the project — quick reference
Targets compileSdk 36 / minSdk 34, AGP 8.7.3, Gradle 8.9, Kotlin 1.9.24, Compose BOM 2024.12.01.
minSdk is set to 34 to match the SDK platforms installed on the dev machine this was built on
(API 34 and API 36 only) 

## Design reference
Dark `#0A0D10`/`#10151A`/`#232B32`, light `#F4F6F8`/`#FFFFFF`/`#D2D8DE`, accent blue `#3D8FE0`,
gamification violet `#9B6FE8`, error red `#E1524C`, Space Grotesk for labels, IBM Plex Mono for
all numeric readouts, 0dp corners everywhere, 1dp borders instead of shadows.
