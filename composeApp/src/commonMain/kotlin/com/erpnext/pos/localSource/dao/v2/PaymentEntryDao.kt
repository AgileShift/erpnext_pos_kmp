package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity
import com.erpnext.pos.localSource.relations.v2.PaymentEntryWithReferences

@Dao
interface PaymentEntryDao {

    @Query(
        """
        SELECT * FROM payment_entries
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
    """
    )
    suspend fun getPaymentEntries(
        instanceId: String,
        companyId: String
    ): List<PaymentEntryEntity>

    @Query(
        """
        SELECT * FROM payment_entries
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND paymentEntryId = :paymentEntryId
          AND is_deleted = 0
        LIMIT 1
    """
    )
    suspend fun getPaymentEntry(
        instanceId: String,
        companyId: String,
        paymentEntryId: String
    ): PaymentEntryEntity?

    @Transaction
    suspend fun getPaymentEntryWithReferences(
        instanceId: String,
        companyId: String,
        paymentEntryId: String
    ): PaymentEntryWithReferences? {
        val entry = getPaymentEntry(instanceId, companyId, paymentEntryId) ?: return null
        return PaymentEntryWithReferences(
            paymentEntry = entry,
            references = getPaymentEntryReferences(instanceId, companyId, paymentEntryId)
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentEntry(entry: PaymentEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferences(references: List<PaymentEntryReferenceEntity>)

    @Transaction
    suspend fun insertPaymentEntryWithReferences(
        entry: PaymentEntryEntity,
        references: List<PaymentEntryReferenceEntity>
    ) {
        insertPaymentEntry(entry)
        if (references.isNotEmpty()) {
            insertReferences(references)
        }
    }

    @Transaction
    suspend fun getPendingPaymentEntriesWithReferences(
        instanceId: String,
        companyId: String
    ): List<PaymentEntryWithReferences> {
        return getPendingPaymentEntries(instanceId, companyId).map { entry ->
            PaymentEntryWithReferences(
                paymentEntry = entry,
                references = getPaymentEntryReferences(instanceId, companyId, entry.paymentEntryId)
            )
        }
    }

    @Query(
        """
        SELECT * FROM payment_entry_references
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND paymentEntryId = :paymentEntryId
    """
    )
    suspend fun getPaymentEntryReferences(
        instanceId: String,
        companyId: String,
        paymentEntryId: String
    ): List<PaymentEntryReferenceEntity>

    @Query(
        """
        SELECT * FROM payment_entries
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
          AND syncStatus = 'PENDING'
    """
    )
    suspend fun getPendingPaymentEntries(
        instanceId: String,
        companyId: String
    ): List<PaymentEntryEntity>

    @Query(
        """
      UPDATE payment_entries
      SET syncStatus = :syncStatus,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND paymentEntryId = :paymentEntryId
    """
    )
    suspend fun updateSyncStatus(
        instanceId: String,
        companyId: String,
        paymentEntryId: String,
        syncStatus: String,
        lastSyncedAt: Long?,
        updatedAt: Long
    )

    @Query(
        """
      UPDATE payment_entries
      SET partyId = :newCustomerId,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND partyType = 'Customer'
        AND partyId = :oldCustomerId
    """
    )
    suspend fun replaceCustomerReference(
        instanceId: String,
        companyId: String,
        oldCustomerId: String,
        newCustomerId: String,
        updatedAt: Long
    )
}
