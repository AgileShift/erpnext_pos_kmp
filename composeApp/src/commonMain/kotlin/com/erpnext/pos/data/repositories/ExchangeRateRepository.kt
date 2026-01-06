package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.datasources.ExchangeRateLocalSource
import com.erpnext.pos.localSource.entities.ExchangeRateEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.sync.SyncTTL

class ExchangeRateRepository(
    private val localSource: ExchangeRateLocalSource,
    private val api: APIService
) {
    suspend fun getRate(fromCurrency: String, toCurrency: String): Double? {
        val normalizedFrom = fromCurrency.trim().uppercase()
        val normalizedTo = toCurrency.trim().uppercase()
        if (normalizedFrom.isBlank() || normalizedTo.isBlank()) return null
        if (normalizedFrom == normalizedTo) return 1.0

        val cached = localSource.getRate(normalizedFrom, normalizedTo)
        if (cached != null && !SyncTTL.isExpired(cached.lastSyncedAt)) {
            return cached.rate
        }

        val fetched = fetchFromApi(normalizedFrom, normalizedTo)
        if (fetched != null) {
            localSource.save(ExchangeRateEntity.fromPair(normalizedFrom, normalizedTo, fetched))
            return fetched
        }

        if (cached != null) {
            return cached.rate
        }

        return null
    }

    private suspend fun fetchFromApi(fromCurrency: String, toCurrency: String): Double? {
        return api.getExchangeRate(fromCurrency = fromCurrency, toCurrency = toCurrency)
            ?.takeIf { it > 0.0 }
            ?: api.getExchangeRate(fromCurrency = toCurrency, toCurrency = fromCurrency)
                ?.takeIf { it > 0.0 }
                ?.let { 1 / it }
    }

    suspend fun clearCache() {
        localSource.clear()
    }
}
