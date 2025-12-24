package com.erpnext.pos.localSource.relations.v2

import androidx.room.Embedded
import androidx.room.Relation
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity

data class PaymentEntryWithReferences(
    @Embedded
    val paymentEntry: PaymentEntryEntity,

    @Relation(
        parentColumn = "paymentEntryId",
        entityColumn = "paymentEntryId"
    )
    val references: List<PaymentEntryReferenceEntity> = emptyList()
)
