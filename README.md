# Veyra Xposed 1.0.0

**Goal:** a deeply iOS-26-inspired visual and interaction layer for the real Samsung Settings app on the supplied One UI 8.5 / Android 16 target.

Veyra is a single installable APK: the LSPosed module and its configuration dashboard. Zygisk is intentionally optional and not shipped in 1.0.0 because the current work only needs Java/Kotlin hooks inside `com.android.settings`.

## Target profile confirmed from supplied files

- Settings package: `com.android.settings`
- Android 16 / API 36
- One UI property 80500 (8.5)
- Supplied product identity: SM-A346N / device codename `a22` / build `A346NKSUEFZE6`
- Main Settings activity: `com.android.settings.homepage.SettingsHomepageActivity`
- Main fragment: `com.android.settings.homepage.TopLevelSettings`
- Sub-settings activity: `com.android.settings.SubSettings`

See [`docs/TARGET-ANALYSIS.md`](docs/TARGET-ANALYSIS.md).

## Architecture

- `VeyraModule`: modern libxposed 102 entry point.
- `SettingsHookInstaller`: process hooks limited to the Settings package.
- `SettingsPageClassifier`: recognizes known pages from activity/fragment/resource signals.
- `SettingsUiEngine`: presentation-only view styling; does not replace preference listeners or system controllers.
- `GlassSurfaceDrawable`: low-cost frosted-glass approximation.
- `VeyraMotionEngine`: spring entrance motion.
- `VeyraDiagnostics`: view classes/resource IDs/sizes only; avoids logging visible text and user values.
- `MainActivity`: dashboard writes to libxposed RemotePreferences.

The supplied Settings APK is used as an analysis target only. It is a system app and must not be installed over the firmware app.

## Build with GitHub Actions (recommended from Android/Termux)

1. Create an empty GitHub repository named `Veyra-Xposed`.
2. Extract this ZIP into Termux shared storage. Then run:

   ```sh
   pkg update -y
   pkg install git unzip -y
   termux-setup-storage
   cd ~/storage/downloads
   unzip Veyra-Xposed-1.0.0-Premium-src.zip
   cd Veyra-Xposed-1.0.0-Premium
   git init -b main
   git add .
   git commit -m "Veyra Settings 1.0.0"
   git remote add origin https://github.com/USERNAME/Veyra-Xposed.git
   git push -u origin main
   ```

   Replace `USERNAME` with the GitHub account name. GitHub may ask you to authenticate in a browser/token flow.

3. Open the repository on GitHub, then **Actions** → **Build Veyra Settings 1.0.0**. The workflow installs JDK 17, Android SDK 37, and Gradle 9.3.1 and runs `:app:assembleRelease`.
4. When the run is green, open the run → **Artifacts** → `Veyra-Settings-1.0.0`. Download and extract it. The installable APK is `Veyra-Settings-1.0.0.apk`.

The Release variant is signed with the workflow runner's debug key so the APK is installable without uploading a private key. Each fresh GitHub runner may create a different debug signing key; a later rebuild may therefore require uninstalling the previous APK before installing the new one. For long-term update continuity, configure a private signing key through GitHub Secrets before distributing the module.

## Install and activate

1. Install `Veyra-Settings-1.0.0.apk` normally.
2. Open LSPosed Manager and enable **Veyra Settings**.
3. Confirm its scope is `com.android.settings` only. `scope.list` is already restricted to this package.
4. Force-stop the Settings app (or reboot) so its process is recreated with the module.
5. Open Veyra Settings dashboard. Confirm that the LSPosed connection status is connected.
6. Open Android Settings and inspect the home page and sub-pages.
7. Enable diagnostics temporarily if required. Visit Settings and run this in Termux:

   ```sh
   bash tools/collect-diagnostics.sh
   ```

   It saves `Download/VeyraDiagnostics.txt` with class names and resource IDs only; do not share passwords, Wi-Fi credentials, account data, or other private screen contents.

## Rollback

Disable Veyra Settings in LSPosed and force-stop Settings. The module stores original in-memory view properties and does not replace the system APK or patch system partitions. If a Settings page crashes, disable the module from LSPosed before opening Settings again.

## Important fidelity note

The 1.0.0 source package is a premium-oriented implementation baseline, not a claim of verified 100% pixel identity. It restyles the real Settings view tree and bound preference rows while preserving native controllers. A truly close per-page match needs runtime screenshots and class/resource diagnostics from the exact installed build; unknown fragments should not be guessed. Liquid Glass here is a lightweight frosted/tinted material, not Apple's proprietary optical implementation.

## Build versions

- Android Gradle Plugin: 9.1.0
- Gradle: 9.3.1
- Kotlin Gradle Plugin: 2.4.0
- Compile SDK: 37 (the module still targets Android 16 / API 36)
- Android compile SDK: 37 / target SDK: 36
- Java: 17
- libxposed API: 102.0.0
- libxposed service: 102.0.1
