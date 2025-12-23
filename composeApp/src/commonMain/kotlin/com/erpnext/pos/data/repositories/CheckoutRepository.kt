package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.remoteSource.datasources.SalesInvoiceRemoteSource
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

interface ICheckoutRepository {
    suspend fun fetchInventoryAvailable(): List<ItemDto>
    suspend fun createInvoice(data: SalesInvoiceDto): Long
    suspend fun checkCreditToCustomer(customerId: String)
    suspend fun calculateTotalAndTaxes(): Double
    suspend fun countPending(): Int
}

class CheckoutRepository(
    private val remoteSource: SalesInvoiceRemoteSource,
    private val localSource: InvoiceLocalSource
) : ICheckoutRepository {
    override suspend fun fetchInventoryAvailable(): List<ItemDto> {
        TODO("Not yet implemented")
    }

    override suspend fun createInvoice(data: SalesInvoiceDto): Long {
        TODO("Not yet implemented")
    }

    override suspend fun checkCreditToCustomer(customerId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun calculateTotalAndTaxes(): Double {
        TODO("Not yet implemented")
    }

    override suspend fun countPending(): Int {
        TODO("Not yet implemented")
    }
}