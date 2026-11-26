package dev.veyra.xposed

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

/** Dashboard-side write bridge. Hooked target processes read RemotePreferences read-only. */
object VeyraServiceManager {
    private const val TAG = "VeyraService"
    @Volatile private var service: XposedService? = null
    @Volatile private var registered = false
    private val main = Handler(Looper.getMainLooper())
    private val observers = CopyOnWriteArraySet<() -> Unit>()

    @Synchronized
    fun register(listener: XposedServiceHelper.OnServiceListener) {
        if (registered) return
        registered = true
        try {
            XposedServiceHelper.registerListener(listener)
        } catch (t: Throwable) {
            registered = false
            Log.e(TAG, "Unable to register libxposed service listener", t)
        }
    }

    fun onBound(bound: XposedService) {
        service = bound
        Log.i(TAG, "Connected to ${bound.getFrameworkName()} ${bound.getFrameworkVersion()}")
        notifyObservers()
    }

    fun onDied(dead: XposedService) {
        if (service === dead) service = null
        Log.w(TAG, "Framework service disconnected")
        notifyObservers()
    }

    fun addObserver(observer: () -> Unit) { observers.add(observer) }
    fun removeObserver(observer: () -> Unit) { observers.remove(observer) }
    fun isConnected(): Boolean = service != null

    fun frameworkLabel(): String {
        val current = service ?: return "Not connected"
        return "${current.getFrameworkName()} ${current.getFrameworkVersion()}"
    }

    fun readBoolean(key: String, fallback: Boolean): Boolean = runCatching {
        service?.getRemotePreferences(VeyraConfig.GROUP)?.getBoolean(key, fallback) ?: fallback
    }.getOrElse { fallback }

    fun readInt(key: String, fallback: Int): Int = runCatching {
        service?.getRemotePreferences(VeyraConfig.GROUP)?.getInt(key, fallback) ?: fallback
    }.getOrElse { fallback }

    fun writeBoolean(key: String, value: Boolean): Boolean = edit { it.putBoolean(key, value) }
    fun writeInt(key: String, value: Int): Boolean = edit { it.putInt(key, value) }

    fun writeDefaults(): Boolean = edit { editor ->
        VeyraConfig.defaults.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
            }
        }
    }

    private fun edit(block: (android.content.SharedPreferences.Editor) -> Unit): Boolean {
        val current = service ?: return false
        return try {
            val editor = current.getRemotePreferences(VeyraConfig.GROUP).edit()
            block(editor)
            val ok = editor.commit()
            if (ok) notifyObservers()
            ok
        } catch (t: Throwable) {
            Log.e(TAG, "Remote preference write failed", t)
            false
        }
    }

    private fun notifyObservers() {
        main.post { observers.forEach { observer -> runCatching(observer) } }
    }
}
