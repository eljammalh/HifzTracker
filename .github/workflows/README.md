# Hifz Tracker

An offline Android app for organizing Quran memorization (hifz) and review (muraja'ah).

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · WorkManager · min Android 8.0 (API 26)

## Build the APK

1. Install **Android Studio** (free): https://developer.android.com/studio
2. Unzip this folder, then in Android Studio choose **File → Open** and select the `HifzTracker` folder.
3. Wait for the Gradle sync to finish (the first time downloads a few hundred MB). If Studio offers to upgrade the Android Gradle Plugin, you can accept or skip. Both work.
4. **Quick test APK:** Build → Build App Bundle(s) / APK(s) → **Build APK(s)**.
   The file appears at `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your phone and open it to install (allow "install unknown apps" when asked).
5. **Release APK (for keeping or sharing):** Build → **Generate Signed App Bundle / APK** → choose **APK** → **Create new…** key store. Save the `.jks` file and its passwords somewhere safe: you need the same key to install future updates over the existing app. Pick the **release** build. The APK appears in `app/release/`.

You can also run it directly on a phone connected by USB (with USB debugging on) using the green ▶ Run button.

## Project layout

```
app/src/main/java/com/hifz/tracker/
  data/QuranData.kt          114 surahs: names, ayahs, start pages (604-page Madani Mushaf)
  data/Database.kt           Room: surah progress, review chunks, activity log
  data/SettingsRepository.kt DataStore: daily amount, review goal, order, language, reminders
  logic/Planner.kt           Segmentation, progress, completion estimate, review suggestions
  HifzViewModel.kt           App state and actions
  MainActivity.kt            Navigation (Today, Memorize, Memorized, Review, Settings)
  ui/                        Screens, theme, formatting, English/Arabic text
  notify/Reminders.kt        Daily reminder notifications
```

## How the main rules work

- **Segments:** a surah's length in pages is divided by the daily amount. A 2-page surah at ½ page/day gives 4 segments of 25% each. The last segment may be shorter.
- **Changing the daily amount** keeps the segments you've already completed and re-divides only what's left.
- **Progress** is shown both as surahs (out of 114) and pages (out of 604).
- **Completion estimate:** remaining pages ÷ daily amount, plus a second estimate from your real pace over the last 14 days.
- **Review:** draws only from fully memorized surahs. Each day the app suggests the portions that have gone longest without review until the goal is met. You can override it with "Choose manually". Surahs not reviewed for 14+ days are highlighted.
- **"I already know this surah"** marks a surah as memorized without counting it toward your pace.

## Data note

Page start numbers follow the standard 604-page Madani Mushaf (Hafs). When several short surahs start on the same page, that page is split evenly between them, so lengths of short surahs are approximate to within a fraction of a page.
