package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.PendingSync
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.DeliveryNoteDao
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteCreateDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteDetailDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteItemCreateDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteSnapshot
import com.erpnext.pos.remoteSource.mapper.v2.toEntity
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.remoteSource.sdk.v2.IncrementalSyncFilters
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DeliveryNoteRepository(
    private val deliveryNoteDao: DeliveryNoteDao,
    private val customerRepository: CustomerRepository,
    private val api: APIServiceV2
) {
    private companion object {
        val DETAIL_ELIGIBLE_STATUSES = setOf("To Bill")
    }

    suspend fun pull(ctx: SyncContext): Boolean {
        val notes = api.list<DeliveryNoteSnapshot>(
            doctype = ERPDocType.DeliveryNote,
            filters = IncrementalSyncFilters.deliveryNote(ctx)
        )
        if (notes.isEmpty()) return false
        val entities = notes.map { it.toEntity(ctx.instanceId, ctx.companyId) }
        deliveryNoteDao.upsertDeliveryNotes(entities)

        val noteIds = notes
            .filter { it.status in DETAIL_ELIGIBLE_STATUSES }
            .map { it.deliveryNoteId }
        val existing = deliveryNoteDao.getDeliveryNoteIdsWithItems(
            ctx.instanceId,
            ctx.companyId,
            noteIds
        ).toSet()
        val missing = noteIds.filterNot { it in existing }

        val details = if (missing.isEmpty()) {
            emptyList()
        } else {
            api.getDocsInBatches<DeliveryNoteDetailDto>(ERPDocType.DeliveryNote, missing)
        }

        details.forEach { detail ->
            val noteId = detail.deliveryNoteId
            if (detail.items.isNotEmpty()) {
                deliveryNoteDao.upsertItems(
                    detail.items.map { item ->
                        item.toEntity(noteId, ctx.instanceId, ctx.companyId)
                    }
                )
                val links = detail.items.mapNotNull { item ->
                    if (item.salesOrderId == null && item.salesInvoiceId == null) return@mapNotNull null
                    DeliveryNoteLinkEntity(
                        deliveryNoteId = noteId,
                        salesOrderId = item.salesOrderId,
                        salesInvoiceId = item.salesInvoiceId
                    ).apply {
                        instanceId = ctx.instanceId
                        companyId = ctx.companyId
                    }
                }.distinctBy { "${it.deliveryNoteId}|${it.salesOrderId}|${it.salesInvoiceId}" }
                if (links.isNotEmpty()) {
                    deliveryNoteDao.upsertLinks(links)
                }
            }
        }
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<PendingSync<DeliveryNoteCreateDto>> {
        return buildPendingCreatePayloads(ctx.instanceId, ctx.companyId)
    }

    suspend fun insertDeliveryNoteWithDetails(
        note: DeliveryNoteEntity,
        items: List<DeliveryNoteItemEntity>,
        links: List<DeliveryNoteLinkEntity>
    ) {
        deliveryNoteDao.insertDeliveryNoteWithDetails(note, items, links)
    }

    suspend fun getPendingDeliveryNotesWithDetails(
        instanceId: String,
        companyId: String
    ) = deliveryNoteDao.getPendingDeliveryNotesWithDetails(instanceId, companyId)

    @OptIn(ExperimentalTime::class)
    suspend fun markSynced(
        instanceId: String,
        companyId: String,
        deliveryNoteId: String,
        remoteName: String?,
        remoteModified: String?
    ) {
        val now = Clock.System.now().epochSeconds
        deliveryNoteDao.updateSyncStatus(
            instanceId,
            companyId,
            deliveryNoteId,
            syncStatus = "SYNCED",
            lastSyncedAt = now,
            updatedAt = now
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markFailed(
        instanceId: String,
        companyId: String,
        deliveryNoteId: String
    ) {
        val now = Clock.System.now().epochSeconds
        deliveryNoteDao.updateSyncStatus(
            instanceId,
            companyId,
            deliveryNoteId,
            syncStatus = "FAILED",
            lastSyncedAt = null,
            updatedAt = now
        )
    }

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<PendingSync<DeliveryNoteCreateDto>> {
        return deliveryNoteDao.getPendingDeliveryNotesWithDetails(instanceId, companyId).map { snapshot ->
            val link = snapshot.links.firstOrNull()
            val customerId = customerRepository.resolveRemoteCustomerId(
                instanceId,
                companyId,
                snapshot.deliveryNote.customerId
            )
            PendingSync(
                localId = snapshot.deliveryNote.deliveryNoteId,
                payload = DeliveryNoteCreateDto(
                    company = snapshot.deliveryNote.company,
                    postingDate = snapshot.deliveryNote.postingDate,
                    customerId = customerId,
                    customerName = snapshot.deliveryNote.customerName,
                    territory = snapshot.deliveryNote.territory,
                    setWarehouse = snapshot.deliveryNote.setWarehouse,
                    items = snapshot.items.map { item ->
                        DeliveryNoteItemCreateDto(
                            itemCode = item.itemCode,
                            qty = item.qty,
                            rate = item.rate,
                            uom = item.uom,
                            warehouse = item.warehouse,
                            salesOrderId = link?.salesOrderId,
                            salesInvoiceId = link?.salesInvoiceId
                        )
                    }
                )
            )
        }
    }
}
