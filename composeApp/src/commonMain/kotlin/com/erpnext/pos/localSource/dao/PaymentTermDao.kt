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

    @Query("SELECT * FROM tabPaymentTerm WHERE is_deleted = 0 ORDER BY payment_term_name ASC")
    suspend fun getAll(): List<PaymentTermEntity>

    @Query("UPDATE tabPaymentTerm SET is_deleted = 1 WHERE is_deleted = 0 AND payment_term_name NOT IN (:names)")
    suspend fun softDeleteNotIn(names: List<String>)

    @Query("DELETE FROM tabPaymentTerm WHERE is_deleted = 1 AND payment_term_name NOT IN (:names)")
    suspend fun hardDeleteDeletedNotIn(names: List<String>)

    @Query("DELETE FROM tabPaymentTerm WHERE is_deleted = 1")
    suspend fun hardDeleteAllDeleted()

    @Query("UPDATE tabPaymentTerm SET is_deleted = 1 WHERE is_deleted = 0")
    suspend fun softDeleteAll()

    @Query("SELECT * FROM tabPaymentTerm WHERE is_deleted = 0 ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): PaymentTermEntity?
}
