package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.SalesInvoiceRemoteRepository
import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.policy.BackoffPolicy
import com.erpnext.pos.domain.policy.CatalogInvalidationPolicy
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class SyncPendingInvoicesInput(
    val instanceId: String,
    val companyId: String,
    val syncedDoctypes: Set<String>
)

@OptIn(ExperimentalTime::class)
class SyncPendingInvoicesUseCase(
    private val invoiceDao: SalesInvoiceDao,
    private val syncRepository: SyncRepository,
    private val remoteRepository: SalesInvoiceRemoteRepository,
    private val backoff: BackoffPolicy,
    private val catalogInvalidationPolicy: CatalogInvalidationPolicy
) : UseCase<SyncPendingInvoicesInput, Unit>() {

    override suspend fun useCaseFunction(input: SyncPendingInvoicesInput) {
        syncRepository.setInProgress(input.instanceId, input.companyId, true)

        try {
            val pending =
                invoiceDao.getPendingInvoicesWithDetails(input.instanceId, input.companyId)

            pending.forEachIndexed { attempt, inv ->
                val res = remoteRepository.submitInvoice(inv)

                if (res.isSuccess) {
                    invoiceDao.updateSyncStatus(
                        input.instanceId, input.companyId, inv.invoice.invoiceId,
                        syncStatus = "SYNCED",
                        lastSyncedAt = Clock.System.now().epochSeconds,
                        updatedAt = Clock.System.now().epochSeconds
                    )
                } else {
                    //TODO: Validar si es correcto el delay aca
                    val delayMs = backoff.nextDelayMs(attempt, Random.nextDouble())
                    delay(delayMs)

                    invoiceDao.updateSyncStatus(
                        input.instanceId, input.companyId, inv.invoice.invoiceId,
                        syncStatus = "FAILED",
                        lastSyncedAt = null,
                        updatedAt = Clock.System.now().epochSeconds
                    )

                    // Invalidacion explicita
                    if (input.syncedDoctypes.any {
                            it in setOf(
                                "Item",
                                "Item Price",
                                "Bin",
                                "Item Group"
                            )
                        }) {
                        catalogInvalidationPolicy.invalidate()
                    }

                    syncRepository.refreshCounters(
                        input.instanceId,
                        input.companyId,
                        lastFullSyncAt = Clock.System.now().epochSeconds
                    )
                }
            }

            syncRepository.refreshCounters(
                input.instanceId, input.companyId, lastFullSyncAt = Clock.System.now().epochSeconds
            )
        } finally {
            syncRepository.setInProgress(input.instanceId, input.companyId, false)
            syncRepository.refreshCounters(
                input.instanceId,
                input.companyId
            ) // asegura contadores coherentes
        }
    }
}

