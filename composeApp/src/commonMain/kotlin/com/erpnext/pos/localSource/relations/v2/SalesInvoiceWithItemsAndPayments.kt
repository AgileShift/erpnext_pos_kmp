package com.erpnext.pos.localSource.relations.v2

import androidx.room.Embedded
import androidx.room.Relation
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity

data class SalesInvoiceWithItemsAndPayments(

    @Embedded
    val invoice: SalesInvoiceEntity,

    @Relation(
        parentColumn = "invoiceId",
        entityColumn = "invoiceId"
    )
    val items: List<SalesInvoiceItemEntity> = emptyList(),

    @Relation(
        parentColumn = "invoiceId",
        entityColumn = "invoiceId"
    )
    val payments: List<SalesInvoicePaymentEntity> = emptyList()
)
