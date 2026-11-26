package dev.veyra.xposed

import android.content.SharedPreferences

/** Primitive-only settings shared through libxposed RemotePreferences. */
data class VeyraConfig(
    val enabled: Boolean = true,
    val groupedCards: Boolean = true,
    val glassLite: Boolean = true,
    val typography: Boolean = true,
    val controlStyling: Boolean = true,
    val springMotion: Boolean = true,
    val systemBars: Boolean = true,
    val diagnostics: Boolean = false,
    val glassStrength: Int = 82,
    val motionStrength: Int = 100,
    val cornerRadius: Int = 20,
) {
    companion object {
        const val GROUP = "veyra_settings_premium"
        const val ENABLED = "enabled"
        const val GROUPED = "grouped_cards"
        const val GLASS = "glass_lite"
        const val TYPOGRAPHY = "typography"
        const val CONTROLS = "control_styling"
        const val MOTION = "spring_motion"
        const val SYSTEM_BARS = "system_bars"
        const val DIAGNOSTICS = "diagnostics"
        const val GLASS_STRENGTH = "glass_strength"
        const val MOTION_STRENGTH = "motion_strength"
        const val CORNER_RADIUS = "corner_radius"

        val defaults = mapOf(
            ENABLED to true,
            GROUPED to true,
            GLASS to true,
            TYPOGRAPHY to true,
            CONTROLS to true,
            MOTION to true,
            SYSTEM_BARS to true,
            DIAGNOSTICS to false,
            GLASS_STRENGTH to 82,
            MOTION_STRENGTH to 100,
            CORNER_RADIUS to 20,
        )

        fun from(prefs: SharedPreferences?): VeyraConfig {
            if (prefs == null) return VeyraConfig()
            return VeyraConfig(
                enabled = prefs.getBoolean(ENABLED, true),
                groupedCards = prefs.getBoolean(GROUPED, true),
                glassLite = prefs.getBoolean(GLASS, true),
                typography = prefs.getBoolean(TYPOGRAPHY, true),
                controlStyling = prefs.getBoolean(CONTROLS, true),
                springMotion = prefs.getBoolean(MOTION, true),
                systemBars = prefs.getBoolean(SYSTEM_BARS, true),
                diagnostics = prefs.getBoolean(DIAGNOSTICS, false),
                glassStrength = prefs.getInt(GLASS_STRENGTH, 82).coerceIn(0, 100),
                motionStrength = prefs.getInt(MOTION_STRENGTH, 100).coerceIn(50, 150),
                cornerRadius = prefs.getInt(CORNER_RADIUS, 20).coerceIn(12, 30),
            )
        }
    }
}
