# Veyra Settings 1.0.1 - structured source update

## Build structure

- Aligns Android Gradle Plugin 9.1.1, Gradle 9.3.1, JDK 17, and AGP built-in Kotlin.
- Compiles against Android API 37.0, required by the selected libxposed 102.0.0 artifacts, while retaining targetSdk 36.
- Requests the exact SDK package `platforms;android-37.0`; it no longer requests the removed `tools` package.
- Uses the published Maven Central coordinates `io.github.libxposed:api:102.0.0` and `io.github.libxposed:service:102.0.0`.

## Runtime hardening

- Restores full preference-row subtrees when a recycled row changes identity or Veyra is disabled.
- Saves and restores switch thumb/track tint state when the target implementation exposes readable tint getters.
- Applies explicit node-count and depth limits to Settings page classification and UI styling.
- Keeps diagnostics limited to view class names, resource IDs, dimensions, and clickable state.

## Validation

- `python tools/verify_static.py`: 32 static checks pass.
- Shell scripts pass `bash -n`.
- Android manifest and resource XML files parse.
- GitHub Actions workflow YAML parses.
- No Android SDK/Gradle build was run in this workspace; successful APK compilation and device runtime behavior remain unverified until the GitHub Actions build and phone test complete.
