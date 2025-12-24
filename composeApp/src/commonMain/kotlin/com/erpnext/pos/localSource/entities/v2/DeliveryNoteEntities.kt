package com.erpnext.pos.localSource.entities.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.erpnext.pos.localSource.dao.SyncStatus

@Entity(
    tableName = "delivery_notes",
    indices = [
        Index(value = ["instanceId", "companyId", "deliveryNoteId"])
    ]
)
data class DeliveryNoteEntity(
    var deliveryNoteId: String,
    var postingDate: String,
    var company: String,
    var customerId: String,
    var customerName: String,
    var territory: String?,
    var status: String,
    var setWarehouse: String?,
    var syncStatus: SyncStatus? = null,
    @ColumnInfo(name = "remote_modified")
    var remoteModified: String? = null,
    @ColumnInfo("remote_name")
    var remoteName: String? = null
) : BaseEntity()

@Entity(
    tableName = "delivery_note_items",
    indices = [
        Index(value = ["instanceId", "companyId", "deliveryNoteId"])
    ]
)
data class DeliveryNoteItemEntity(
    var deliveryNoteId: String,
    var rowId: Int,
    var itemCode: String,
    var itemName: String,
    var qty: Double,
    var uom: String,
    var rate: Double,
    var amount: Double,
    var warehouse: String?
) : BaseEntity()

@Entity(
    tableName = "delivery_note_links",
    indices = [
        Index(value = ["instanceId", "companyId", "deliveryNoteId"])
    ]
)
data class DeliveryNoteLinkEntity(
    var deliveryNoteId: String,
    var salesOrderId: String?,
    var salesInvoiceId: String?
) : BaseEntity()
