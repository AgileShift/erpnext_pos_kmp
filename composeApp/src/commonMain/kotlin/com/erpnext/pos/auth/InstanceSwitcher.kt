package com.erpnext.pos.auth

interface InstanceSwitcher {
    suspend fun switchInstance(siteUrl: String?)
}
