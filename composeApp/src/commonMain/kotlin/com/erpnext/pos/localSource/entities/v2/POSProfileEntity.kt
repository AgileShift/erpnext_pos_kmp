package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "pos_profiles",
    primaryKeys = ["instanceId", "companyId", "posProfileId"]
)
data class POSProfileEntity(
    var posProfileId: String,
    var warehouseId: String,
    var costCenterId: String,
    var currency: String,
    var priceList: String,
    var allowNegativeStock: Boolean,
    var updateStock: Boolean = true,
    var allowCreditSales: Boolean = false,
    var customerId: String? = null,
    var namingSeries: String? = null,
    var taxTemplateId: String? = null,
    var writeOffAccount: String? = null,
    var writeOffCostCenter: String? = null,
    var disabled: Boolean = false
) : BaseEntity()