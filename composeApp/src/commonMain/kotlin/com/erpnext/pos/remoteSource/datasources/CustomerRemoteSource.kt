package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

class CustomerRemoteSource(
    private val api: APIService,
) {
    suspend fun fetchCustomers(territory: String?): List<CustomerDto> {
        return api.getCustomers(territory)
    }

    suspend fun fetchInvoices(
        posProfile: String,
        recentPaidOnly: Boolean = false
    ): List<SalesInvoiceDto> {
        return api.fetchAllInvoicesCombined(posProfile, recentPaidOnly = recentPaidOnly)
    }

    suspend fun fetchAllOutstandingInvoices(posProfile: String): List<SalesInvoiceDto> {
        return api.getAllOutstandingInvoices(posProfile)
    }

    suspend fun fetchInvoicesForCustomerPeriod(
        customerId: String,
        startDate: String,
        endDate: String,
        posProfile: String
    ): List<SalesInvoiceDto> {
        return api.fetchCustomerInvoicesForPeriod(customerId, startDate, endDate, posProfile)
            .distinctBy { it.name }
            .sortedByDescending { it.postingDate }
    }

    suspend fun fetchInvoiceByName(invoiceName: String): SalesInvoiceDto? {
        return runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
    }

    suspend fun fetchInvoiceDetail(invoice: SalesInvoiceDto): SalesInvoiceDto? {
        val name = invoice.name ?: return null
        return runCatching { api.getSalesInvoiceByName(name) }.getOrNull()
    }

    suspend fun fetchInvoiceByNameSmart(invoiceName: String, isPosHint: Boolean? = null): SalesInvoiceDto? {
        return runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
    }
}
