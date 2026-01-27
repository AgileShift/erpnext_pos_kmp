package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.TerritoryEntity

@Dao
interface TerritoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(territories: List<TerritoryEntity>)

    @Query("SELECT * FROM tabTerritory WHERE is_deleted = 0 ORDER BY territory_name ASC, name ASC")
    suspend fun getAll(): List<TerritoryEntity>

    @Query("UPDATE tabTerritory SET is_deleted = 1 WHERE is_deleted = 0 AND name NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM tabTerritory WHERE is_deleted = 1 AND name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("DELETE FROM tabTerritory WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabTerritory SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("SELECT * FROM tabTerritory WHERE is_deleted = 0 ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): TerritoryEntity?
}
