package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.StockSettingsDto

class StockSettingsRepository(
    private val api: APIService,
    private val generalPreferences: GeneralPreferences
) {
    suspend fun sync() {
        val settings = api.getStockSettings().firstOrNull()
        applyBootstrapStockSettings(settings)
    }

    suspend fun applyBootstrapStockSettings(settings: StockSettingsDto?) {
        generalPreferences.setAllowNegativeStock(settings?.allowNegativeStock == true)
    }
}
