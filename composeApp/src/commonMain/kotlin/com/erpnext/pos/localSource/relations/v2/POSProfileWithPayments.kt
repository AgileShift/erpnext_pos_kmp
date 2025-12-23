package com.erpnext.pos.localSource.relations.v2

import androidx.room.Embedded
import androidx.room.Relation
import com.erpnext.pos.localSource.entities.v2.POSPaymentMethodEntity
import com.erpnext.pos.localSource.entities.v2.POSProfileEntity

data class POSProfileWithPayments(
    @Embedded val profile: POSProfileEntity,
    @Relation(
        parentColumn = "posProfileId",
        entityColumn = "posProfileId"
    )
    val paymentMethods: List<POSPaymentMethodEntity>
)
