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
          AND docType = :docType
    """
    )
    suspend fun get(
        instanceId: String,
        companyId: String,
        docType: String
    ): SyncStateEntity?

    @Query(
        """
        SELECT * FROM sync_state
        WHERE instanceId = :instanceId
          AND companyId = :companyId
    """
    )
    suspend fun getAll(
        instanceId: String,
        companyId: String
    ): List<SyncStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncStateEntity)

    @Update
    suspend fun update(entity: SyncStateEntity)

    @Query(
        """
      UPDATE sync_state
      SET isSyncInProgress = :inProgress
      WHERE instanceId = :instanceId AND companyId = :companyId AND docType = :docType
    """
    )
    suspend fun setInProgress(
        instanceId: String,
        companyId: String,
        docType: String,
        inProgress: Boolean
    )

    @Query(
        """
      UPDATE sync_state
      SET pendingInvoices = :pending,
          failedInvoices = :failed,
          lastFullSyncAt = :lastFullSyncAt
      WHERE instanceId = :instanceId AND companyId = :companyId AND docType = :docType
    """
    )
    suspend fun updateCounters(
        instanceId: String,
        companyId: String,
        docType: String,
        pending: Int,
        failed: Int,
        lastFullSyncAt: Long?
    )

    @Query(
        """
      UPDATE sync_state
      SET lastPullAt = :lastPullAt,
          lastError = :lastError
      WHERE instanceId = :instanceId AND companyId = :companyId AND docType = :docType
    """
    )
    suspend fun updatePullState(
        instanceId: String,
        companyId: String,
        docType: String,
        lastPullAt: Long?,
        lastError: String?
    )

    @Query(
        """
      UPDATE sync_state
      SET lastPushAt = :lastPushAt,
          lastError = :lastError
      WHERE instanceId = :instanceId AND companyId = :companyId AND docType = :docType
    """
    )
    suspend fun updatePushState(
        instanceId: String,
        companyId: String,
        docType: String,
        lastPushAt: Long?,
        lastError: String?
    )

}
