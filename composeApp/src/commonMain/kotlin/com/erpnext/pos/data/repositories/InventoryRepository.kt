package com.erpnext.pos.data.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.erpnext.pos.base.Resource
import com.erpnext.pos.base.networkBoundResource
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.data.mappers.toPagingBO
import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.repositories.IInventoryRepository
import com.erpnext.pos.domain.usecases.StockDelta
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import com.erpnext.pos.remoteSource.datasources.InventoryRemoteSource
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.sync.SyncTTL
import com.erpnext.pos.utils.RepoTrace
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class InventoryRepository(
    private val remoteSource: InventoryRemoteSource,
    private val localSource: InventoryLocalSource,
    private val context: CashBoxManager
) : IInventoryRepository {

    suspend fun decrementStock(warehouse: String, deltas: List<StockDelta>) {
        localSource.decrementStock(warehouse, deltas)
    }

    suspend fun getItems(query: String?): Flow<List<ItemBO>> {
        RepoTrace.breadcrumb("InventoryRepository", "getItems", "query=$query")
        return when {
            query.isNullOrEmpty() -> flowOf(localSource.getAll())
            else -> localSource.getAllFiltered(query)
        }.map { list -> list.map { it.toBO() } }
    }

    override suspend fun getItemsPaged(query: String?): Flow<PagingData<ItemBO>> {
        RepoTrace.breadcrumb("InventoryRepository", "getItemsPaged", "query=$query")
        return Pager(PagingConfig(pageSize = 50, prefetchDistance = 20)) {
            if (query.isNullOrBlank()) {
                localSource.getAllPaged()
            } else {
                localSource.getAllFilteredPaged(query)
            }
        }.flow.toPagingBO()
    }

    override suspend fun getItemDetails(itemId: String): ItemBO {
        return remoteSource.getItemDetail(itemId).toBO()
    }

    override suspend fun getCategories(): Flow<Resource<List<CategoryBO>>> = networkBoundResource(
        query = { localSource.getItemCategories().map { entities -> entities.map { it.toBO() } } },
        fetch = { emptyList<com.erpnext.pos.remoteSource.dto.CategoryDto>() },
        saveFetchResult = { },
        shouldFetch = { false },
        onFetchFailed = { }
    )

    override suspend fun sync(): Flow<Resource<List<ItemBO>>> {
        RepoTrace.breadcrumb("InventoryRepository", "sync")
        val context = context.requireContext()
        return networkBoundResource(
            query = { flowOf(localSource.getAll().toBO()) },
            fetch = {
                remoteSource.getItems(
                    context.warehouse,
                    context.priceList,
                )
            },
            saveFetchResult = { remoteItems ->
                val entities = remoteItems.toEntity()
                localSource.insertAll(entities)
                localSource.deleteMissing(entities.map { it.itemCode })
            },
            shouldFetch = { true },
            onFetchFailed = { RepoTrace.capture("InventoryRepository", "sync", it) },
            traceName = "InventoryRepository.sync",
            fetchTtlMillis = SyncTTL.DEFAULT_TTL_HOURS * 60L * 60L * 1000L,
            resolveLocalUpdatedAtMillis = { localData ->
                localData.maxOfOrNull { it.lastSyncedAt ?: 0L }?.takeIf { it > 0L }
            }
        )
    }
}
