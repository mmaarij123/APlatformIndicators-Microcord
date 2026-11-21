# APlatformIndicators for Microcord

See which Discord clients someone is using while reading chat or viewing their profile. This is a Microcord port of [Strencher’s APlatformIndicators for BetterDiscord](https://github.com/Strencher/BetterDiscordStuff/blob/master/PlatformIndicators/APlatformIndicators.plugin.js).

## What it shows

- **Chat headers** show platform icons on the message author’s line.
- **User profiles** show the icons inside Microcord’s badge pill alongside Discord badges. The pill also appears when platform icons are the only badges.
- **Platforms** include desktop, mobile, and web when Microcord has presence for the user. Your own Discord sessions can also show console and VR when reported.
- **Status colors** show online in green, idle in amber, and Do Not Disturb in red. Your own streaming activity uses purple. Offline and invisible sessions are hidden.

Both placements are enabled by default. Plugin settings let you choose which platforms appear in each placement. Bots are hidden by default.

## Install

1. Download the file ending in `-debug.apk` from the [latest GitHub release](https://github.com/mmaarij123/APlatformIndicators-Microcord/releases/latest).
2. Open the APK on your Android device and install it.
3. Open **Microcord Settings → Plugins**, turn on **Load Plugins** if needed, and enable **APlatformIndicators**. Approve the signing certificate if Microcord prompts.

If you installed v1.1.0 or earlier, uninstall the old plugin before installing the latest APK. The Android package name changed in v1.1.1.

## Build from source

The plugin is written in Kotlin and follows the [Microcord plugin template](https://github.com/microcord/plugin-template). It compiles against Microcord Plugin SDK 1.0.6 and uses the host’s presence and session data. The profile badges use Microcord’s native profile header layout.

Use JDK 21, Android SDK Platform 36.1, and Build Tools 36.0.0. Set `ANDROID_HOME` or create a `local.properties` file with `sdk.dir` pointing to your Android SDK.

On Windows, run this from the project directory.

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux, run `./gradlew assembleDebug`. Gradle writes the APK to `build/outputs/apk/debug/`.

To build a signed release, copy [keystore.properties.example](keystore.properties.example) to `keystore.properties`, fill in your signing details, and run the `assembleRelease` Gradle task. Use the same signing key for updates you distribute.

## License

[MIT License](LICENSE).
