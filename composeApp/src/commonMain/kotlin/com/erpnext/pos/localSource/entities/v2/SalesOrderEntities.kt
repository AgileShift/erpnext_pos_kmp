package com.erpnext.pos.localSource.entities.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.erpnext.pos.localSource.dao.SyncStatus

@Entity(
    tableName = "sales_orders",
    indices = [
        Index(value = ["instanceId", "companyId", "salesOrderId"])
    ]
)
data class SalesOrderEntity(
    var salesOrderId: String,
    var transactionDate: String,
    var deliveryDate: String?,
    var company: String,
    var customerId: String,
    var customerName: String,
    var territory: String?,
    var status: String,
    var deliveryStatus: String?,
    var billingStatus: String?,
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
    tableName = "sales_order_items",
    indices = [
        Index(value = ["instanceId", "companyId", "salesOrderId"])
    ]
)
data class SalesOrderItemEntity(
    var salesOrderId: String,
    var rowId: Int,
    var itemCode: String,
    var itemName: String,
    var qty: Double,
    var uom: String,
    var rate: Double,
    var amount: Double,
    var warehouse: String?
) : BaseEntity()
