package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.PosProfileLocalEntity

@Dao
interface PosProfileLocalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PosProfileLocalEntity>)

    @Query("SELECT * FROM tabPosProfileLocal WHERE is_deleted = 0 ORDER BY profile_name ASC")
    suspend fun getAll(): List<PosProfileLocalEntity>

    @Query("SELECT * FROM tabPosProfileLocal WHERE is_deleted = 0 AND profile_name = :profileId")
    suspend fun getProfile(profileId: String): PosProfileLocalEntity?

    @Query("SELECT COUNT(*) FROM tabPosProfileLocal WHERE is_deleted = 0")
    suspend fun countAll(): Int

    @Query("UPDATE tabPosProfileLocal SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("DELETE FROM tabPosProfileLocal WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabPosProfileLocal SET is_deleted = 1 WHERE is_deleted = 0 AND profile_name NOT IN (:profileNames)")
    suspend fun softDeleteNotIn(profileNames: List<String>)

    @Query("DELETE FROM tabPosProfileLocal WHERE is_deleted = 1 AND profile_name NOT IN (:profileNames)")
    suspend fun hardDeleteDeletedNotIn(profileNames: List<String>)
}
