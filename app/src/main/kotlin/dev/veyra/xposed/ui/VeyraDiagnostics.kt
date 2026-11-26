package dev.veyra.xposed.ui

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import dev.veyra.xposed.target.SettingsPageClassifier

/** Privacy-safe diagnostics: class/resource structure only. Never reads visible text or preference values. */
object VeyraDiagnostics {
    private const val TAG = "VeyraTree"
    private val lastDump = HashMap<String, Int>()

    fun dumpIfChanged(activity: Activity, root: View, page: SettingsPageClassifier.Result) {
        val signature = buildString {
            append(activity.javaClass.name).append('|').append(page.page)
            append('|').append(root.javaClass.name)
        }
        val tree = StringBuilder()
        val visited = intArrayOf(0)
        appendTree(root, tree, 0, visited)
        val hash = tree.toString().hashCode()
        synchronized(lastDump) {
            if (lastDump[signature] == hash) return
            lastDump[signature] = hash
        }
        Log.i(TAG, "PAGE ${page.page} activity=${activity.javaClass.name} fragment=${page.fragmentClass}")
        tree.lineSequence().take(240).forEach { Log.d(TAG, it) }
    }

    private fun appendTree(view: View, out: StringBuilder, depth: Int, visited: IntArray) {
        if (depth > 12 || visited[0] >= 260) return
        visited[0]++
        val id = if (view.id == View.NO_ID) "no-id" else
            runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("unknown-id")
        out.append("  ".repeat(depth)).append(view.javaClass.name)
            .append(" id=").append(id)
            .append(" size=").append(view.width).append('x').append(view.height)
            .append(" clickable=").append(view.isClickable).append('\n')
        if (view is ViewGroup) {
            for (i in 0 until minOf(view.childCount, 40)) {
                if (visited[0] >= 260) break
                appendTree(view.getChildAt(i), out, depth + 1, visited)
            }
        }
    }
}
