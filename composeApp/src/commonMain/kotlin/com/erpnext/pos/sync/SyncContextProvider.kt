package com.erpnext.pos.sync

import com.erpnext.pos.domain.policy.DatePolicy
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.views.CashBoxManager

class SyncContextProvider(
    private val contextManager: CashBoxManager,
    private val datePolicy: DatePolicy
) {
    suspend fun buildContext(): SyncContext? {
        val ctx = contextManager.getContext() ?: contextManager.resolveContextForSync() ?: return null
        val instanceId = ctx.company.ifBlank { ctx.profileName }.ifBlank { ctx.username }
        val companyId = ctx.company.ifBlank { ctx.profileName }
        if (instanceId.isBlank() || companyId.isBlank()) return null
        return SyncContext(
            instanceId = instanceId,
            companyId = companyId,
            territoryId = ctx.territory ?: ctx.route ?: "",
            warehouseId = ctx.warehouse ?: "",
            priceList = ctx.priceList ?: ctx.currency,
            fromDate = datePolicy.invoicesFromDate()
        )
    }
}
