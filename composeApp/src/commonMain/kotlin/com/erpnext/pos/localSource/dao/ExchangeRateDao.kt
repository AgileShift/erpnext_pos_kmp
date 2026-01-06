package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: ExchangeRateEntity)

    @Query(
        """
        SELECT * FROM tabExchangeRate
        WHERE from_currency = :fromCurrency AND to_currency = :toCurrency
        LIMIT 1
        """
    )
    suspend fun getRate(fromCurrency: String, toCurrency: String): ExchangeRateEntity?

    @Query("SELECT * FROM tabExchangeRate ORDER BY last_synced_at ASC LIMIT 1")
    suspend fun getOldest(): ExchangeRateEntity?

    @Query("DELETE FROM tabExchangeRate")
    suspend fun clear()
}
