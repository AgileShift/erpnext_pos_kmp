package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.ExchangeRateDao
import com.erpnext.pos.localSource.entities.ExchangeRateEntity

class ExchangeRateLocalSource(private val dao: ExchangeRateDao) {
    suspend fun getRate(fromCurrency: String, toCurrency: String): ExchangeRateEntity? {
        return dao.getRate(fromCurrency, toCurrency)
    }

    suspend fun save(rate: ExchangeRateEntity) {
        dao.insert(rate)
    }

    suspend fun getOldest(): ExchangeRateEntity? = dao.getOldest()

    suspend fun clear() = dao.clear()
}
