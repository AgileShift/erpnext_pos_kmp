package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto

class PaymentEntryRepository(
    private val api: APIService
) {
    suspend fun createPaymentEntry(entry: PaymentEntryCreateDto) {
        api.createPaymentEntry(entry)
    }
}
