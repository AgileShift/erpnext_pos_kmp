package com.erpnext.pos.domain.repositories

import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto


interface ICashboxRepository {
    suspend fun openCashbox(entry: POSOpeningEntryDto)
    suspend fun closeCashbox(entry: POSClosingEntryDto)
}