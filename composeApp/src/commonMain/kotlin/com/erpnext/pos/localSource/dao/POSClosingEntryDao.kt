package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSClosingWithTaxes
import com.erpnext.pos.localSource.entities.TaxDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface POSClosingEntryDao {

    @Query("SELECT * FROM tab_pos_closing_entry ORDER BY period_end_date DESC")
    fun getAll(): Flow<List<POSClosingEntryEntity>>

    @Query("SELECT * FROM tab_pos_closing_entry WHERE pos_opening_entry = :openingName LIMIT 1")
    suspend fun getByOpeningEntry(openingName: String): POSClosingEntryEntity?

    @Query("SELECT * FROM tab_pos_closing_entry WHERE pending_sync = true")
    fun getAllPendingSync(): Flow<List<POSClosingEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: POSClosingEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxes(taxes: List<TaxDetailsEntity>)

    @Transaction
    @Query("SELECT * FROM tab_pos_closing_entry WHERE name = :name")
    suspend fun getClosingWithTaxes(name: String): POSClosingWithTaxes?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<POSClosingEntryEntity>)

    @Update
    suspend fun update(entry: POSClosingEntryEntity)

    @Query("DELETE FROM tab_pos_closing_entry WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM tab_pos_closing_entry")
    suspend fun clear()
}