package dev.veyra.xposed.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import dev.veyra.xposed.R
import dev.veyra.xposed.VeyraConfig
import dev.veyra.xposed.target.SettingsPageClassifier
import java.util.WeakHashMap

/** Presentation-only styling for the target's existing view tree and real preference holders. */
object SettingsUiEngine {
    private const val TAG = "VeyraUI"
    private val windowStates = WeakHashMap<Window, WindowState>()

    fun currentPage(activity: Activity, root: View?): SettingsPageClassifier.Page =
        SettingsPageClassifier.classify(activity, null, root).page

    fun apply(activity: Activity, config: VeyraConfig, fragmentClass: String?) {
        if (activity.packageName != "com.android.settings" || activity.isFinishing || activity.isDestroyed) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val palette = Palette.from(activity)
        val page = SettingsPageClassifier.classify(activity, fragmentClass, content)
        val signature = 31 * config.hashCode() + page.page.hashCode()
        val previous = content.getTag(R.id.veyra_signature) as? Int

        if (!config.enabled) {
            restoreTree(content)
            restoreWindow(activity.window)
            content.setTag(R.id.veyra_signature, signature)
            return
        }

        if (previous != null && previous != signature) {
            restoreTree(content)
            restoreWindow(activity.window)
        }
        snapshot(content)
        content.setBackgroundColor(palette.page)
        if (config.systemBars) applySystemBars(activity.window, palette) else restoreWindow(activity.window)
        styleViewTree(activity, content, palette, config, 0)
        content.setTag(R.id.veyra_signature, signature)

        if (config.springMotion) VeyraMotionEngine.animatePage(activity, content, page.page.name, config.motionStrength)
        if (config.diagnostics) VeyraDiagnostics.dumpIfChanged(activity, content, page)
    }

