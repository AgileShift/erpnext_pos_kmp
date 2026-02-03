package com.erpnext.pos.domain.ports.remote

import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

interface SalesInvoiceRemotePort {
    suspend fun createInvoice(dto: SalesInvoiceDto): SalesInvoiceDto
    suspend fun fetchInvoicesForTerritory(
        territoryId: String,
        fromDate: String,
    ): List<SalesInvoiceDto>

    suspend fun fetchAllOutstandingInvoices(posProfile: String): List<SalesInvoiceDto>
}
