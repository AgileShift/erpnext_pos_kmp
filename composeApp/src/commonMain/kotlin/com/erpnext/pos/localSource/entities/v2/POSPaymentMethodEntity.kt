package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "pos_payment_methods",
    primaryKeys = ["instanceId", "companyId", "posProfileId", "modeOfPayment"]
)
data class POSPaymentMethodEntity(
    var posProfileId: String,
    var modeOfPayment: String,
    var isDefault: Boolean = false
) : BaseEntity()
