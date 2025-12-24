package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryNote(note: DeliveryNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<DeliveryNoteItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<DeliveryNoteLinkEntity>)

    @Transaction
    suspend fun insertDeliveryNoteWithDetails(
        note: DeliveryNoteEntity,
        items: List<DeliveryNoteItemEntity>,
        links: List<DeliveryNoteLinkEntity>
    ) {
        insertDeliveryNote(note)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
        if (links.isNotEmpty()) {
            insertLinks(links)
        }
    }

    @Transaction
    @Query(
        """
        SELECT * FROM delivery_notes
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
          AND syncStatus = 'PENDING'
    """
    )
    suspend fun getPendingDeliveryNotesWithDetails(
        instanceId: String,
        companyId: String
    ): List<DeliveryNoteWithItemsAndLinks>

    @Query(
        """
      UPDATE delivery_notes
      SET syncStatus = :syncStatus,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND deliveryNoteId = :deliveryNoteId
    """
    )
    suspend fun updateSyncStatus(
        instanceId: String,
        companyId: String,
        deliveryNoteId: String,
        syncStatus: String,
        lastSyncedAt: Long?,
        updatedAt: Long
    )

    @Query(
        """
      UPDATE delivery_notes
      SET customerId = :newCustomerId,
          customerName = :newCustomerName,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND customerId = :oldCustomerId
    """
    )
    suspend fun replaceCustomerReference(
        instanceId: String,
        companyId: String,
        oldCustomerId: String,
        newCustomerId: String,
        newCustomerName: String,
        updatedAt: Long
    )
}
