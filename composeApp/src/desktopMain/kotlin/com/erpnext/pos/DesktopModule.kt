package com.erpnext.pos

import com.erpnext.pos.utils.NetworkMonitor
import org.koin.dsl.module

val desktopModule = module {
    single { NetworkMonitor() }
}