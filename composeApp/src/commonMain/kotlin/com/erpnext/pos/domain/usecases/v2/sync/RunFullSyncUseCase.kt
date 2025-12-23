package com.erpnext.pos.domain.usecases.v2.sync

import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.domain.usecases.v2.SyncPendingInvoicesUseCase

data class SyncResult(
    val syncedDoctypes: Set<String>,
    val failedDoctypes: Set<String>
)

data class SyncInput(
    val instanceId: String,
    val companyId: String
)
class RunSyncUseCase(
    private val syncItems: SyncItemsUseCase,
    private val syncPrices: SyncItemPricesUseCase,
    private val syncBins: SyncBinsUseCase,
    private val syncCustomers: SyncCustomersUseCase,
    private val syncInvoices: SyncPendingInvoicesUseCase
): UseCase<SyncInput, SyncResult>() {

    override suspend fun useCaseFunction(input: SyncInput): SyncResult {
        val instanceId = input.instanceId
        val companyId = input.companyId

        val synced = mutableSetOf<String>()
        val failed = mutableSetOf<String>()

        suspend fun run(name: String, block: suspend () -> Unit) {
            try {
                block()
                synced += name
            } catch (e: Throwable) {
                failed += name
            }
        }

        /*run("Item") { syncItems(instanceId, companyId) }
        run("Item Price") { syncPrices(instanceId, companyId) }
        run("Bin") { syncBins(instanceId, companyId) }
        run("Customer") { syncCustomers(instanceId, companyId) }
        run("Sales Invoice") { syncInvoices(instanceId, companyId) }*/

        return SyncResult(
            syncedDoctypes = synced,
            failedDoctypes = failed
        )
    }
}
