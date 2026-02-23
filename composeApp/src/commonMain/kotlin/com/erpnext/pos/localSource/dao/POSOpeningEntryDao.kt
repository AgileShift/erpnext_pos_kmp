package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningWithClosingAndTaxes
import kotlinx.coroutines.flow.Flow

@Dao
interface POSOpeningEntryDao {
    @Query("SELECT * FROM tab_pos_opening_entry ORDER BY period_start_date DESC")
    fun getAll(): Flow<List<POSOpeningEntryEntity>>

    @Query("SELECT * FROM tab_pos_opening_entry WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): POSOpeningEntryEntity?

    @Query("SELECT * FROM tab_pos_opening_entry WHERE pending_sync = true")
    fun getAllPendingSync(): Flow<List<POSOpeningEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: POSOpeningEntryEntity)

    @Transaction
    @Query("SELECT * FROM tab_pos_opening_entry WHERE name = :name")
    suspend fun getOpeningWithClosingAndTaxes(name: String): POSOpeningWithClosingAndTaxes?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<POSOpeningEntryEntity>)

    @Update
    suspend fun update(entry: POSOpeningEntryEntity)

    @Query("UPDATE tab_pos_opening_entry SET pending_sync = 0 WHERE name = :name")
    suspend fun markSynced(name: String): Int

    @Query("DELETE FROM tab_pos_opening_entry WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM tab_pos_opening_entry")
    suspend fun clear()
}
