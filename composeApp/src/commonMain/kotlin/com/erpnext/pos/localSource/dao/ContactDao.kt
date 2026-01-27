package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ContactEntity

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM tabContact WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getAll(): List<ContactEntity>

    @Query("UPDATE tabContact SET is_deleted = 1 WHERE is_deleted = 0 AND name NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM tabContact WHERE is_deleted = 1 AND name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("UPDATE tabContact SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("DELETE FROM tabContact WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("SELECT * FROM tabContact WHERE is_deleted = 0 ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): ContactEntity?
}
