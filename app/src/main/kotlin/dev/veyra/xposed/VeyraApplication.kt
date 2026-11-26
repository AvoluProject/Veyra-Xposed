package dev.veyra.xposed

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class VeyraApplication : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        VeyraServiceManager.register(this)
    }

    override fun onServiceBind(service: XposedService) = VeyraServiceManager.onBound(service)
    override fun onServiceDied(service: XposedService) = VeyraServiceManager.onDied(service)
}
