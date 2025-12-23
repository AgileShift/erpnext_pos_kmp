package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.repositories.ICashboxRepository
import com.erpnext.pos.localSource.datasources.CashboxLocalSource
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.views.CashBoxManager

class CashboxRemoteSource {}
class CashboxRepository(
    private val localSource: CashboxLocalSource,
    private val remoteSource: CashboxRemoteSource,
    private val context: CashBoxManager
) : ICashboxRepository {
    override suspend fun openCashbox(entry: POSOpeningEntryDto) {
        TODO("Not yet implemented")
    }

    override suspend fun closeCashbox(entry: POSClosingEntryDto) {
        TODO("Not yet implemented")
    }
}