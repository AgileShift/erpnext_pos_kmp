package com.erpnext.pos.localSource.relations.v2

import androidx.room.Embedded
import androidx.room.Relation
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity

data class QuotationWithDetails(
    @Embedded
    val quotation: QuotationEntity,

    @Relation(
        parentColumn = "quotationId",
        entityColumn = "quotationId"
    )
    val items: List<QuotationItemEntity> = emptyList(),

    @Relation(
        parentColumn = "quotationId",
        entityColumn = "quotationId"
    )
    val taxes: List<QuotationTaxEntity> = emptyList(),

    @Relation(
        parentColumn = "quotationId",
        entityColumn = "quotationId"
    )
    val customerLinks: List<QuotationCustomerLinkEntity> = emptyList()
)
