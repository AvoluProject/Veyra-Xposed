# Veyra Xposed 1.0.1

Veyra is an LSPosed module with a native settings dashboard. It applies a lightweight iOS-inspired visual treatment to the real Samsung Settings view tree while leaving native Settings controllers and preference actions in place.

## Important scope and safety notes

- Xposed scope is limited to `com.android.settings` in `app/src/main/resources/META-INF/xposed/scope.list`.
- Veyra does not replace the Samsung Settings APK, patch `/system`, or ship a Zygisk native component.
- If Settings becomes unstable, disable Veyra in LSPosed and force-stop Settings before reopening it.
- The glass material is a frosted/tinted approximation, not Apple's proprietary optical refraction.
- View-tree styling and classification use traversal caps, and saved view state is restored when a setting is switched off or a recycled preference row changes identity.
- The source package has static checks, but an APK build and device runtime test are still required before calling this release verified.

## Target profile

The source notes in `docs/TARGET-ANALYSIS.md` describe the supplied One UI 8.5 / Android 16 Settings target:

- Settings package: `com.android.settings`
- Runtime target: Android 16 / API 36
- Main activity: `com.android.settings.homepage.SettingsHomepageActivity`
- Main fragment: `com.android.settings.homepage.TopLevelSettings`
- Sub-settings activity: `com.android.settings.SubSettings`

The device identifiers in the supplied dump contain mixed/spoofed model information. Veyra therefore scopes by the Settings package and does not use the reported model name as a runtime gate.

## Build toolchain

The Android build uses one consistent toolchain:

| Component | Version |
|---|---:|
| Android Gradle Plugin | 9.1.1 |
| Gradle | 9.3.1 |
| JDK | 17 |
| Kotlin | AGP built-in Kotlin (KGP 2.2.10 runtime) |
| Compile SDK | Android 17 / API 37.0 |
| Target SDK | API 36 |
| Build Tools | 36.0.0 |
| libxposed API | 102.0.0 |
| libxposed service | 102.0.0 |

`compileSdk` is set to Android 17 API 37.0 because libxposed 102 AAR metadata requires API 37 or later. `targetSdk` intentionally remains 36 so this module does not opt into Android 17 target behavior. Android publishes the 37.0 platform as the minor-versioned package `platforms;android-37.0`; the project uses the AGP minor-version `compileSdk` DSL instead of the older integer-only shorthand.

Official compatibility references:

- [AGP 9.1.1 compatibility and API 37.0](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [Android SDK minor API `compileSdk` syntax](https://developer.android.com/build)
- [libxposed API](https://github.com/libxposed/api)
- [libxposed service](https://github.com/libxposed/service)

## Build with GitHub Actions

1. Push this source to the `main` branch of your GitHub repository.
2. Open **Actions** and run **Build Veyra Settings 1.0.1**.
3. Wait for the `Assemble release APK` step to finish.
4. Open the successful run and download the `Veyra-Settings-1.0.1` artifact.
5. The APK is `Veyra-Settings-1.0.1.apk`; `SHA256SUMS.txt` contains its checksum.

The workflow installs the Android 37.0 SDK platform and Build Tools 36.0.0, validates project metadata, builds `:app:assembleRelease`, and uploads the APK. It does not request the removed SDK package named `tools`.

### Update an existing Termux clone

Extract this ZIP into a **new** folder, then run the included helper from that extracted folder:

```sh
bash tools/apply-upgrade-termux.sh /storage/emulated/0/Download/Veyra-Xposed-1.0.0-Premium
```

The helper copies the new source files into the existing Git clone, preserves its `.git` directory, stages changes, commits, and pushes `main`. Change the destination path if your current clone is stored elsewhere.

## Install and activate

1. Install `Veyra-Settings-1.0.1.apk` normally.
2. Enable **Veyra Settings** in LSPosed.
3. Confirm that the only scope is `com.android.settings`.
4. Force-stop Settings or reboot so its process is recreated with the module.
5. Open the Veyra dashboard and check the LSPosed service status.
6. Open Android Settings and inspect the home screen and sub-pages.

The release variant is signed with the GitHub runner's debug key for personal testing. Fresh GitHub runners can use different debug keys, so installing a later build over an earlier one may require uninstalling the previous APK first. Configure a private signing key through GitHub Secrets before distributing repeatable updates.

## Diagnostics

Optional diagnostics record view class names, resource IDs, and dimensions only. They do not read visible text, account details, Wi-Fi names, or preference values. Enable diagnostics in the dashboard, visit Settings, and run:

```sh
bash tools/collect-diagnostics.sh
```

Review the generated file before sharing it.

## Static validation versus full verification

Run:

```sh
python tools/verify_static.py
```

This checks manifest declarations, Xposed metadata, dependency and toolchain alignment, scope restriction, and required source files. It does **not** replace `:app:assembleRelease`, LSPosed activation, or device testing. This environment has not produced or verified the final APK.

## Current implementation limits

- The UI engine restyles existing Android/Samsung views and preference rows; it does not reconstruct every Samsung page into a completely new iOS navigation hierarchy.
- Search placement, per-page row grouping, exact icons, and Samsung-specific controls still need runtime captures and screenshots from the exact firmware build.
- `GlassSurfaceDrawable` uses a gradient/tint and highlight rather than true optical blur/refraction.
