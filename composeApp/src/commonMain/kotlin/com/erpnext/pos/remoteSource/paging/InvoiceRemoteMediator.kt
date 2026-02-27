package com.erpnext.pos.remoteSource.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

@OptIn(ExperimentalPagingApi::class)
class InvoiceRemoteMediator(
    private val apiService: APIService,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val posProfile: String,
    private val pageSize: Int = 20,
    private val preserveCacheOnEmptyRefresh: Boolean = true
) : RemoteMediator<Int, SalesInvoiceWithItemsAndPayments>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SalesInvoiceWithItemsAndPayments>
    ): MediatorResult {
        return withContext(Dispatchers.IO) {
            try {
                val offset = when (loadType) {
                    LoadType.REFRESH -> 0
                    LoadType.PREPEND -> return@withContext MediatorResult.Success(
                        endOfPaginationReached = true
                    )

                    LoadType.APPEND -> salesInvoiceDao.countAll()
                }

                val fetched = apiService.fetchAllInvoices(
                    posProfile = posProfile,
                    offset = offset,
                    limit = pageSize
                )
                val entities = fetched.toEntities().map { mergeLocalInvoiceFields(it) }
                val endReached = entities.isEmpty() || entities.size < pageSize

                when (loadType) {
                    LoadType.REFRESH -> {
                        if (!preserveCacheOnEmptyRefresh || entities.isNotEmpty()) {
                            if (entities.isNotEmpty()) salesInvoiceDao.insertFullInvoices(entities)
                        }
                    }

                    LoadType.APPEND -> {
                        if (entities.isNotEmpty()) salesInvoiceDao.insertFullInvoices(entities)
                    }

                    else -> {}
                }

                MediatorResult.Success(endOfPaginationReached = endReached)
            } catch (e: IOException) {
                MediatorResult.Error(e)
            } catch (e: Exception) {
                MediatorResult.Error(e)
            }
        }
    }

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    private suspend fun mergeLocalInvoiceFields(
        payload: SalesInvoiceWithItemsAndPayments
    ): SalesInvoiceWithItemsAndPayments {
        val invoiceName = payload.invoice.invoiceName?.trim().orEmpty()
        if (invoiceName.isBlank()) return payload
        val local = salesInvoiceDao.getInvoiceByName(invoiceName)?.invoice ?: return payload
        val mergedInvoice = payload.invoice.copy(
            profileId = payload.invoice.profileId?.takeIf { it.isNotBlank() } ?: local.profileId,
            posOpeningEntry = payload.invoice.posOpeningEntry?.takeIf { it.isNotBlank() }
                ?: local.posOpeningEntry,
            warehouse = payload.invoice.warehouse?.takeIf { it.isNotBlank() } ?: local.warehouse
        )
        return payload.copy(invoice = mergedInvoice)
    }
}
