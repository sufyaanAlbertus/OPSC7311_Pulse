# Pulse — Personal Budget Tracker (Android / Kotlin / Jetpack Compose)

## Opening the project
1. Open Android Studio (Koala or newer).
2. File > Open > select this `Pulse` folder.
3. Let Gradle sync. Targets compileSdk 36 / minSdk 34, AGP 8.7.3, Gradle 8.9, Kotlin 1.9.24, Compose BOM 2024.12.01. minSdk is intentionally set to 34 to match the SDK platforms actually installed on the dev machine this was set up on (API 34 and API 36 only) — if you're on a different machine with older platforms installed, you can safely lower it back toward 26 without any code changes, since nothing in this codebase uses an API newer than what API 26 already provides.
4. A real Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) is included, so `./gradlew assembleDebug` and the GitHub Actions workflow both work without any extra setup.

## Before it will build
- **Fonts**: download Space Grotesk (Regular/Medium/Bold) and IBM Plex Mono (Regular/Medium) from
  Google Fonts and drop the `.ttf` files into `app/src/main/res/font/` using these exact filenames,
  matching `ui/theme/Type.kt`:
  `space_grotesk_regular.ttf`, `space_grotesk_medium.ttf`, `space_grotesk_bold.ttf`,
  `ibm_plex_mono_regular.ttf`, `ibm_plex_mono_medium.ttf`.
- **App icon**: `res/drawable/ic_pulse_logo.xml` is a placeholder hexagon-and-pulse-line vector.
  Swap it for a proper adaptive icon via Android Studio's Image Asset tool when ready.

## What's fully working end to end

- **Login/Register use a username and password** (Part 2 brief wording), stored hashed (SHA-256,
  not plaintext) in Room, with a DataStore session that persists across app restarts and a
  working Log Out button on the Badges screen.
- **Expense entries capture date, start time, end time, description, and category** — all four
  fields the brief asks for, not just a date.
- **Categories are user-creatable.** A built-in set of twelve plus a "+ Add new category" dialog
  in Add Expense that writes to Room and is immediately selectable.
- **Optional receipt photos** via the system photo picker, with a live thumbnail before saving
  and tap-to-view (`ACTION_VIEW` intent) from the Expense List.
- **Both a minimum and a maximum monthly goal**, set together with a `RangeSlider` on the Goals
  screen (Compose's equivalent of a `SeekBar`) plus a precise text field for the max value.
- **User-selectable period** ("This Week" / "This Month") genuinely drives the Expense List and
  Category Breakdown screens — not hardcoded to the current month.
- **Category limits are editable** — tap any category on Goals & Limits to set its limit.
- **Currency is formatted with `java.text.NumberFormat`** (see `util/CurrencyFormat.kt`), not
  hand-rolled string formatting, across every screen that shows money.
- **Logging** (`android.util.Log`) is present through the repository and ViewModel layers at
  every meaningful operation — login/register outcomes, expense saves, budget and limit changes,
  badge unlocks, period changes.
- **The 7-Day Signal chart uses real data**, and the over-threshold check
  (`util/BudgetMath.kt::isDayOverThreshold`) is a small, deliberately pure function pulled out of
  the ViewModel so it's actually unit-testable.
- **Gamification (streak, XP, level, 8 badges)** is evaluated against real logged activity.
- **Theme toggle persists** via DataStore, applied instantly app-wide.
- **Automated testing + CI**: `app/src/test/.../CurrencyFormatTest.kt` is a real JUnit test suite
  (pure JVM, no emulator needed) and `.github/workflows/build.yml` runs `./gradlew test` then
  `./gradlew assembleDebug` on every push, uploading the built APK as a workflow artifact.

## What's still missing — read before you submit

**Do not submit this as a zip.** The brief is explicit: *"No zip files are allowed"* for the
actual submission. This zip is only so you can open the project locally — the real deliverable
is a GitHub repository with your code pushed to it, a README with a demo video link, and a
built APK. None of that GitHub setup, the demo video, or the actual push has been done from this
end — that part is on you:

1. Create the GitHub repo, `git init`, commit, and push this project.
2. Record the demo video (voiceover required per the brief) showing every feature working,
   upload it (YouTube unlisted is fine), and link it in the README.
3. Confirm the GitHub Actions workflow actually goes green on your repo — it should, since the
   wrapper files are real, but CI environments occasionally surface issues a local build doesn't.
4. Build a release/debug APK (`./gradlew assembleDebug`, or let the Actions artifact serve as
   your APK) and include it in the submission.

**Functional gaps still open:**
- Recurring expenses save the weekly/monthly flag but nothing auto-generates future occurrences
  yet — a WorkManager job would close this.
- Receipt photo permissions may not survive a device reboot (the picker's read grant isn't
  explicitly persisted with `takePersistableUriPermission`).
- No forgot-password backend — the link is present but inert, since there's nothing to reset
  a password against in a local-only prototype.
- Local database only, per this Part 2 scope — an online database is a later-stage requirement
  in your module, and `PulseRepository` is written so Room calls can be swapped for remote calls
  without touching ViewModels or UI.
- `AppDatabase` uses `fallbackToDestructiveMigration()` since no real user data exists yet —
  replace with a proper Room `Migration` before this ever holds data you care about.
- Only one test file exists. The brief says "conduct automated testing on the main functionality"
  (plural, broadly) — worth adding tests for the badge-unlock thresholds and the auth validation
  rules too, both of which are currently only exercised manually.

## Design reference
Dark `#0A0D10`/`#10151A`/`#232B32`, light `#F4F6F8`/`#FFFFFF`/`#D2D8DE`, accent blue `#3D8FE0`,
gamification violet `#9B6FE8`, error red `#E1524C`, Space Grotesk for labels, IBM Plex Mono for
all numeric readouts, 0dp corners everywhere, 1dp borders instead of shadows.
