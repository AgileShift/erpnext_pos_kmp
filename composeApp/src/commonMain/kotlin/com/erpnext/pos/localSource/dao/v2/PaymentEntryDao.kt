package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
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
    suspend fun getPaymentEntryWithReferences(
        instanceId: String,
        companyId: String,
        paymentEntryId: String
    ): PaymentEntryWithReferences?
}
