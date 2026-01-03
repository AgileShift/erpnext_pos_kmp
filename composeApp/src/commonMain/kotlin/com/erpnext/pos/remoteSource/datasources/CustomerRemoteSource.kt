package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.ContactChildDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.OutstandingInfo
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

class CustomerRemoteSource(
    private val api: APIService,
) {
    suspend fun fetchCustomers(territory: String?): List<CustomerDto> {
        return api.getCustomers(territory)
    }

    suspend fun getCustomerOutstanding(customerId: String): OutstandingInfo {
        return api.getCustomerOutstanding(customerId)
    }

    suspend fun getCustomerAddress(customerId: String): String? {
        val address = api.getCustomerAddress(customerId) ?: return null
        val parts = listOf(
            address.line1.takeIf { it.isNotBlank() },
            address.line2?.takeIf { it.isNotBlank() },
            address.city.takeIf { it.isNotBlank() },
            address.country.takeIf { it.isNotBlank() }
        ).filterNotNull()
        return parts.joinToString(", ").ifBlank { null }
    }

    suspend fun getCustomerContact(customerId: String): ContactChildDto? {
        return api.getCustomerContact(customerId)
    }

    suspend fun fetchInvoices(posProfile: String): List<SalesInvoiceDto> {
        return api.fetchAllInvoices(posProfile)
    }

    suspend fun fetchAllOutstandingInvoices(): List<SalesInvoiceDto> {
        return api.getAllOutstandingInvoices()
    }
}
