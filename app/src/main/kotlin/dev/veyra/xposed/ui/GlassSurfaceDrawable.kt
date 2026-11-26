package dev.veyra.xposed.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Lightweight frosted/tinted material; intentionally not presented as Apple's optical Liquid Glass. */
class GlassSurfaceDrawable(
    private val radiusPx: Float,
    private val isLight: Boolean,
    private val strength: Int,
    private val strokeColor: Int,
    private val topRounded: Boolean = true,
    private val bottomRounded: Boolean = true,
    private val drawSeparator: Boolean = false,
) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()

    override fun draw(canvas: Canvas) {
        rect.set(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
        if (rect.isEmpty) return
        val factor = strength.coerceIn(0, 100) / 100f
        val top = if (isLight) Color.argb((225 + 25 * factor).toInt().coerceAtMost(250), 255, 255, 255)
                  else Color.argb((150 + 54 * factor).toInt(), 40, 42, 48)
        val bottom = if (isLight) Color.argb((218 + 25 * factor).toInt().coerceAtMost(245), 246, 247, 250)
                     else Color.argb((164 + 42 * factor).toInt(), 27, 29, 35)
        val r = radiusPx.coerceAtLeast(0f)
        val tr = if (topRounded) r else 0f
        val br = if (bottomRounded) r else 0f
        val radii = floatArrayOf(tr, tr, tr, tr, br, br, br, br)
        path.reset()
        path.addRoundRect(rect, radii, Path.Direction.CW)

        fillPaint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(top, bottom), null, Shader.TileMode.CLAMP)
        canvas.drawPath(path, fillPaint)
        fillPaint.shader = null

        edgePaint.color = strokeColor
        canvas.drawPath(path, edgePaint)

        highlightPaint.shader = LinearGradient(rect.left, rect.top, rect.left, rect.top + rect.height() * 0.5f,
            intArrayOf(if (isLight) 0x66FFFFFF else 0x33FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawRect(rect.left + 1f, rect.top + 1f, rect.right - 1f,
            rect.top + rect.height() * 0.48f, highlightPaint)
        canvas.restore()
        highlightPaint.shader = null

        if (drawSeparator) {
            separatorPaint.color = if (isLight) 0x1C777780 else 0x2EFFFFFF
            separatorPaint.strokeWidth = 1f
            val inset = radiusPx * 1.15f
            canvas.drawLine(rect.left + inset, rect.bottom - 0.5f,
                rect.right - inset, rect.bottom - 0.5f, separatorPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        edgePaint.alpha = alpha
        highlightPaint.alpha = alpha
        separatorPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        edgePaint.colorFilter = colorFilter
        highlightPaint.colorFilter = colorFilter
        separatorPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
