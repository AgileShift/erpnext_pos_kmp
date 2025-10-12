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

    @Query("SELECT * FROM tabPosProfile WHERE profile_name = :profileId")
    suspend fun getPOSProfile(profileId: String): POSProfileEntity

    @Query("SELECT * FROM tabPosProfile WHERE active = 1")
    suspend fun getActiveProfile(): POSProfileEntity?

    @Transaction
    suspend fun updateProfileState(profile: String, status: Boolean) {
        val activeProfile = getPOSProfile(profile)
        update(activeProfile.copy(active = status))
    }

    @Update
    suspend fun update(profile: POSProfileEntity): Int

    @Query("DELETE FROM tabposprofile")
    suspend fun deleteAll()
}