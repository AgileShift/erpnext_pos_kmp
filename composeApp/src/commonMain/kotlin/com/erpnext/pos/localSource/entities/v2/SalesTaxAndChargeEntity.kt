package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(tableName = "sales_tax_and_charges")
data class SalesTaxAndChargeEntity(
    var invoiceId: String,
    var chargeType: String,
    var accountHead: String,
    var rate: Float,
    var taxAmount: Float
) : BaseEntity()
