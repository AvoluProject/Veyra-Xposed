package dev.veyra.dashboard

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import dev.veyra.xposed.VeyraConfig
import dev.veyra.xposed.VeyraServiceManager
import kotlin.math.roundToInt

/** Native dashboard. All controls write real libxposed RemotePreferences. */
class MainActivity : Activity() {
    private val ink = Color.rgb(25, 27, 33)
    private val muted = Color.rgb(117, 120, 130)
    private val accent = Color.rgb(10, 132, 255)
    private val canvas = Color.rgb(242, 243, 248)
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private var observer: (() -> Unit)? = null
    private val controlRefreshers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val scroll = ScrollView(this).apply {
            setBackgroundColor(canvas)
            clipToPadding = false
            isVerticalScrollBarEnabled = false
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(36))
        }
        scroll.addView(root)
        setContentView(scroll)

        buildHeader()
        buildStatusCard()
        buildAppearanceSection()
        buildMotionSection()
        buildSystemSection()
        buildFooter()

        observer = {
            refreshStatus()
            refreshControls()
        }
        VeyraServiceManager.addObserver(observer!!)
        refreshStatus()
        refreshControls()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshControls()
    }

    override fun onDestroy() {
        observer?.let(VeyraServiceManager::removeObserver)
        super.onDestroy()
    }

    private fun buildHeader() {
        val eyebrow = text("SYSTEM EXPERIENCE", 11f, muted, true)
        eyebrow.letterSpacing = 0.16f
        root.addView(eyebrow)
        root.addView(text("Veyra", 38f, ink, true).apply {
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(text("A liquid-glass inspired interface engine for Samsung Settings.", 14f, muted).apply {
            setPadding(0, dp(4), 0, dp(18))
        })
    }

    private fun buildStatusCard() {
        val card = card()
        status = text("Checking LSPosed connection…", 14f, ink, true)
        card.addView(status)
        card.addView(text("Target: com.android.settings · One UI 8.5 · API 36", 12f, muted).apply {
            setPadding(0, dp(7), 0, 0)
        })
        root.addView(card, sectionParams())
    }

    private fun buildAppearanceSection() {
        sectionTitle("APPEARANCE")
        val card = card()
        addToggle(card, "Enable Veyra", "Master switch for Settings styling", VeyraConfig.ENABLED, true)
        addDivider(card)
        addToggle(card, "Grouped glass cards", "Rounded surfaces and cleaner row spacing", VeyraConfig.GROUPED, true)
        addDivider(card)
        addToggle(card, "Frosted glass Lite", "Gradient glass surface with a subtle edge highlight", VeyraConfig.GLASS, true)
        addDivider(card)
        addToggle(card, "iOS-style typography", "Refined hierarchy and lighter text weight", VeyraConfig.TYPOGRAPHY, true)
        addDivider(card)
        addToggle(card, "System controls", "Restyle real switches, sliders and search fields", VeyraConfig.CONTROLS, true)
        addSlider(card, "Glass intensity", "Surface opacity and highlight strength", VeyraConfig.GLASS_STRENGTH, 0, 100, 82, "%")
        root.addView(card, sectionParams())
    }

    private fun buildMotionSection() {
        sectionTitle("MOTION")
        val card = card()
        addToggle(card, "Spring transitions", "Soft page entrance and responsive motion", VeyraConfig.MOTION, true)
        addDivider(card)
        addSlider(card, "Motion strength", "Adjust animation energy", VeyraConfig.MOTION_STRENGTH, 50, 150, 100, "%")
        addSlider(card, "Corner radius", "Rounded card geometry", VeyraConfig.CORNER_RADIUS, 12, 30, 20, " dp")
        root.addView(card, sectionParams())
    }

    private fun buildSystemSection() {
        sectionTitle("SYSTEM")
        val card = card()
        addToggle(card, "System bar styling", "Coordinate status and navigation bar colors", VeyraConfig.SYSTEM_BARS, true)
        addDivider(card)
        addToggle(card, "Private diagnostics", "Log view class and resource IDs only", VeyraConfig.DIAGNOSTICS, false)
        val reset = TextView(this).apply {
            text = "Restore Veyra defaults"
            textSize = 14f
            setTextColor(accent)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(17), dp(2), dp(5))
            setOnClickListener {
                val ok = VeyraServiceManager.writeDefaults()
                toast(if (ok) "Defaults saved. Reopen Settings." else "LSPosed service is not connected.")
            }
        }
        card.addView(reset)
        root.addView(card, sectionParams())
    }

    private fun buildFooter() {
        root.addView(text("VEYRA SETTINGS 1.0.1", 10f, muted, true).apply {
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        })
        root.addView(text("Built for the supplied One UI 8.5 Settings target. Changes apply to the real Settings process after LSPosed is enabled.", 11f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), 0)
        })
    }

    private fun refreshStatus() {
        if (!::status.isInitialized) return
        val connected = VeyraServiceManager.isConnected()
        status.text = if (connected) "●  LSPosed connected · ${VeyraServiceManager.frameworkLabel()}" else
            "○  LSPosed service not connected"
        status.setTextColor(if (connected) Color.rgb(34, 145, 87) else Color.rgb(190, 111, 27))
    }

    private fun addToggle(parent: LinearLayout, title: String, subtitle: String, key: String, fallback: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(text(title, 14f, ink, true))
        labels.addView(text(subtitle, 11f, muted).apply { setPadding(0, dp(3), 0, 0) })
        row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val toggle = Switch(this)
        fun attachListener() {
            toggle.setOnCheckedChangeListener { _, checked ->
                val saved = VeyraServiceManager.writeBoolean(key, checked)
                if (!saved) toast("LSPosed belum terhubung. Pengaturan belum tersimpan.")
            }
        }
        toggle.isChecked = VeyraServiceManager.readBoolean(key, fallback)
        attachListener()
        controlRefreshers += {
            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = VeyraServiceManager.readBoolean(key, fallback)
            attachListener()
        }

        row.addView(toggle)
        parent.addView(row)
    }

    private fun addSlider(parent: LinearLayout, title: String, subtitle: String, key: String,
                          min: Int, max: Int, fallback: Int, suffix: String) {
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(13), 0, dp(12))
        }
        val labelRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        labelRow.addView(text(title, 14f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        val initialValue = VeyraServiceManager.readInt(key, fallback).coerceIn(min, max)
        val value = text("$initialValue$suffix", 13f, accent, true)
        labelRow.addView(value)
        block.addView(labelRow)
        block.addView(text(subtitle, 11f, muted).apply { setPadding(0, dp(3), 0, dp(4)) })

        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = initialValue - min
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(accent)
        }
        fun attachListener() {
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    value.text = "${progress + min}$suffix"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val current = (seekBar?.progress ?: 0) + min
                    if (!VeyraServiceManager.writeInt(key, current)) toast("LSPosed belum terhubung.")
                }
            })
        }
        attachListener()
        controlRefreshers += {
            val current = VeyraServiceManager.readInt(key, fallback).coerceIn(min, max)
            seek.setOnSeekBarChangeListener(null)
            seek.progress = current - min
            value.text = "$current$suffix"
            attachListener()
        }

        block.addView(seek)
        parent.addView(block)
    }

    private fun refreshControls() {
        if (!VeyraServiceManager.isConnected()) return
        controlRefreshers.forEach { refresher ->
            runCatching(refresher).onFailure {
                android.util.Log.w("VeyraDashboard", "Unable to refresh a control", it)
            }
        }
    }

    private fun sectionTitle(value: String) {
        root.addView(text(value, 10f, muted, true).apply {
            letterSpacing = 0.13f
            setPadding(dp(3), dp(8), dp(3), dp(9))
        })
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(4), dp(17), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(23).toFloat()
            setColor(Color.WHITE)
            setStroke(dp(1), 0x18FFFFFF)
        }
        elevation = dp(1).toFloat()
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(this).apply { setBackgroundColor(0xFFE8E9ED.toInt()) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = true
    }

    private fun sectionParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(17) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun toast(message: String) = android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
}
