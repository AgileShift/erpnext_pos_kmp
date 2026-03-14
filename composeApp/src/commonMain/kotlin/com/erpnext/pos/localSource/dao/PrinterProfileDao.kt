package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.PrinterProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterProfileDao {
  @Query("SELECT * FROM printer_profiles ORDER BY is_default DESC, name ASC")
  fun observeProfiles(): Flow<List<PrinterProfileEntity>>

  @Query("SELECT * FROM printer_profiles WHERE is_default = 1 LIMIT 1")
  fun observeDefaultProfile(): Flow<PrinterProfileEntity?>

  @Query("SELECT * FROM printer_profiles WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): PrinterProfileEntity?

  @Query("SELECT * FROM printer_profiles WHERE is_default = 1 LIMIT 1")
  suspend fun getDefault(): PrinterProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: PrinterProfileEntity)

  @Query("UPDATE printer_profiles SET is_default = 0")
  suspend fun clearDefaultFlag()

  @Query("UPDATE printer_profiles SET is_default = CASE WHEN id = :id THEN 1 ELSE 0 END")
  suspend fun setDefault(id: String)

  @Query("DELETE FROM printer_profiles WHERE id = :id")
  suspend fun delete(id: String)
}
