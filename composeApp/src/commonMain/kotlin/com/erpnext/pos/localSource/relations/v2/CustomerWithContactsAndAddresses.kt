package com.erpnext.pos.localSource.relations.v2

import androidx.room.Embedded
import androidx.room.Relation
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.localSource.entities.v2.CustomerEntity

data class CustomerWithContactsAndAddresses(
    @Embedded val customer: CustomerEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "customerId"
    )
    val contacts: List<CustomerContactEntity>,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "customerId"
    )
    val addresses: List<CustomerAddressEntity>
)