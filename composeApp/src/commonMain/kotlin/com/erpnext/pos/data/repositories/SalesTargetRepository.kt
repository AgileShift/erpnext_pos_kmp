package com.erpnext.pos.data.repositories

import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.utils.NetworkMonitor
import kotlinx.coroutines.flow.first

class SalesTargetRepository(
    private val api: APIService,
    private val sessionRefresher: SessionRefresher,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun fetchMonthlyCompanyTarget(companyId: String): Double? {
        val online = networkMonitor.isConnected.first()
        if (!online) return null
        if (!sessionRefresher.ensureValidSession()) return null
        return api.getCompanyMonthlySalesTarget(companyId)
    }
}
