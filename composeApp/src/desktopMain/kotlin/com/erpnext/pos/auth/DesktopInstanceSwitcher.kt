package com.erpnext.pos.auth

import com.erpnext.pos.desktopModule
import com.erpnext.pos.di.appModule
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin

class DesktopInstanceSwitcher : InstanceSwitcher {
    override suspend fun switchInstance(siteUrl: String?) {
        stopKoin()
        startKoin {
            modules(
                appModule,
                desktopModule
            )
        }
    }
}
