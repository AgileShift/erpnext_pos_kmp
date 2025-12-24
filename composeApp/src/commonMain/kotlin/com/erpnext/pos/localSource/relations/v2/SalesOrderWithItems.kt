package com.erpnext.pos.localSource.relations.v2

import androidx.room.Embedded
import androidx.room.Relation
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity

data class SalesOrderWithItems(
    @Embedded
    val salesOrder: SalesOrderEntity,

    @Relation(
        parentColumn = "salesOrderId",
        entityColumn = "salesOrderId"
    )
    val items: List<SalesOrderItemEntity> = emptyList()
)
