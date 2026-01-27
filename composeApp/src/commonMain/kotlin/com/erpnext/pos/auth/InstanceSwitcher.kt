package com.erpnext.pos.auth

interface InstanceSwitcher {
    suspend fun switchInstance(siteUrl: String?)
}

class NoopInstanceSwitcher : InstanceSwitcher {
    override suspend fun switchInstance(siteUrl: String?) = Unit
}
