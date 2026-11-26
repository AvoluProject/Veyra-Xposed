# Supplied target analysis

Analysis source: user-supplied `SecSettings.apk` and `device-info.txt`.

## Device profile

- Android release: 16
- SDK: 36
- One UI property: `ro.build.version.oneui=80500` (8.5)
- Reported model: `SM-A346N`
- Reported device codename: `a22`
- Build identifier: `A346NKSUEFZE6`
- Settings package: `com.android.settings`

The identity fields are mixed, which is consistent with a port/spoofed product profile. Veyra therefore targets the supplied APK and runtime class tree rather than assuming a stock A22/A34 implementation.

## Manifest and code facts

- Settings package: `com.android.settings`
- APK version name: `16`
- `SettingsHomepageActivity`
- `SubSettings`
- `TopLevelSettings`
- Samsung connections fragment: `com.samsung.android.settings.connection.ConnectionsSettings`
- Package is a platform/system Settings app and declares the Android system shared UID. Do not try to replace it with the Veyra APK or install the supplied Settings APK as a normal app.

## Confirmed resource keys/layouts

Top-level categories:

- `top_level_account_category`
- `top_level_connectivity_category`
- `top_level_personalize_category`
- `top_level_system_info_category`
- `top_level_security_privacy_category`
- `top_level_support_category`

Other target XML resources:

- `top_level_settings.xml`
- `top_level_settings_expressive.xml`
- `sec_top_level_settings.xml`
- `sec_wireless_settings.xml`
- `display_settings.xml`
- `sec_display_settings.xml`
- `sound_settings.xml`
- `bluetooth_settings.xml`
- `wifi_settings.xml`
- `my_device_info.xml`
- `sec_my_device_info.xml`

Relevant layout names:

- `homepage_preference.xml`
- `homepage_preference_expressive.xml`
- `card_preference.xml`
- `preference_material.xml`
- `search_bar.xml`

The APK contains AndroidX Preference classes including `Preference`, `PreferenceCategory`, `PreferenceViewHolder`, `PreferenceGroupAdapter`, and `PreferenceFragmentCompat`.

## Implementation consequence

The first pass hooks `Activity`/AndroidX Fragment lifecycle and `PreferenceGroupAdapter.onBindViewHolder`. This keeps Samsung's real preference objects and click/controller handlers intact while changing presentation. Runtime diagnostics must be captured before writing any hard-coded class-specific transformations.
