package com.erpnext.pos.remoteSource.datasources

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.erpnext.pos.localSource.dao.PendingInvoiceDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.PendingSalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.remoteSource.mapper.toEntities
import com.erpnext.pos.remoteSource.paging.InvoiceRemoteMediator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalPagingApi::class)
class SalesInvoiceRemoteSource(
    private val apiService: APIService,
    private val pendingInvoiceDao: PendingInvoiceDao,
) {

    suspend fun fetchInvoices(
        limit: Int,
        offset: Int
    ): List<SalesInvoiceDto> =
        emptyList()  //apiService.getPendingInvoices(limit, offset, baseUrl, headers)

    suspend fun fetchInvoice(name: String): SalesInvoiceDto? = null
    //apiService.getInvoiceDetail(name, baseUrl, headers)

    suspend fun createInvoice(invoice: SalesInvoiceDto): SalesInvoiceDto =
        apiService.createSalesInvoice(invoice)

    suspend fun updateInvoice(name: String, invoice: SalesInvoiceDto): SalesInvoiceDto =
        apiService.updateSalesInvoice(name, invoice)

    suspend fun deleteInvoice(name: String) = null
    //apiService.delete(name, baseUrl, headers)

    fun getAllInvoices(
        posProfileName: String,
        query: String? = null,
        date: String? = null,
    ): Flow<PagingData<PendingSalesInvoiceEntity>> {
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