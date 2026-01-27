package com.erpnext.pos.auth

import com.erpnext.pos.iosModule
import com.erpnext.pos.di.appModule
import com.erpnext.pos.di.v2.appModulev2
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin

class IosInstanceSwitcher : InstanceSwitcher {
    override suspend fun switchInstance(siteUrl: String?) {
        stopKoin()
        startKoin {
            modules(
                appModule,
                appModulev2,
                iosModule
            )
        }
    }
}
