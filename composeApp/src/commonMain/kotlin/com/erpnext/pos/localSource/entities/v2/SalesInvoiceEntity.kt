package com.erpnext.pos.localSource.entities.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.erpnext.pos.localSource.dao.SyncStatus

@Entity(
    tableName = "sales_invoices",
    primaryKeys = ["instanceId", "companyId", "invoiceId"],
    indices = [
        Index(value = ["instanceId", "companyId", "territoryId", "postingDate"]),
        Index(value = ["instanceId", "companyId", "customerId", "status"]),
        Index(value = ["instanceId", "companyId", "syncStatus"]),
    ]
)
data class SalesInvoiceEntity(
    var invoiceId: String,
    var territoryId: String,
    var namingSeries: String,
    var docStatus: String,
    var status: String,
    var postingDate: String,
    var postingTime: String,
    var customerId: String,
    var customerName: String,
    var company: String,
    var territory: String,
    var salesPerson: String,
    var currency: String,
    var conversionRate: Float,
    var total: Double,
    var totalTaxesAndCharges: Float,
    var grandTotal: Double,
    var roundedTotal: Float? = null,
    var outstandingAmount: Double,
    var isPos: Boolean = true,
    var dueDate: String,
    var paymentTerms: String? = null, //TODO: Si no existe, poner un valor en configuracion
    var updateStock: Boolean = true, //TODO: Mandar a configuracion
    var setWarehouse: String? = null, //TODO: Mandar a configuracion
    var priceList: String? = null,
    var disableRoundedTotal: Boolean = false, //TODO: Mandar a configuracion
    var syncStatus: SyncStatus? = null,
    @ColumnInfo(name = "remote_modified")
    var remoteModified: String? = null,
    @ColumnInfo("remote_name")
    var remoteName: String? = null
) : BaseEntity()