    /** Called after AndroidX has bound an actual PreferenceViewHolder. No listeners are replaced. */
    fun applyPreferenceItem(
        itemView: View,
        activity: Activity,
        config: VeyraConfig,
        page: SettingsPageClassifier.Page,
        preferenceKey: String?,
        preferenceClass: String,
        firstInGroup: Boolean,
        lastInGroup: Boolean,
    ) {
        if (!config.enabled || !config.groupedCards) return
        val palette = Palette.from(activity)
        val category = preferenceClass.contains("PreferenceCategory") ||
            preferenceKey in setOf(
                "top_level_account_category", "top_level_connectivity_category",
                "top_level_personalize_category", "top_level_system_info_category",
                "top_level_security_privacy_category", "top_level_support_category"
            ) || preferenceKey?.endsWith("_category") == true

        val signature = 31 * config.hashCode() + (preferenceKey?.hashCode() ?: preferenceClass.hashCode())
        val oldSignature = itemView.getTag(R.id.veyra_signature) as? Int
        if (oldSignature != null && oldSignature != signature) restoreTree(itemView)
        val state = snapshot(itemView)

        if (category) {
            itemView.background = ColorDrawable(Color.TRANSPARENT)
            itemView.setPadding(state.paddingLeft + dp(activity, 18), state.paddingTop + dp(activity, 8),
                state.paddingRight + dp(activity, 12), state.paddingBottom + dp(activity, 4))
            itemView.minimumHeight = dp(activity, 34)
            styleCategoryText(itemView, palette)
        } else {
            val homeTile = page == SettingsPageClassifier.Page.HOME ||
                preferenceClass.contains("HomepagePreference", ignoreCase = true)
            val radius = config.cornerRadius.toFloat() + if (homeTile) 2f else -2f
            itemView.background = if (config.glassLite) {
                GlassSurfaceDrawable(dp(activity, radius).toFloat(), palette.isLight,
                    config.glassStrength, palette.stroke, firstInGroup, lastInGroup, !lastInGroup)
            } else {
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(
                        if (firstInGroup) dp(activity, radius).toFloat() else 0f,
                        if (firstInGroup) dp(activity, radius).toFloat() else 0f,
                        if (firstInGroup) dp(activity, radius).toFloat() else 0f,
                        if (firstInGroup) dp(activity, radius).toFloat() else 0f,
                        if (lastInGroup) dp(activity, radius).toFloat() else 0f,
                        if (lastInGroup) dp(activity, radius).toFloat() else 0f,
                        if (lastInGroup) dp(activity, radius).toFloat() else 0f,
                        if (lastInGroup) dp(activity, radius).toFloat() else 0f,
                    )
                    setColor(palette.surface)
                    setStroke(dp(activity, 1), palette.stroke)
                }
            }
            val vertical = if (homeTile) 8 else 5
            itemView.setPadding(state.paddingLeft + dp(activity, 13), state.paddingTop + dp(activity, vertical),
                state.paddingRight + dp(activity, 13), state.paddingBottom + dp(activity, vertical))
            itemView.minimumHeight = dp(activity, if (homeTile) 68 else 58)
            applyRowMargins(itemView, activity, if (homeTile || firstInGroup) 5 else 0,
                if (homeTile || lastInGroup) 5 else 0)
            itemView.clipToOutline = true
            itemView.elevation = dp(activity, 0.5f).toFloat()
            styleViewTree(activity, itemView, palette, config, 0)
        }
        itemView.setTag(R.id.veyra_signature, signature)
    }

    private fun styleCategoryText(root: View, palette: Palette, depth: Int = 0, visited: IntArray = intArrayOf(0)) {
        if (depth > 12 || visited[0] >= 240) return
        visited[0]++
        // Category labels are styled recursively, so save every descendant before mutation.
        snapshot(root)
        if (root is TextView) {
            root.setTextColor(palette.secondaryText)
            root.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            root.textSize = 12.5f
            root.letterSpacing = 0.035f
            root.isAllCaps = false
        }
        if (root is ViewGroup) {
            for (i in 0 until minOf(root.childCount, 40)) {
                if (visited[0] >= 240) break
                styleCategoryText(root.getChildAt(i), palette, depth + 1, visited)
            }
        }
    }

    private fun styleViewTree(
        activity: Activity, view: View, palette: Palette, config: VeyraConfig,
        depth: Int, visited: IntArray = intArrayOf(0)
    ) {
        if (depth > 17 || visited[0] >= 600) return
        visited[0]++
        snapshot(view)
        val entry = resourceName(view).orEmpty().lowercase()
        val className = view.javaClass.name.lowercase()

        if (config.typography && view is TextView) {
            val sizeSp = view.textSize / activity.resources.displayMetrics.scaledDensity
            val secondary = sizeSp <= 13.5f || view.isAllCaps || entry.contains("summary")
            view.setTextColor(if (secondary) palette.secondaryText else palette.primaryText)
            view.typeface = Typeface.create("sans-serif", view.typeface?.style ?: Typeface.NORMAL)
            if (!view.isAllCaps) view.letterSpacing = 0f
        }

        if (config.controlStyling) {
            when (view) {
                is CompoundButton -> {
                    view.buttonTintList = ColorStateList.valueOf(palette.accent)
                    tintSwitchByReflection(view, palette)
                }
                is SeekBar -> {
                    view.progressTintList = ColorStateList.valueOf(palette.accent)
                    view.progressBackgroundTintList = ColorStateList.valueOf(palette.track)
                    view.thumbTintList = ColorStateList.valueOf(palette.accent)
                }
                is ProgressBar -> {
                    view.progressTintList = ColorStateList.valueOf(palette.accent)
                    view.progressBackgroundTintList = ColorStateList.valueOf(palette.track)
                }
                is EditText -> if (entry.contains("search") || entry.contains("query") || className.contains("search")) {
                    view.setTextColor(palette.primaryText)
                    view.setHintTextColor(palette.secondaryText)
                    view.background = GlassSurfaceDrawable(dp(activity, 16f).toFloat(), palette.isLight,
                        config.glassStrength, palette.stroke)
                }
            }
        }

        if (view is ViewGroup) {
            if (view is ScrollView || className.contains("recyclerview")) {
                view.clipToPadding = false
                view.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                view.isVerticalScrollBarEnabled = false
            }
            for (index in 0 until minOf(view.childCount, 120)) {
                if (visited[0] >= 600) break
                styleViewTree(activity, view.getChildAt(index), palette, config, depth + 1, visited)
            }
        }
    }

    private fun tintSwitchByReflection(view: CompoundButton, palette: Palette) {
        if (!view.javaClass.name.contains("switch", ignoreCase = true)) return
        // Only tint implementations whose original colors can also be read and restored.
        val restorable = runCatching {
            view.javaClass.getMethod("getThumbTintList")
            view.javaClass.getMethod("getTrackTintList")
            view.javaClass.getMethod("setThumbTintList", ColorStateList::class.java)
            view.javaClass.getMethod("setTrackTintList", ColorStateList::class.java)
        }.isSuccess
        if (!restorable) return
        val checked = ColorStateList.valueOf(Color.WHITE)
        val unchecked = ColorStateList.valueOf(if (palette.isLight) 0xFFF7F7F8.toInt() else 0xFFE5E5EA.toInt())
        val thumb = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(checked.defaultColor, unchecked.defaultColor))
        val track = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(0xFF34C759.toInt(), palette.track))
        runCatching { view.javaClass.getMethod("setThumbTintList", ColorStateList::class.java).invoke(view, thumb) }
        runCatching { view.javaClass.getMethod("setTrackTintList", ColorStateList::class.java).invoke(view, track) }
    }

    private fun applyRowMargins(view: View, activity: Activity, topDp: Int, bottomDp: Int) {
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val saved = view.getTag(R.id.veyra_snapshot) as? ViewState ?: return
        lp.setMargins(saved.marginLeft, saved.marginTop + dp(activity, topDp), saved.marginRight,
            saved.marginBottom + dp(activity, bottomDp))
        view.layoutParams = lp
    }

    private fun applySystemBars(window: Window, palette: Palette) {
        synchronized(windowStates) {
            if (!windowStates.containsKey(window)) {
                windowStates[window] = WindowState(window.statusBarColor, window.navigationBarColor,
                    window.decorView.systemUiVisibility,
                    if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced else null)
            }
        }
        window.statusBarColor = palette.page
        window.navigationBarColor = palette.page
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        var flags = window.decorView.systemUiVisibility
        flags = if (palette.isLight) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        window.decorView.systemUiVisibility = flags
    }

    private fun restoreWindow(window: Window) {
        val saved = synchronized(windowStates) { windowStates.remove(window) } ?: return
        window.statusBarColor = saved.status
        window.navigationBarColor = saved.navigation
        window.decorView.systemUiVisibility = saved.flags
        if (Build.VERSION.SDK_INT >= 29 && saved.navigationContrast != null) {
            window.isNavigationBarContrastEnforced = saved.navigationContrast
        }
    }

    private fun snapshot(view: View): ViewState {
        (view.getTag(R.id.veyra_snapshot) as? ViewState)?.let { return it }
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
        val state = ViewState(
            background = view.background,
            foreground = if (Build.VERSION.SDK_INT >= 23) view.foreground else null,
            paddingLeft = view.paddingLeft, paddingTop = view.paddingTop,
            paddingRight = view.paddingRight, paddingBottom = view.paddingBottom,
            minimumHeight = view.minimumHeight,
            textColors = (view as? TextView)?.textColors,
            hintColors = (view as? EditText)?.hintTextColors,
            typeface = (view as? TextView)?.typeface,
            textSizePx = (view as? TextView)?.textSize,
            allCaps = (view as? TextView)?.isAllCaps,
            letterSpacing = (view as? TextView)?.letterSpacing,
            buttonTint = (view as? CompoundButton)?.buttonTintList,
            switchThumbTint = readSwitchTint(view, "getThumbTintList"),
            switchTrackTint = readSwitchTint(view, "getTrackTintList"),
            hasSwitchThumbTint = hasSwitchTintGetter(view, "getThumbTintList"),
            hasSwitchTrackTint = hasSwitchTintGetter(view, "getTrackTintList"),
            progressTint = (view as? ProgressBar)?.progressTintList,
            progressBackgroundTint = (view as? ProgressBar)?.progressBackgroundTintList,
            seekThumbTint = (view as? SeekBar)?.thumbTintList,
            marginLeft = lp?.leftMargin ?: 0, marginTop = lp?.topMargin ?: 0,
            marginRight = lp?.rightMargin ?: 0, marginBottom = lp?.bottomMargin ?: 0,
            elevation = view.elevation, clipToOutline = view.clipToOutline,
            clipToPadding = (view as? ViewGroup)?.clipToPadding,
            overScrollMode = view.overScrollMode,
            verticalScrollBarEnabled = view.isVerticalScrollBarEnabled,
        )
        view.setTag(R.id.veyra_snapshot, state)
        return state
    }

    private fun restoreOne(view: View) {
        val s = view.getTag(R.id.veyra_snapshot) as? ViewState ?: return
        view.background = s.background
        if (Build.VERSION.SDK_INT >= 23) view.foreground = s.foreground
        view.setPadding(s.paddingLeft, s.paddingTop, s.paddingRight, s.paddingBottom)
        view.minimumHeight = s.minimumHeight
        (view as? TextView)?.let {
            s.textColors?.let { colors -> it.setTextColor(colors) }
            s.typeface?.let { type -> it.typeface = type }
            s.textSizePx?.let { size -> it.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size) }
            s.allCaps?.let { allCaps -> it.isAllCaps = allCaps }
            s.letterSpacing?.let { spacing -> it.letterSpacing = spacing }
        }
        (view as? EditText)?.let { s.hintColors?.let(it::setHintTextColor) }
        (view as? CompoundButton)?.buttonTintList = s.buttonTint
        if (s.hasSwitchThumbTint) restoreSwitchTint(view, "setThumbTintList", s.switchThumbTint)
        if (s.hasSwitchTrackTint) restoreSwitchTint(view, "setTrackTintList", s.switchTrackTint)
        (view as? ProgressBar)?.let {
            it.progressTintList = s.progressTint
            it.progressBackgroundTintList = s.progressBackgroundTint
        }
        (view as? SeekBar)?.thumbTintList = s.seekThumbTint
        view.elevation = s.elevation
        view.clipToOutline = s.clipToOutline
        if (view is ViewGroup) s.clipToPadding?.let { clip -> view.clipToPadding = clip }
        view.overScrollMode = s.overScrollMode
        view.isVerticalScrollBarEnabled = s.verticalScrollBarEnabled
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
        if (lp != null) {
            lp.setMargins(s.marginLeft, s.marginTop, s.marginRight, s.marginBottom)
            view.layoutParams = lp
        }
        view.setTag(R.id.veyra_snapshot, null)
        view.setTag(R.id.veyra_signature, null)
    }

    private fun readSwitchTint(view: View, getter: String): ColorStateList? =
        if (view !is CompoundButton) null else runCatching {
            view.javaClass.getMethod(getter).invoke(view) as? ColorStateList
        }.getOrNull()

    private fun hasSwitchTintGetter(view: View, getter: String): Boolean =
        view is CompoundButton && runCatching { view.javaClass.getMethod(getter) }.isSuccess

    private fun restoreSwitchTint(view: View, setter: String, value: ColorStateList?) {
        if (view !is CompoundButton) return
        runCatching {
            view.javaClass.getMethod(setter, ColorStateList::class.java).invoke(view, value)
        }
    }

    private fun restoreTree(view: View) {
        restoreOne(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) restoreTree(view.getChildAt(i))
    }

    private fun resourceName(view: View): String? = if (view.id == View.NO_ID) null else
        runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull()

    private fun dp(activity: Activity, value: Int): Int = dp(activity, value.toFloat())
    private fun dp(activity: Activity, value: Float): Int = (value * activity.resources.displayMetrics.density).toInt()

    private data class ViewState(
        val background: Drawable?, val foreground: Drawable?,
        val paddingLeft: Int, val paddingTop: Int, val paddingRight: Int, val paddingBottom: Int,
        val minimumHeight: Int, val textColors: ColorStateList?, val hintColors: ColorStateList?,
        val typeface: Typeface?, val textSizePx: Float?, val allCaps: Boolean?,
        val letterSpacing: Float?, val buttonTint: ColorStateList?,
        val switchThumbTint: ColorStateList?, val switchTrackTint: ColorStateList?,
        val hasSwitchThumbTint: Boolean, val hasSwitchTrackTint: Boolean,
        val progressTint: ColorStateList?, val progressBackgroundTint: ColorStateList?, val seekThumbTint: ColorStateList?,
        val marginLeft: Int, val marginTop: Int, val marginRight: Int, val marginBottom: Int,
        val elevation: Float, val clipToOutline: Boolean,
        val clipToPadding: Boolean?, val overScrollMode: Int,
        val verticalScrollBarEnabled: Boolean,
    )

    private data class WindowState(
        val status: Int, val navigation: Int, val flags: Int, val navigationContrast: Boolean?
    )

    private data class Palette(
        val isLight: Boolean, val page: Int, val surface: Int, val primaryText: Int,
        val secondaryText: Int, val stroke: Int, val track: Int, val accent: Int,
    ) {
        companion object {
            fun from(activity: Activity): Palette {
                val night = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                return if (night) Palette(false, 0xFF000000.toInt(), 0xFF1C1C1E.toInt(),
                    0xFFF5F5F7.toInt(), 0xFF98989F.toInt(), 0x35FFFFFF, 0xFF3A3A3C.toInt(), 0xFF0A84FF.toInt())
                else Palette(true, 0xFFF2F2F7.toInt(), 0xFFFFFFFF.toInt(), 0xFF1C1C1E.toInt(),
                    0xFF8E8E93.toInt(), 0x1F000000, 0xFFD1D1D6.toInt(), 0xFF007AFF.toInt())
            }
        }
    }
}
