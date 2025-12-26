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
class SyncItemsUseCase(
    private val syncRepository: SyncRepository,
    private val catalogSyncRepository: CatalogSyncRepository
) : UseCase<SyncContext, SyncUnitResult>() {

    override suspend fun useCaseFunction(input: SyncContext): SyncUnitResult {
        val itemDocType = SyncDocType.ITEM.value
        val groupDocType = SyncDocType.ITEM_GROUP.value
        syncRepository.setInProgress(input.instanceId, input.companyId, groupDocType, true)
        syncRepository.setInProgress(input.instanceId, input.companyId, itemDocType, true)

        return try {
            val groupsChanged = syncItemGroups(input, groupDocType)
            val itemsChanged = syncItems(input, itemDocType)
            SyncUnitResult(success = true, changed = groupsChanged || itemsChanged)
        } catch (e: Throwable) {
            SyncUnitResult(success = false, changed = false, error = e)
        } finally {
            syncRepository.setInProgress(input.instanceId, input.companyId, groupDocType, false)
            syncRepository.setInProgress(input.instanceId, input.companyId, itemDocType, false)
        }
    }

    private suspend fun syncItemGroups(ctx: SyncContext, docType: String): Boolean {
        return try {
            val modifiedSince = syncRepository
                .getLastPullAt(ctx.instanceId, ctx.companyId, docType)
                ?.let { (it * 1000).toErpDateTime() }
            val count = catalogSyncRepository.syncItemGroups(ctx, modifiedSince)
            syncRepository.markPullSuccess(ctx.instanceId, ctx.companyId, docType)
            syncRepository.refreshCounters(
                ctx.instanceId,
                ctx.companyId,
                docType,
                Clock.System.now().epochSeconds
            )
            count > 0
        } catch (e: Throwable) {
            syncRepository.markFailure(ctx.instanceId, ctx.companyId, docType, e)
            throw e
        }
    }

    private suspend fun syncItems(ctx: SyncContext, docType: String): Boolean {
        return try {
            val modifiedSince = syncRepository
                .getLastPullAt(ctx.instanceId, ctx.companyId, docType)
                ?.let { (it * 1000).toErpDateTime() }
            val count = catalogSyncRepository.syncItems(ctx, modifiedSince)
            syncRepository.markPullSuccess(ctx.instanceId, ctx.companyId, docType)
            syncRepository.refreshCounters(
                ctx.instanceId,
                ctx.companyId,
                docType,
                Clock.System.now().epochSeconds
            )
            count > 0
        } catch (e: Throwable) {
            syncRepository.markFailure(ctx.instanceId, ctx.companyId, docType, e)
            throw e
        }
    }
}
