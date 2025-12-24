package com.erpnext.pos.localSource.entities.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.erpnext.pos.localSource.dao.SyncStatus

@Entity(
    tableName = "quotations",
    indices = [
        Index(value = ["instanceId", "companyId", "quotationId"])
    ]
)
data class QuotationEntity(
    var quotationId: String,
    var transactionDate: String,
    var validUntil: String?,
    var company: String,
    var partyName: String,
    var customerName: String,
    var territory: String?,
    var status: String,
    var priceListCurrency: String,
    var sellingPriceList: String?,
    var netTotal: Double,
    var grandTotal: Double,
    var syncStatus: SyncStatus? = null,
    @ColumnInfo(name = "remote_modified")
    var remoteModified: String? = null,
    @ColumnInfo("remote_name")
    var remoteName: String? = null
) : BaseEntity()

@Entity(
    tableName = "quotation_items",
    indices = [
        Index(value = ["instanceId", "companyId", "quotationId"])
    ]
)
data class QuotationItemEntity(
    var quotationId: String,
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
    tableName = "quotation_taxes",
    indices = [
        Index(value = ["instanceId", "companyId", "quotationId"])
    ]
)
data class QuotationTaxEntity(
    var quotationId: String,
    var chargeType: String,
    var accountHead: String,
    var rate: Double,
    var taxAmount: Double
) : BaseEntity()

@Entity(
    tableName = "quotation_customer_links",
    indices = [
        Index(value = ["instanceId", "companyId", "quotationId"])
    ]
)
data class QuotationCustomerLinkEntity(
    var quotationId: String,
    var partyName: String,
    var customerName: String,
    var contactId: String?,
    var addressId: String?
) : BaseEntity()
