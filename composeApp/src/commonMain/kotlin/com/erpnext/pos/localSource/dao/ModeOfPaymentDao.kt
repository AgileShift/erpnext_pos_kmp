package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.PaymentModesEntity

@Dao
interface ModeOfPaymentDao {
    @Query("SELECT * FROM tabmodeofpayment ORDER BY mode_of_payment ASC")
    suspend fun getAll(): List<ModeOfPaymentEntity>

    @Query("SELECT * FROM tabModeOfPayment")
    suspend fun getAllModes(): List<ModeOfPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllModes(items: List<ModeOfPaymentEntity>)
}
