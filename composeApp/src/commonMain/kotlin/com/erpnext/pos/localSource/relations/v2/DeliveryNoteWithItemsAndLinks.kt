package com.erpnext.pos.localSource.relations.v2

import androidx.room.Embedded
import androidx.room.Relation
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity

data class DeliveryNoteWithItemsAndLinks(
    @Embedded
    val deliveryNote: DeliveryNoteEntity,

    @Relation(
        parentColumn = "deliveryNoteId",
        entityColumn = "deliveryNoteId"
    )
    val items: List<DeliveryNoteItemEntity> = emptyList(),

    @Relation(
        parentColumn = "deliveryNoteId",
        entityColumn = "deliveryNoteId"
    )
    val links: List<DeliveryNoteLinkEntity> = emptyList()
)
