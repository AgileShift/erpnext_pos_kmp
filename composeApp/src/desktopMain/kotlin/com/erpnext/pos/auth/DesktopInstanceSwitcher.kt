package com.erpnext.pos.auth

import com.erpnext.pos.DesktopRuntimeRestart
import com.erpnext.pos.utils.AppLogger

class DesktopInstanceSwitcher : InstanceSwitcher {
    override suspend fun switchInstance(siteUrl: String?) {
        AppLogger.info("DesktopInstanceSwitcher.switchInstance -> request runtime restart (site=$siteUrl)")
        DesktopRuntimeRestart.requestRestart()
        AppLogger.info("DesktopInstanceSwitcher.switchInstance -> runtime restart requested")
    }
}
