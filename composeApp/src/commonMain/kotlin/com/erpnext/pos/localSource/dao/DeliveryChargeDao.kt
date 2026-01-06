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

    @Query("SELECT * FROM tabDeliveryCharge ORDER BY label ASC")
    suspend fun getAll(): List<DeliveryChargeEntity>

    @Query("DELETE FROM tabDeliveryCharge")
    suspend fun deleteAll()

    @Query("SELECT * FROM tabDeliveryCharge ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): DeliveryChargeEntity?
}
