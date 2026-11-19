# APlatformIndicators for Microcord

A native Android port of Strencher’s [APlatformIndicators for BetterDiscord](https://github.com/Strencher/BetterDiscordStuff/blob/master/PlatformIndicators/APlatformIndicators.plugin.js). The Android project follows the [Microcord plugin template](https://github.com/microcord/plugin-template).

## Install

1. Open the plugin APK on the Android device and install it.
2. Open **Microcord Settings > Plugins**, enable **Load Plugins** if needed, and enable **APlatformIndicators**. Approve the signing certificate if Microcord prompts.
3. Open the plugin settings to control the chat header and profile badge placements, select platforms, or show indicators for bots. Both placements are on by default. Bots are hidden by default.

## What it displays

- Status-colored icons on the author line in chat and inside Microcord's native profile badge pill. On profiles without Discord badges, platform icons can fill the pill on their own.
- Desktop, mobile, and web when Microcord has presence data for the user. For your own account, indicators come from the Discord sessions delivered to Microcord, including console and VR when reported.
- Online, idle, and Do Not Disturb colors, plus a streaming color when Microcord detects your own streaming activity. Offline and invisible clients are hidden.

Version 1.1.0 moves the old **Active platforms** section into the badge pill. It uses the profile header layout and IDs present in the Microcord SDK used by the template. If a Microcord build changes that layout, its chat placement remains available. The avatar, friends, server member, DM, and favorites placements require additional host hooks. Microcord’s presence model for other users exposes desktop, mobile, and web.

## Build

Use JDK 21 and Android SDK Platform 36.1 with Build Tools 36.0.0. Set `ANDROID_HOME` or create a `local.properties` file with `sdk.dir` pointing to your SDK. From the project directory, run:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux, run `./gradlew assembleDebug`. The APK is written to `build/outputs/apk/debug/`. For a release build, fill in a local `keystore.properties` from the example and run `assembleRelease`. Reuse the same signing key when distributing updates.
