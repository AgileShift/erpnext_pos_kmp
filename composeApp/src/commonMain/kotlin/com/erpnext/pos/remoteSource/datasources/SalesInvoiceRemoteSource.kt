package com.erpnext.pos.remoteSource.datasources

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.PaymentEntryDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.paging.InvoiceRemoteMediator
import com.erpnext.pos.remoteSource.sdk.ERPDocType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalPagingApi::class)
class SalesInvoiceRemoteSource(
    private val apiService: APIService,
    private val pendingInvoiceDao: SalesInvoiceDao,
) {

    suspend fun fetchInvoices(
        posProfile: String,
        recentPaidOnly: Boolean = false
    ): List<SalesInvoiceDto> =
        apiService.fetchAllInvoicesCombined(posProfile, recentPaidOnly = recentPaidOnly)

    suspend fun fetchStockForItems(
        warehouse: String,
        itemCodes: List<String>
    ): Map<String, Double> = apiService.fetchStockForItems(warehouse, itemCodes)

    suspend fun findExistingInvoiceName(
        posOpeningEntry: String?,
        postingDate: String?,
        customer: String?,
        grandTotal: Double?
    ): String? {
        return apiService.findInvoiceBySignature(
            doctype = ERPDocType.SalesInvoice.path,
            posOpeningEntry = posOpeningEntry,
            postingDate = postingDate,
            customer = customer,
            grandTotal = grandTotal
        )
    }

    suspend fun fetchInvoice(name: String): SalesInvoiceDto? =
        runCatching { apiService.getSalesInvoiceByName(name) }.getOrNull()

    suspend fun fetchInvoiceSmart(name: String, isPosHint: Boolean? = null): SalesInvoiceDto? {
        return fetchInvoice(name)
    }
    //apiService.getInvoiceDetail(name, baseUrl, headers)

    suspend fun fetchOutstandingInvoicesForCustomer(
        customerName: String,
        posProfile: String
    ): List<SalesInvoiceDto> {
        return apiService.getCustomerOutstanding(customerName, posProfile).pendingInvoices
    }

    suspend fun fetchReturnInvoices(
        returnAgainst: String,
        posProfile: String
    ): List<SalesInvoiceDto> {
        val names = apiService.fetchReturnInvoiceNames(returnAgainst, posProfile)
        if (names.isEmpty()) return emptyList()
        return names.mapNotNull { name ->
            fetchInvoice(name)
        }
    }

    suspend fun createInvoice(invoice: SalesInvoiceDto): SalesInvoiceDto =
        apiService.createSalesInvoice(invoice)

    suspend fun updateInvoice(name: String, invoice: SalesInvoiceDto): SalesInvoiceDto =
        apiService.updateSalesInvoice(name, invoice)

    suspend fun cancelInvoice(name: String) =
        apiService.cancelSalesInvoice(name)

    suspend fun fetchPaymentEntries(fromDate: String): List<PaymentEntryDto> =
        apiService.fetchPaymentEntries(fromDate)

    suspend fun fetchPaymentEntry(name: String): PaymentEntryDto? =
        runCatching { apiService.getPaymentEntryByName(name) }.getOrNull()

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
