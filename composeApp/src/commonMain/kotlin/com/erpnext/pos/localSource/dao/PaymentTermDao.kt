package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.PaymentTermEntity

@Dao
interface PaymentTermDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terms: List<PaymentTermEntity>)

    @Query("SELECT * FROM tabPaymentTerm ORDER BY payment_term_name ASC")
    suspend fun getAll(): List<PaymentTermEntity>

    @Query("DELETE FROM tabPaymentTerm")
    suspend fun deleteAll()

    @Query("SELECT * FROM tabPaymentTerm ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): PaymentTermEntity?
}
