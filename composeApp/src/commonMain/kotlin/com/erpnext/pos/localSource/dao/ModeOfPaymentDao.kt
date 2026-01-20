package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity

@Dao
interface ModeOfPaymentDao {
    @Query(
        """
        SELECT * 
             FROM tabmodeofpayment
             WHERE company = :company
             ORDER BY mode_of_payment ASC"""
    )
    suspend fun getAll(company: String?): List<ModeOfPaymentEntity>

    @Query("SELECT * FROM tabModeOfPayment WHERE company = :company")
    suspend fun getAllModes(company: String): List<ModeOfPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllModes(items: List<ModeOfPaymentEntity>)

    @Query("SELECT MAX(last_synced_at) FROM tabModeOfPayment WHERE company = :company")
    suspend fun getLastSyncedAt(company: String): Long?
}
