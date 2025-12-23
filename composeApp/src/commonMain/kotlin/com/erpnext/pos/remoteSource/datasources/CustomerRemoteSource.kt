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
        return null
    }

    suspend fun getCustomerContact(customerId: String): ContactChildDto? {
        return null
    }

    suspend fun fetchInvoices(posProfile: String): List<SalesInvoiceDto> {
        return api.fetchAllInvoices(posProfile)
    }

    suspend fun fetchAllOutstandingInvoices(): List<SalesInvoiceDto> {
        return api.getAllOutstandingInvoices()
    }
}