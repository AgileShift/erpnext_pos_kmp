package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.PaymentModesEntity

@Dao
interface ModeOfPaymentDao {
    @Query("SELECT * FROM tabpaymentmodes WHERE profileId = :profileId ORDER BY mode_of_payment ASC")
    suspend fun getAll(profileId: String): List<PaymentModesEntity>

    @Query("SELECT * FROM tabModeOfPayment")
    suspend fun getAllModes(): List<ModeOfPaymentEntity>
}
