package com.erpnext.pos.domain.usecases.v2.sync

import com.erpnext.pos.data.repositories.v2.CatalogSyncRepository
import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.domain.sync.SyncDocType
import com.erpnext.pos.domain.sync.SyncUnitResult
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.utils.toErpDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SyncItemPricesUseCase(
    private val syncRepository: SyncRepository,
    private val catalogSyncRepository: CatalogSyncRepository
) : UseCase<SyncContext, SyncUnitResult>() {

    override suspend fun useCaseFunction(input: SyncContext): SyncUnitResult {
        val docType = SyncDocType.ITEM_PRICE.value
        syncRepository.setInProgress(input.instanceId, input.companyId, docType, true)

        return try {
            val modifiedSince = syncRepository
                .getLastPullAt(input.instanceId, input.companyId, docType)
                ?.let { (it * 1000).toErpDateTime() }
            val count = catalogSyncRepository.syncItemPrices(input, modifiedSince)
            syncRepository.markPullSuccess(input.instanceId, input.companyId, docType)
            syncRepository.refreshCounters(
                input.instanceId,
                input.companyId,
                docType,
                Clock.System.now().epochSeconds
            )
            SyncUnitResult(success = true, changed = count > 0)
        } catch (e: Throwable) {
            syncRepository.markFailure(input.instanceId, input.companyId, docType, e)
            SyncUnitResult(success = false, changed = false, error = e)
        } finally {
            syncRepository.setInProgress(input.instanceId, input.companyId, docType, false)
        }
    }
}
