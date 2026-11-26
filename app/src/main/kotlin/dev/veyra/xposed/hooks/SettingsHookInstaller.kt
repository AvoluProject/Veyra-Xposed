package dev.veyra.xposed.hooks

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import dev.veyra.xposed.VeyraConfig
import dev.veyra.xposed.ui.SettingsUiEngine
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap

/** Hooks lifecycle and AndroidX preference binding, never preference click listeners/controllers. */
object SettingsHookInstaller {
    private const val TAG = "VeyraHooks"
    private val activeActivities = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())
    @Volatile private var preferences: SharedPreferences? = null
    @Volatile private var installedLoader: ClassLoader? = null
    @Volatile private var configProvider: () -> VeyraConfig = { VeyraConfig() }
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    @Synchronized
    fun install(module: XposedModule, loader: ClassLoader, remote: SharedPreferences?) {
        if (installedLoader === loader) return
        installedLoader = loader
        preferences = remote
        configProvider = { VeyraConfig.from(preferences) }
        registerPreferenceListener(remote)
        hookActivityLifecycle(module)
        hookFragmentResume(module, loader)
        hookPreferenceAdapter(module, loader)
        module.log(Log.INFO, TAG, "Installed lifecycle + preference binding hooks")
    }

    private fun registerPreferenceListener(remote: SharedPreferences?) {
        if (remote == null) return
        val callback = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshActivities() }
        listener = callback
        runCatching { remote.registerOnSharedPreferenceChangeListener(callback) }
            .onFailure { Log.w(TAG, "Remote preference live listener unavailable; changes apply on next resume", it) }
    }

    private fun hookActivityLifecycle(module: XposedModule) {
        runCatching {
            val postResume = Activity::class.java.getDeclaredMethod("onPostResume")
            module.hook(postResume)
                .setId("veyra.premium.activity.post_resume")
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    val activity = chain.thisObject as? Activity
                    if (activity != null && activity.packageName == "com.android.settings") {
                        remember(activity)
                        activity.window?.decorView?.post {
                            runCatching { SettingsUiEngine.apply(activity, configProvider(), null) }
                                .onFailure { Log.e(TAG, "Activity styling failed", it) }
                        }
                    }
                    result
                }
            module.log(Log.INFO, TAG, "Hooked Activity.onPostResume")
        }.onFailure { module.log(Log.WARN, TAG, "Activity lifecycle hook unavailable", it) }

        runCatching {
            val onDestroy = Activity::class.java.getDeclaredMethod("onDestroy")
            module.hook(onDestroy)
                .setId("veyra.premium.activity.destroy")
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val activity = chain.thisObject as? Activity
                    val result = chain.proceed()
                    if (activity != null) synchronized(activeActivities) { activeActivities.remove(activity) }
                    result
                }
            module.log(Log.INFO, TAG, "Hooked Activity.onDestroy")
        }.onFailure { module.log(Log.WARN, TAG, "Activity cleanup hook unavailable", it) }
    }

    private fun hookFragmentResume(module: XposedModule, loader: ClassLoader) {
        val fragmentClass = runCatching { Class.forName("androidx.fragment.app.Fragment", false, loader) }.getOrNull()
            ?: run {
                module.log(Log.WARN, TAG, "AndroidX Fragment class unavailable")
                return
            }

        // performResume is the lifecycle dispatch point; it still runs when a concrete Fragment
        // overrides onResume(), unlike hooking only Fragment.onResume().
        val performResume = runCatching { fragmentClass.getDeclaredMethod("performResume") }.getOrNull()
            ?: runCatching { fragmentClass.getDeclaredMethod("onResume") }.getOrNull()
            ?: return

        runCatching {
            module.hook(performResume)
                .setId("veyra.premium.fragment.resume")
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    val fragment = chain.thisObject ?: return@intercept result
                    val activity = invokeNoArg(fragment, "getActivity") as? Activity
                    val root = invokeNoArg(fragment, "getView") as? View
                    if (activity?.packageName == "com.android.settings" && root != null) {
                        remember(activity)
                        val fragmentName = fragment.javaClass.name
                        root.post {
                            runCatching { SettingsUiEngine.apply(activity, configProvider(), fragmentName) }
                                .onFailure { Log.e(TAG, "Fragment styling failed for $fragmentName", it) }
                        }
                    }
                    result
                }
            module.log(Log.INFO, TAG, "Hooked ${fragmentClass.name}.${performResume.name}")
        }.onFailure { module.log(Log.WARN, TAG, "Fragment lifecycle hook unavailable", it) }
    }

    private fun hookPreferenceAdapter(module: XposedModule, loader: ClassLoader) {
        val adapterClass = runCatching {
            Class.forName("androidx.preference.PreferenceGroupAdapter", false, loader)
        }.getOrNull() ?: run {
            module.log(Log.WARN, TAG, "AndroidX PreferenceGroupAdapter unavailable")
            return
        }

        val bindMethod = adapterClass.declaredMethods.firstOrNull { method ->
            method.name == "onBindViewHolder" && method.parameterTypes.size == 2 &&
                method.parameterTypes[0].name.endsWith("PreferenceViewHolder") &&
                method.parameterTypes[1] == Int::class.javaPrimitiveType
        } ?: run {
            module.log(Log.WARN, TAG, "PreferenceGroupAdapter bind method not found")
            return
        }

        runCatching {
            module.hook(bindMethod)
                .setId("veyra.premium.preference.bind")
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed()
                    val holder = chain.getArg(0)
                    val position = chain.getArg(1) as? Int ?: -1
                    val itemView = invokeNoArg(holder, "getItemView") as? View
                    if (itemView != null) {
                        val adapter = chain.thisObject
                        val preference = getAdapterItem(adapter, position)
                        val key = preference?.let { invokeNoArg(it, "getKey") as? String }
                        val prefClass = preference?.javaClass?.name.orEmpty()
                        val contextActivity = findActivity(itemView.context)
                        if (contextActivity?.packageName == "com.android.settings") {
                            val pageRoot = contextActivity.findViewById<View>(android.R.id.content)
                            val page = SettingsUiEngine.currentPage(contextActivity, pageRoot)
                            val parent = preference?.let { invokeNoArg(it, "getParent") }
                            val previous = if (position > 0) getAdapterItem(adapter, position - 1) else null
                            val next = getAdapterItem(adapter, position + 1)
                            val firstInGroup = parent == null || previous?.let { invokeNoArg(it, "getParent") !== parent } != false
                            val lastInGroup = parent == null || next?.let { invokeNoArg(it, "getParent") !== parent } != false
                            itemView.post {
                                runCatching {
                                    SettingsUiEngine.applyPreferenceItem(
                                        itemView, contextActivity, configProvider(), page, key, prefClass,
                                        firstInGroup, lastInGroup
                                    )
                                }.onFailure { Log.e(TAG, "Preference row styling failed", it) }
                            }
                        }
                    }
                    result
                }
            module.log(Log.INFO, TAG, "Hooked ${adapterClass.name}.${bindMethod.name}")
        }.onFailure { module.log(Log.WARN, TAG, "Preference adapter hook unavailable", it) }
    }

    private fun getAdapterItem(adapter: Any?, position: Int): Any? {
        if (adapter == null || position < 0) return null
        val method = generateSequence(adapter.javaClass as Class<*>?) { it.superclass }
            .flatMap { it.declaredMethods.asSequence() }
            .firstOrNull {
                it.name == "getItem" && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == Int::class.javaPrimitiveType
            } ?: return null
        return runCatching { method.isAccessible = true; method.invoke(adapter, position) }.getOrNull()
    }

    private fun invokeNoArg(target: Any?, name: String): Any? {
        if (target == null) return null
        val method = generateSequence(target.javaClass as Class<*>?) { it.superclass }
            .flatMap { it.declaredMethods.asSequence() }
            .firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
            ?: return null
        return runCatching { method.isAccessible = true; method.invoke(target) }.getOrNull()
    }

    private fun findActivity(context: Context?): Activity? {
        var current = context
        repeat(12) {
            when (current) {
                is Activity -> return current
                is ContextWrapper -> current = current.baseContext
                else -> return null
            }
        }
        return null
    }

    private fun remember(activity: Activity) {
        synchronized(activeActivities) { activeActivities.add(activity) }
    }

    private fun refreshActivities() {
        val snapshot = synchronized(activeActivities) { activeActivities.toList() }
        snapshot.forEach { activity ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.runOnUiThread {
                    runCatching { SettingsUiEngine.apply(activity, configProvider(), null) }
                        .onFailure { Log.e(TAG, "Live style refresh failed", it) }
                }
            }
        }
    }
}
