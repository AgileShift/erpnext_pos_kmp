package com.erpnext.pos.data.adapters.remote

import com.erpnext.pos.domain.ports.remote.SalesInvoiceRemotePort
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

class SalesInvoiceRemoteAdapter(
    private val api: APIService
) : SalesInvoiceRemotePort {
    override suspend fun createInvoice(dto: SalesInvoiceDto): SalesInvoiceDto {
        return api.createSalesInvoice(dto)
    }

    override suspend fun fetchInvoicesForTerritory(
        territoryId: String,
        fromDate: String
    ): List<SalesInvoiceDto> {
        return api.fetchInvoicesForTerritoryFromDate(
            territoryId, fromDate
        )
    }

    override suspend fun fetchAllOutstandingInvoices(): List<SalesInvoiceDto> {
        return api.getAllOutstandingInvoices()
    }
}