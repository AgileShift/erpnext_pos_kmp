package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.relations.v2.DeliveryNoteWithItemsAndLinks

@Dao
interface DeliveryNoteDao {

    @Query(
        """
        SELECT * FROM delivery_notes
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
    """
    )
    suspend fun getDeliveryNotes(
        instanceId: String,
        companyId: String
    ): List<DeliveryNoteEntity>

    @Query(
        """
        SELECT * FROM delivery_notes
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND deliveryNoteId = :deliveryNoteId
          AND is_deleted = 0
        LIMIT 1
    """
    )
    suspend fun getDeliveryNote(
        instanceId: String,
        companyId: String,
        deliveryNoteId: String
    ): DeliveryNoteEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM delivery_notes
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND deliveryNoteId = :deliveryNoteId
          AND is_deleted = 0
        LIMIT 1
    """
    )
    suspend fun getDeliveryNoteWithDetails(
        instanceId: String,
        companyId: String,
        deliveryNoteId: String
    ): DeliveryNoteWithItemsAndLinks?
}
