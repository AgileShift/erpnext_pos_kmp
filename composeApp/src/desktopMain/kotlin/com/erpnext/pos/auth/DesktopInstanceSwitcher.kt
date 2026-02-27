package com.erpnext.pos.auth

import com.erpnext.pos.utils.AppLogger

class DesktopInstanceSwitcher : InstanceSwitcher {
    override suspend fun switchInstance(siteUrl: String?) {
        AppLogger.info("DesktopInstanceSwitcher.switchInstance skipped (site=$siteUrl)")
    }
}
