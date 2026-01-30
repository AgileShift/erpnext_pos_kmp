package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.utils.RepoTrace

class PaymentEntryRepository(
    private val api: APIService
) {
    suspend fun createPaymentEntry(entry: PaymentEntryCreateDto): String {
        RepoTrace.breadcrumb("PaymentEntryRepository", "createPaymentEntry")
        return runCatching {
            val created = api.createPaymentEntry(entry)
            api.submitPaymentEntry(created.name)
            created.name
        }
            .getOrElse {
                RepoTrace.capture("PaymentEntryRepository", "createPaymentEntry", it)
                throw it
            }
    }
}
