package com.erpnext.pos.data.repositories

import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.erpnext.pos.base.Resource
import com.erpnext.pos.base.networkBoundResource
import com.erpnext.pos.base.networkBoundResourcePaged
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.repositories.IInventoryRepository
import com.erpnext.pos.domain.usecases.StockDelta
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.remoteSource.datasources.InventoryRemoteSource
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.sync.SyncTTL
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

    fun <Key : Any, Value : Any, R : Any> PagingSource<Key, Value>.mapEntityToBO(
        transform: (Value) -> R = { (it as ItemEntity).toBO() as R }
    ): PagingSource<Key, R> {
        return object : PagingSource<Key, R>() {

            override suspend fun load(params: LoadParams<Key>): LoadResult<Key, R> {
                val result = this@mapEntityToBO.load(params)
                return when (result) {
                    is LoadResult.Page -> LoadResult.Page(
                        data = result.data.map(transform),
                        prevKey = result.prevKey,
                        nextKey = result.nextKey
                    )

                    is LoadResult.Error -> LoadResult.Error(result.throwable)
                    is LoadResult.Invalid -> LoadResult.Invalid()
                }
            }

            override fun getRefreshKey(state: PagingState<Key, R>): Key? =
                null // Puedes personalizar si lo necesitas
        }
    }

    suspend fun getItems(query: String?): Flow<List<ItemBO>> {
        val context = context.requireContext()
        return networkBoundResource(
            query = {
                when {
                    query.isNullOrEmpty() -> flowOf(localSource.getAll())
                    else -> localSource.getAllFiltered(query)
                }.map { list -> list.map { it.toBO() } }
            },
            fetch = {
                remoteSource.getItems(
                    context.warehouse,
                    context.priceList,
                )
            },
            saveFetchResult = { localSource.insertAll(it.toEntity()) },
            onFetchFailed = { },
            shouldFetch = { localData ->
                localData.isEmpty() ||
                        SyncTTL.isExpired(localData.maxOf { it.lastSyncedAt?.toDouble() ?: 0.0 }
                            .toLong())
            }
        ).map { resource ->
            resource.data ?: emptyList()
        }
    }

    override suspend fun getItemsPaged(query: String?): Flow<PagingData<ItemBO>> {
        val context = context.requireContext()
        val items: Flow<PagingData<ItemBO>> = networkBoundResourcePaged(
            query = {
                localSource.getAllPaged().mapEntityToBO()
            },
            fetch = { page, pageSize ->
                remoteSource.getItems(
                    context.warehouse,
                    context.priceList,
                    page * pageSize,
                    pageSize
                )
            },
            saveFetchResult = { localSource.insertAll(it.toEntity()) },
            clearLocalData = { localSource.deleteAll() },
            shouldFetch = {
                val first = localSource.getOldestItem()
                first == null || SyncTTL.isExpired(first.lastSyncedAt)
            },
            pageSize = 50
        )
        return items
    }

    override suspend fun getItemDetails(itemId: String): ItemBO {
        return remoteSource.getItemDetail(itemId).toBO()
    }

    override suspend fun getCategories(): Flow<Resource<List<CategoryBO>>> = networkBoundResource(
        query = {
            localSource.getItemCategories().map { entities -> entities.map { it.toBO() } }
        },
        fetch = { remoteSource.getCategories() },
        saveFetchResult = { categories ->
            localSource.deleteAllCategories()
            localSource.insertCategories(categories.map { it.toEntity() })
        },
        shouldFetch = { cached -> cached.isEmpty() },
        onFetchFailed = { e ->
            print("Fallo la sincronizacion de categorias -> ${e.message}")
        }
    )

    override suspend fun sync(): Flow<Resource<List<ItemBO>>> {
        val context = context.requireContext()
        return networkBoundResource(
            query = { flowOf(localSource.getAll().toBO()) },
            fetch = {
                remoteSource.getItems(
                    context.warehouse,
                    context.priceList,
                ).toBO()
            },
            saveFetchResult = { localSource.insertAll(it.toEntity()) },
            shouldFetch = {
                val first = localSource.getOldestItem()
                first == null || SyncTTL.isExpired(first.lastSyncedAt)
            },
        )
    }
}