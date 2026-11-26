package dev.veyra.xposed.target

/** Facts confirmed from the user-provided Settings APK manifest and device-info dump. */
object TargetProfile {
    const val PACKAGE_NAME = "com.android.settings"
    const val APK_VERSION_NAME = "16"
    const val SDK_LEVEL = 36
    const val ONE_UI_VERSION_CODE = 80500
    const val HOME_ACTIVITY = "com.android.settings.homepage.SettingsHomepageActivity"
    const val HOME_FRAGMENT = "com.android.settings.homepage.TopLevelSettings"
    const val SUB_SETTINGS_ACTIVITY = "com.android.settings.SubSettings"
    const val CONNECTIONS_FRAGMENT = "com.samsung.android.settings.connection.ConnectionsSettings"

    val topLevelCategoryKeys = setOf(
        "top_level_account_category",
        "top_level_connectivity_category",
        "top_level_personalize_category",
        "top_level_system_info_category",
        "top_level_security_privacy_category",
        "top_level_support_category",
    )

    val knownResourceXml = setOf(
        "top_level_settings.xml",
        "top_level_settings_expressive.xml",
        "sec_top_level_settings.xml",
        "sec_wireless_settings.xml",
        "display_settings.xml",
        "sec_display_settings.xml",
        "sound_settings.xml",
        "bluetooth_settings.xml",
        "wifi_settings.xml",
        "my_device_info.xml",
        "sec_my_device_info.xml",
    )

    /** This firmware reports a port/spoofed identity; do not assume a stock A22 class layout. */
    const val BUILD_ID = "A346NKSUEFZE6"
    const val DEVICE_CODENAME = "a22"
    const val REPORTED_MODEL = "SM-A346N"
}
