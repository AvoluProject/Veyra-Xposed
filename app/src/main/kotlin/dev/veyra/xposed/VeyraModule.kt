package dev.veyra.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import dev.veyra.xposed.hooks.SettingsHookInstaller

/** Single APK entry point, scoped only to the real Settings package. */
class VeyraModule : XposedModule() {
    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        super.onModuleLoaded(param)
        log(Log.INFO, TAG, "Veyra 1.0.0 loaded in ${param.processName}; API=${getApiVersion()}")
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        super.onPackageReady(param)
        if (param.packageName != TARGET_PACKAGE) return

        val prefs = runCatching { getRemotePreferences(VeyraConfig.GROUP) }
            .onFailure { log(Log.WARN, TAG, "Remote preferences unavailable; using safe defaults", it) }
            .getOrNull()

        SettingsHookInstaller.install(this, param.classLoader, prefs)
        log(Log.INFO, TAG, "Premium Settings hook pipeline registered")
    }

    companion object {
        const val TARGET_PACKAGE = "com.android.settings"
        private const val TAG = "VeyraModule"
    }
}
