package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.PrintJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintJobDao {
  @Query("SELECT * FROM print_jobs ORDER BY created_at_epoch_ms DESC")
  fun observeJobs(): Flow<List<PrintJobEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: PrintJobEntity)

  @Query("SELECT * FROM print_jobs WHERE status IN ('PENDING', 'RETRYING') ORDER BY created_at_epoch_ms ASC")
  suspend fun getPendingJobs(): List<PrintJobEntity>

  @Query("SELECT * FROM print_jobs WHERE last_error IS NOT NULL ORDER BY created_at_epoch_ms DESC LIMIT 1")
  suspend fun latestError(): PrintJobEntity?
}
