package com.erpnext.pos.remoteSource.datasources

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.paging.InvoiceRemoteMediator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalPagingApi::class)
class SalesInvoiceRemoteSource(
    private val apiService: APIService,
    private val pendingInvoiceDao: SalesInvoiceDao,
) {

    suspend fun fetchInvoices(
        posProfile: String
    ): List<SalesInvoiceDto> = apiService.fetchAllInvoices(posProfile)

    suspend fun fetchInvoice(name: String): SalesInvoiceDto? =
        runCatching { apiService.getSalesInvoiceByName(name) }.getOrNull()
    //apiService.getInvoiceDetail(name, baseUrl, headers)

    suspend fun fetchOutstandingInvoicesForCustomer(customerName: String): List<SalesInvoiceDto> {
        return apiService.getCustomerOutstanding(customerName).pendingInvoices
    }

    suspend fun createInvoice(invoice: SalesInvoiceDto): SalesInvoiceDto =
        apiService.createSalesInvoice(invoice)

    suspend fun updateInvoice(name: String, invoice: SalesInvoiceDto): SalesInvoiceDto =
        apiService.updateSalesInvoice(name, invoice)

    suspend fun submitInvoice(name: String) =
        apiService.submitSalesInvoice(name)

    suspend fun cancelInvoice(name: String) =
        apiService.cancelSalesInvoice(name)

    suspend fun deleteInvoice(name: String) = null
    //apiService.delete(name, baseUrl, headers)

    fun getAllInvoices(
        posProfileName: String,
        query: String? = null,
        date: String? = null,
    ): Flow<PagingData<SalesInvoiceWithItemsAndPayments>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 4,
                enablePlaceholders = false,
                initialLoadSize = 40
            ),
            remoteMediator = InvoiceRemoteMediator(apiService, pendingInvoiceDao, posProfileName),
            initialKey = null
        ) {
            pendingInvoiceDao.getFilteredInvoices(query, date)
        }.flow.onStart {
            emit(PagingData.empty())
        }
    }
}
