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

    @Query("SELECT * FROM tabPosProfileLocal ORDER BY profile_name ASC")
    suspend fun getAll(): List<PosProfileLocalEntity>

    @Query("SELECT * FROM tabPosProfileLocal WHERE profile_name = :profileId")
    suspend fun getProfile(profileId: String): PosProfileLocalEntity?

    @Query("SELECT COUNT(*) FROM tabPosProfileLocal")
    suspend fun countAll(): Int

    @Query("DELETE FROM tabPosProfileLocal")
    suspend fun deleteAll()

    @Query("DELETE FROM tabPosProfileLocal WHERE profile_name NOT IN (:profileNames)")
    suspend fun deleteNotIn(profileNames: List<String>)
}
