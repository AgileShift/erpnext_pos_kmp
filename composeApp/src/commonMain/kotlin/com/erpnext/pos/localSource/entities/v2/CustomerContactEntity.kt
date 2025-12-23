package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "customer_contact",
    primaryKeys = ["instanceId", "companyId", "contactId"]
)
data class CustomerContactEntity(
    var contactId: String,
    var customerId: String,
    var firstName: String,
    var lastName: String? = null,
    var fullName: String,
    var emailId: String? = null,
    var phone: String? = null,
    var mobileNo: String? = null,
    var isPrimary: Boolean = false,
    var status: String,
) : BaseEntity()
