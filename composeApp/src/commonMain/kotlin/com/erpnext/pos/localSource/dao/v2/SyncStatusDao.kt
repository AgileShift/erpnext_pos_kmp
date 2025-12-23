package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.erpnext.pos.localSource.entities.v2.SyncStateEntity

@Dao
interface SyncStatusDao {

    @Query(
        """
        SELECT * FROM sync_state
        WHERE instanceId = :instanceId
          AND companyId = :companyId
    """
    )
    suspend fun get(
        instanceId: String,
        companyId: String
    ): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncStateEntity)

    @Update
    suspend fun update(entity: SyncStateEntity)

    @Query(
        """
      UPDATE sync_state
      SET isSyncInProgress = :inProgress
      WHERE instanceId = :instanceId AND companyId = :companyId
    """
    )
    suspend fun setInProgress(instanceId: String, companyId: String, inProgress: Boolean)

    @Query(
        """
      UPDATE sync_state
      SET pendingInvoices = :pending,
          failedInvoices = :failed,
          lastFullSyncAt = :lastFullSyncAt
      WHERE instanceId = :instanceId AND companyId = :companyId
    """
    )
    suspend fun updateCounters(
        instanceId: String,
        companyId: String,
        pending: Int,
        failed: Int,
        lastFullSyncAt: Long?
    )

}
