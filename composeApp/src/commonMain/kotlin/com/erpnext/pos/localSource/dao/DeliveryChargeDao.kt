package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.DeliveryChargeEntity

@Dao
interface DeliveryChargeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(charges: List<DeliveryChargeEntity>)

    @Query("SELECT * FROM tabDeliveryCharge WHERE is_deleted = 0 ORDER BY label ASC")
    suspend fun getAll(): List<DeliveryChargeEntity>

    @Query("UPDATE tabDeliveryCharge SET is_deleted = 1 WHERE is_deleted = 0 AND label NOT IN (:labels)")
    suspend fun softDeleteNotIn(labels: List<String>)

    @Query("DELETE FROM tabDeliveryCharge WHERE is_deleted = 1 AND label NOT IN (:labels)")
    suspend fun hardDeleteDeletedNotIn(labels: List<String>)

    @Query("DELETE FROM tabDeliveryCharge WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabDeliveryCharge SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("SELECT * FROM tabDeliveryCharge WHERE is_deleted = 0 ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): DeliveryChargeEntity?
}
