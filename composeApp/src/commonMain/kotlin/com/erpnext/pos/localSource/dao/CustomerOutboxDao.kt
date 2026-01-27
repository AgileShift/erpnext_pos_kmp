package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.CustomerOutboxEntity

@Dao
interface CustomerOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomerOutboxEntity)

    @Query("SELECT * FROM customer_outbox WHERE status != 'Synced' ORDER BY created_at ASC")
    suspend fun getPending(): List<CustomerOutboxEntity>

    @Query(
        """
        UPDATE customer_outbox
        SET status = :status,
            last_error = :error,
            attempts = attempts + :attemptIncrement,
            last_attempt_at = :attemptAt
        WHERE local_id = :localId
        """
    )
    suspend fun updateStatus(
        localId: String,
        status: String,
        error: String?,
        attemptIncrement: Int,
        attemptAt: Long
    )

    @Query(
        """
        UPDATE customer_outbox
        SET remote_id = :remoteId
        WHERE local_id = :localId
        """
    )
    suspend fun updateRemoteId(localId: String, remoteId: String)

    @Query("DELETE FROM customer_outbox WHERE customer_local_id NOT IN (:ids)")
    suspend fun deleteByCustomerIdsNotIn(ids: List<String>)
}
