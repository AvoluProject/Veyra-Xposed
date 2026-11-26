package dev.veyra.xposed.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import dev.veyra.xposed.R
import dev.veyra.xposed.target.SettingsPageClassifier

object VeyraMotionEngine {
    fun animatePage(activity: Activity, root: ViewGroup, page: String, strength: Int) {
        val child = if (root.childCount > 0) root.getChildAt(0) else root
        val token = "${page}:${System.identityHashCode(child)}"
        if (root.getTag(R.id.veyra_motion_token) == token) return
        root.setTag(R.id.veyra_motion_token, token)

        val scale = strength.coerceIn(50, 150) / 100f
        val density = activity.resources.displayMetrics.density
        root.alpha = 0.94f
        root.translationY = 8f * density * scale
        root.animate().alpha(1f).setDuration((150L * scale).toLong().coerceIn(100L, 260L))
            .setInterpolator(DecelerateInterpolator()).start()
        SpringAnimation(root, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f)
                .setDampingRatio(0.88f)
                .setStiffness((520f / scale.coerceAtLeast(0.5f)).coerceIn(280f, 900f))
            setStartValue(8f * density * scale)
            start()
        }
    }
}
