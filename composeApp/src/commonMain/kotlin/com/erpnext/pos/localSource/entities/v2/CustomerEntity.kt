package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import com.erpnext.pos.localSource.dao.SyncStatus

@Entity(
    tableName = "customersv2",
    primaryKeys = ["instanceId", "companyId", "customerId"],
    indices = [
        Index(value = ["instanceId", "companyId", "territoryId"]),
        Index(value = ["instanceId", "companyId", "customerId"]),
        Index(value = ["customerId"])
    ]
)
data class CustomerEntity(
    var customerId: String,
    var customerName: String,
    var territoryId: String,
    var customerType: String,
    var disabled: Boolean = false,
    var customerGroup: String? = null,
    var territory: String,
    var creditLimit: Double? = null,
    var paymentTerms: String? = null,
    var defaultCurrency: String? = null,
    var defaultPriceList: String? = null,
    var primaryAddress: String? = null,
    var mobileNo: String? = null,
    var salesTeam: String? = null,
    var outstandingAmount: Double? = 0.0,
    var overdueAmount: Double? = 0.0,
    var unpaidInvoiceCount: Int? = 0,
    var lastPaidDate: String? = null,
    var syncStatus: SyncStatus? = null,
    @ColumnInfo(name = "remote_name")
    var remoteName: String? = null,
    @ColumnInfo(name = "remote_modified")
    var remoteModified: String? = null
) : BaseEntity()
