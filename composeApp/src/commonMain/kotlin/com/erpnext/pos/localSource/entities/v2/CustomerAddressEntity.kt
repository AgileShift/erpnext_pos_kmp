package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "customer_address",
    primaryKeys = ["instanceId", "companyId", "addressId"]
)
data class CustomerAddressEntity(
    var addressId: String,
    var customerId: String,
    var addressTitle: String,
    var addressType: String,
    var line1 : String,
    var line2 : String? = null,
    var city: String,
    var county: String? = null,
    var state: String? = null,
    var country: String? = null,
    var pinCode: String? = null,
    var isPrimary: Boolean = false,
    var isShipping: Boolean = false,
    var disabled: Boolean = false
) : BaseEntity()