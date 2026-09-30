# TouchGrass

Per-app daily screen-time limits for Android, built as a Digital Wellbeing replacement for GrapheneOS.

> I switched to [GrapheneOS](https://grapheneos.org/), which has no Digital Wellbeing feature. Since I tend to get stuck on Instagram or TikTok, I needed a replacement. However, every option I found online is either outdated, looks awful, or needs an unreasonable amount of permissions on your phone. Hence I built TouchGrass. Btw, this is almost entirely AI work.
> 
- No accessibility service. It uses Usage Access and "Display over other apps" only.
- Material 3 with dynamic color.
- Installable and updatable through [Obtainium](https://github.com/ImranR98/Obtainium) from this repo's GitHub Releases.

## Build

Requires JDK 21 and the Android SDK (platform 37.2).

```bash
./gradlew installDebug      # build and install a debug build over adb
./gradlew assembleRelease   # build a release APK
```

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
