package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.ContactChildDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.OutstandingInfo
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.utils.isLikelyPosInvoiceName

class CustomerRemoteSource(
    private val api: APIService,
) {
    suspend fun fetchCustomers(territory: String?): List<CustomerDto> {
        return api.getCustomers(territory)
    }

    suspend fun getCustomerOutstanding(customerId: String, posProfile: String): OutstandingInfo {
        return api.getCustomerOutstanding(customerId, posProfile)
    }

    suspend fun getCustomerAddress(customerId: String): String? {
        val address = api.getCustomerAddress(customerId) ?: return null
        val parts = listOfNotNull(
            address.line1.takeIf { it.isNotBlank() },
            address.line2?.takeIf { it.isNotBlank() },
            address.city.takeIf { it.isNotBlank() },
            address.country.takeIf { it.isNotBlank() }
        )
        return parts.joinToString(", ").ifBlank { null }
    }

    suspend fun getCustomerContact(customerId: String): ContactChildDto? {
        return api.getCustomerContact(customerId)
    }

    suspend fun fetchInvoices(posProfile: String): List<SalesInvoiceDto> {
        return api.fetchAllInvoices(posProfile)
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
    }

    suspend fun fetchInvoiceByName(invoiceName: String): SalesInvoiceDto? {
        return runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
    }

    suspend fun fetchInvoiceDetail(invoice: SalesInvoiceDto): SalesInvoiceDto? {
        val name = invoice.name ?: return null
        return runCatching {
            val isPosHint =
                invoice.doctype.equals("POS Invoice", ignoreCase = true) || invoice.isPos
            val likelyPos = isPosHint || isLikelyPosInvoiceName(name)
            if (likelyPos) api.getPOSInvoiceByName(name) else api.getSalesInvoiceByName(name)
        }.getOrNull() ?: runCatching { api.getSalesInvoiceByName(name) }.getOrNull()
            ?: runCatching { api.getPOSInvoiceByName(name) }.getOrNull()
    }

    suspend fun fetchInvoiceByNameSmart(invoiceName: String, isPosHint: Boolean? = null): SalesInvoiceDto? {
        val likelyPos = isPosHint == true || isLikelyPosInvoiceName(invoiceName)
        return if (likelyPos) {
            runCatching { api.getPOSInvoiceByName(invoiceName) }.getOrNull()
                ?: runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
        } else {
            runCatching { api.getSalesInvoiceByName(invoiceName) }.getOrNull()
                ?: runCatching { api.getPOSInvoiceByName(invoiceName) }.getOrNull()
        }
    }
}
