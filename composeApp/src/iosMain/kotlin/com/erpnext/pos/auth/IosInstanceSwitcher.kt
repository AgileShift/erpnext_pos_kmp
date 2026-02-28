package com.erpnext.pos.auth

import com.erpnext.pos.di.appModule
import com.erpnext.pos.iosModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class IosInstanceSwitcher : InstanceSwitcher {
  override suspend fun switchInstance(siteUrl: String?) {
    stopKoin()
    startKoin { modules(appModule, iosModule) }
  }
}
