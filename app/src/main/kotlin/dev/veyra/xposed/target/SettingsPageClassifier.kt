package dev.veyra.xposed.target

import android.app.Activity
import android.view.View
import android.view.ViewGroup

object SettingsPageClassifier {
    enum class Page { HOME, CONNECTIONS, WIFI, BLUETOOTH, DISPLAY, BATTERY, SOUND, ABOUT, SECURITY, OTHER }

    data class Result(val page: Page, val activityClass: String, val fragmentClass: String, val signals: Set<String>)

    fun classify(activity: Activity, fragmentClass: String?, root: View?): Result {
        val activityName = activity.javaClass.name
        val fragmentName = fragmentClass.orEmpty()
        val signals = linkedSetOf(activityName, fragmentName)
        if (root != null) collectResourceNames(root, 0, signals, intArrayOf(0))
        val joined = signals.joinToString("|").lowercase()

        val page = when {
            activityName == TargetProfile.HOME_ACTIVITY || fragmentName == TargetProfile.HOME_FRAGMENT -> Page.HOME
            joined.contains("connectionssettings") || joined.contains("sec_wireless_settings") -> Page.CONNECTIONS
            joined.contains("wifi") || joined.contains("wlan") -> Page.WIFI
            joined.contains("bluetooth") -> Page.BLUETOOTH
            joined.contains("display") || joined.contains("brightness") -> Page.DISPLAY
            joined.contains("battery") || joined.contains("powerusagesummary") -> Page.BATTERY
            joined.contains("sound") || joined.contains("volume") || joined.contains("vibration") -> Page.SOUND
            joined.contains("mydeviceinfo") || joined.contains("firmwareversion") || joined.contains("sec_my_device_info") -> Page.ABOUT
            joined.contains("security") || joined.contains("privacy") || joined.contains("biometric") -> Page.SECURITY
            else -> Page.OTHER
        }
        return Result(page, activityName, fragmentName, signals)
    }

    private fun collectResourceNames(view: View, depth: Int, out: MutableSet<String>, visited: IntArray) {
        if (depth > 12 || visited[0] >= 260) return
        visited[0]++
        val id = view.id
        if (id != View.NO_ID) {
            runCatching { view.resources.getResourceEntryName(id) }.getOrNull()?.let(out::add)
        }
        out += view.javaClass.name
        if (view is ViewGroup) {
            for (i in 0 until minOf(view.childCount, 32)) {
                if (visited[0] >= 260) break
                collectResourceNames(view.getChildAt(i), depth + 1, out, visited)
            }
        }
    }
}
