# Implementation status: 1.0.1 structured source package

## Included

- Modern libxposed API 102 entry point.
- Scope restricted to `com.android.settings`.
- Dashboard with RemotePreferences controls.
- Hooks for Activity resume/destroy, AndroidX Fragment resume, and AndroidX PreferenceGroupAdapter binding.
- Target page classifier seeded from the supplied Settings APK.
- Light/dark palette, frosted-glass drawable, typography/control styling, system-bar styling, spring entrance, and privacy-safe view-tree diagnostics.
- GitHub Actions build job for a signed personal-use debug APK.

## Not claimed as complete

- This package has not been compiled by Android Gradle Plugin in this workspace.
- Runtime behavior has not been verified on the phone.
- The current engine restyles real existing Android views and preference rows; it does not yet reconstruct every Samsung page into a fully new iOS navigation hierarchy.
- Search placement, per-page row grouping, precise icons, and some Samsung-specific controls need runtime tree captures and iterative device screenshots.
- `GlassSurfaceDrawable` is a frosted/tinted surface, not true optical refraction. Hardware blur is not enabled globally because it can be expensive and can affect content readability.

## Safe rollback

If Settings behaves incorrectly, disable Veyra in LSPosed and force-stop Settings. Veyra does not patch or replace the system Settings APK and does not modify `/system`.


## Build structure update (1.0.1)

- Pinned Android Gradle Plugin 9.1.1, Gradle 9.3.1, JDK 17, and AGP built-in Kotlin.
- Uses the minor-versioned Android 17 compile platform API 37.0 required by libxposed 102.
- Uses the published Maven Central artifacts `io.github.libxposed:api:102.0.0` and `io.github.libxposed:service:102.0.0`.
- Keeps `targetSdk` at 36; `compileSdk` is not the same as `targetSdk`.
- Adds a Termux in-place upgrade helper and stricter offline metadata checks.
- Saves and restores additional view properties when Veyra is disabled, including text size, all-caps state, scroll settings, switch thumb/track tints, and category descendants.
- Bounds Settings view-tree classification, styling, and diagnostics to avoid unbounded recursive walks on large preference hierarchies.

The Android release build still needs to be run by GitHub Actions. Static validation is not proof of successful APK compilation or runtime compatibility.
