package com.erpnext.pos.localSource.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.PaymentModesEntity

@Dao
interface PaymentModesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PaymentModesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PaymentModesEntity)

    @Query("SELECT * FROM tabPaymentModes ORDER BY name ASC")
    fun getAll(): PaymentModesEntity

    @Query("DELETE FROM tabPaymentModes")
    suspend fun deleteAll()
}