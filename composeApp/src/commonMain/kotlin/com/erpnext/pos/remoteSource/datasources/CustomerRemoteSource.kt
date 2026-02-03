package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.utils.isLikelyPosInvoiceName
import com.erpnext.pos.utils.isLikelySalesInvoiceName

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
        val sales = api.fetchCustomerInvoicesForPeriod(customerId, startDate, endDate, posProfile)
        val pos = api.fetchCustomerPosInvoicesForPeriod(customerId, startDate, endDate, posProfile)
        return (sales + pos).distinctBy { it.name }
            .sortedByDescending { it.postingDate }
    }

    suspend fun fetchInvoiceByName(invoiceName: String): SalesInvoiceDto? {
        return runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
    }

    suspend fun fetchInvoiceDetail(invoice: SalesInvoiceDto): SalesInvoiceDto? {
        val name = invoice.name ?: return null
        return runCatching {
            val isPosHint =
                invoice.doctype.equals("POS Invoice", ignoreCase = true) || invoice.isPos
            val likelyPosName = isLikelyPosInvoiceName(name)
            val likelySalesName = isLikelySalesInvoiceName(name)
            val resolvedPos = when {
                likelyPosName -> true
                likelySalesName -> false
                else -> isPosHint
            }
            if (resolvedPos) api.getPOSInvoiceByName(name) else api.getSalesInvoiceByName(name)
        }.getOrNull() ?: runCatching { api.getSalesInvoiceByName(name) }.getOrNull()
            ?: runCatching { api.getPOSInvoiceByName(name) }.getOrNull()
    }

    suspend fun fetchInvoiceByNameSmart(invoiceName: String, isPosHint: Boolean? = null): SalesInvoiceDto? {
        val likelyPosName = isLikelyPosInvoiceName(invoiceName)
        val likelySalesName = isLikelySalesInvoiceName(invoiceName)
        val resolvedPos = when {
            likelyPosName -> true
            likelySalesName -> false
            isPosHint != null -> isPosHint
            else -> null
        }
        return when (resolvedPos) {
            true -> runCatching { api.getPOSInvoiceByName(invoiceName) }.getOrNull()
                ?: runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
            false -> runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
                ?: runCatching { api.getPOSInvoiceByName(invoiceName) }.getOrNull()
            null -> runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
                ?: runCatching { api.getPOSInvoiceByName(invoiceName) }.getOrNull()
        }
    }
}
