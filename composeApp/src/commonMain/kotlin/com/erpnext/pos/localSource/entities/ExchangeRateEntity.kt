package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabExchangeRate")
data class ExchangeRateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "from_currency")
    val fromCurrency: String,

    @ColumnInfo(name = "to_currency")
    val toCurrency: String,

    @ColumnInfo(name = "rate")
    val rate: Double,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        fun buildId(from: String, to: String): String {
            return "${from.uppercase()}-${to.uppercase()}"
        }

        fun fromPair(from: String, to: String, rate: Double): ExchangeRateEntity {
            return ExchangeRateEntity(
                id = buildId(from, to),
                fromCurrency = from.uppercase(),
                toCurrency = to.uppercase(),
                rate = rate
            )
        }
    }
}
