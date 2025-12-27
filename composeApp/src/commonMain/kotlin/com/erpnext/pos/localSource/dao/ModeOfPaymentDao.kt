package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity

@Dao
interface ModeOfPaymentDao {
    @Query("SELECT * FROM tabModeOfPayment WHERE enabled = 1 ORDER BY mode_of_payment ASC")
    suspend fun getAll(): List<ModeOfPaymentEntity>
}
