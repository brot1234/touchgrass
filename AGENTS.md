# AGENTS.md

Instructions for AI coding agents working in this repository.

## Project

TouchGrass is a personal Android app that replaces Digital Wellbeing on GrapheneOS, which ships without it.
It runs on one device: a Pixel 10a on the current GrapheneOS release.

- **Main feature:** per-app daily time limits (e.g. Instagram, TikTok). When a limit is reached, the app is blocked for the rest of the day and there is no "5 more minutes" bypass.
- **Later feature:** screen-time overview per app, per day and per week.
- The block does not need to be tamper-proof. Changing or removing a limit in TouchGrass's own settings is acceptable, just like in Digital Wellbeing.

## Hard constraints

- **No accessibility service.** Never add `BIND_ACCESSIBILITY_SERVICE` or suggest it.
- **Use as few permissions as possible.** The intended set is:
  - Usage Access (`PACKAGE_USAGE_STATS`, granted in Settings): reads usage events.
  - Display over other apps (`SYSTEM_ALERT_WINDOW`): shows the block screen and allows starting an activity from the background.
  - Foreground service, notifications and boot-completed permissions, as needed.
- **No `QUERY_ALL_PACKAGES`.** Use a `<queries>` element with a launcher-intent filter to list apps.
- **No network access, analytics or Google Play Services.** Nothing leaves the device.
- Add any other permission only after explaining why it's needed and getting approval.

## Planned architecture for limits

- A foreground service runs while the screen is on and polls `UsageStatsManager.queryEvents` about once a second. It works out the foreground app from `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` events.
- Today's usage per app is computed from the raw events. Don't use `queryUsageStats` aggregates; they are unreliable.
- When the foreground app reaches its limit, the service adds a full-screen `TYPE_APPLICATION_OVERLAY` window ("Time's up for <app>"). After about 2 seconds it starts the home intent while the overlay is still visible, then removes the overlay. The visible overlay is what allows the background activity start on Android 15+. There is no block activity and no bypass; reopening the app triggers the same thing again.
- Limits are stored in SharedPreferences (package → minutes). History for the overview goes in Room and is snapshotted periodically, because the system keeps usage events only for a short time.

## Stack and versions

- Kotlin, Jetpack Compose, Material 3 with dynamic color (no static color scheme, since minSdk guarantees dynamic color).
- AGP 9.4.1 with built-in Kotlin: do **not** apply `org.jetbrains.kotlin.android` in modules. It is declared `apply false` in the root build only, to pin Kotlin 2.4.20.
- Gradle 9.8.0 (wrapper), JDK 21.
- `minSdk = 36`, `targetSdk = 36`, `compileSdk = 37.2`. Built for one device, so don't add compatibility code for older API levels.
- Dependency versions live in `gradle/libs.versions.toml`.
- Package / applicationId: `dev.touchgrass.app`. Debug builds add the `.debug` suffix.
- Before adding a library, check it's compatible with the current compileSdk and AGP. Compose 1.12, for example, requires compileSdk 37.

## Commands

```bash
./gradlew installDebug       # build and install on the connected phone via adb
./gradlew assembleRelease    # release APK
./gradlew testDebugUnitTest  # unit tests (domain logic only)
```

## Release and distribution

- The app is installed and updated with Obtainium from this repo's GitHub Releases.
- Releases are built by GitHub Actions (`.github/workflows/release.yml`) when a `vX.Y.Z` tag is pushed. `versionName` and `versionCode` come from the tag (`vX.Y.Z` → `X*10000 + Y*100 + Z`, so Y and Z stay below 100), and `versionCode` must always increase.
- `app/build.gradle.kts` signs release builds only when `KEYSTORE_PATH` is set, so local release builds are unsigned.
- Signing uses the GitHub secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` and `KEY_ALIAS`. The keystore is never committed; `.gitignore` excludes `*.jks`, `*.keystore` and `keystore.properties`.

License: GPL-3.0-or-later.
