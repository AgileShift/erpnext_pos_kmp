package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.erpnext.pos.localSource.entities.POSProfileEntity

@Dao
interface POSProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pos: List<POSProfileEntity>)

    @Query("SELECT * FROM tabPosProfile WHERE is_deleted = 0 AND profile_name = :profileId")
    suspend fun getPOSProfile(profileId: String): POSProfileEntity

    @Query("SELECT * FROM tabPosProfile WHERE is_deleted = 0 AND active = 1")
    suspend fun getActiveProfile(): POSProfileEntity?

    @Transaction
    suspend fun updateProfileState(user: String?, profile: String, status: Boolean) {
        val activeProfile = getPOSProfile(profile)
        update(activeProfile.copy(active = status, user = user))
    }

    @Update
    suspend fun update(profile: POSProfileEntity): Int

    @Query("UPDATE tabPosProfile SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("DELETE FROM tabPosProfile WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabPosProfile SET is_deleted = 1 WHERE is_deleted = 0 AND profile_name NOT IN (:profileNames)")
    suspend fun softDeleteNotIn(profileNames: List<String>)

    @Query("DELETE FROM tabPosProfile WHERE is_deleted = 1 AND profile_name NOT IN (:profileNames)")
    suspend fun hardDeleteDeletedNotIn(profileNames: List<String>)
}
