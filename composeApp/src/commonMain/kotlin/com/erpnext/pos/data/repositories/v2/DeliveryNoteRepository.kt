package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.DeliveryNoteDao
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteCreateDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteItemCreateDto

class DeliveryNoteRepository(
    private val deliveryNoteDao: DeliveryNoteDao
) {

    suspend fun pull(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<DeliveryNoteCreateDto> {
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

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<DeliveryNoteCreateDto> {
        return deliveryNoteDao.getPendingDeliveryNotesWithDetails(instanceId, companyId).map { snapshot ->
            val link = snapshot.links.firstOrNull()
            DeliveryNoteCreateDto(
                company = snapshot.note.company,
                postingDate = snapshot.note.postingDate,
                customerId = snapshot.note.customerId,
                customerName = snapshot.note.customerName,
                territory = snapshot.note.territory,
                setWarehouse = snapshot.note.setWarehouse,
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
        }
    }
}
