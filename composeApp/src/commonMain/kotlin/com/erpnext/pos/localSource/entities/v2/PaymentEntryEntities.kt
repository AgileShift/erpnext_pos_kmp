package com.erpnext.pos.localSource.entities.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.erpnext.pos.localSource.dao.SyncStatus

@Entity(
    tableName = "payment_entries",
    indices = [
        Index(value = ["instanceId", "companyId", "paymentEntryId"])
    ]
)
data class PaymentEntryEntity(
    var paymentEntryId: String,
    var postingDate: String,
    var company: String,
    var territory: String?,
    var paymentType: String,
    var modeOfPayment: String,
    var partyType: String,
    var partyId: String,
    var paidAmount: Double,
    var receivedAmount: Double,
    var unallocatedAmount: Double? = null,
    var syncStatus: SyncStatus? = null,
    @ColumnInfo(name = "remote_modified")
    var remoteModified: String? = null,
    @ColumnInfo("remote_name")
    var remoteName: String? = null
) : BaseEntity()

@Entity(
    tableName = "payment_entry_references",
    indices = [
        Index(value = ["instanceId", "companyId", "paymentEntryId"])
    ]
)
data class PaymentEntryReferenceEntity(
    var paymentEntryId: String,
    var referenceDoctype: String,
    var referenceName: String,
    var totalAmount: Double,
    var outstandingAmount: Double,
    var allocatedAmount: Double
) : BaseEntity()
