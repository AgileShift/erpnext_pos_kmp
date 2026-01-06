package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.CatalogDao
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.BinDto
import com.erpnext.pos.remoteSource.dto.v2.ItemDto
import com.erpnext.pos.remoteSource.dto.v2.ItemGroupDto
import com.erpnext.pos.remoteSource.dto.v2.ItemPriceDto
import com.erpnext.pos.remoteSource.mapper.v2.toEntity
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.remoteSource.sdk.v2.IncrementalSyncFilters
import com.erpnext.pos.utils.RepoTrace

class CatalogSyncRepository(
    private val catalogDao: CatalogDao,
    private val api: APIServiceV2
) {

    suspend fun syncItemGroups(ctx: SyncContext, modifiedSince: String?): Int {
        RepoTrace.breadcrumb("CatalogSyncRepositoryV2", "syncItemGroups")
        val groups = api.list<ItemGroupDto>(
            doctype = ERPDocType.Category,
            filters = IncrementalSyncFilters.category(modifiedSince)
        )
        if (groups.isNotEmpty()) {
            catalogDao.upsertItemGroups(
                groups.map { it.toEntity(ctx.instanceId, ctx.companyId) }
            )
        }
        return groups.size
    }

    suspend fun syncItems(ctx: SyncContext, modifiedSince: String?): Int {
        RepoTrace.breadcrumb("CatalogSyncRepositoryV2", "syncItems")
        val items = api.list<ItemDto>(
            doctype = ERPDocType.Item,
            filters = IncrementalSyncFilters.item(modifiedSince)
        )
        if (items.isNotEmpty()) {
            catalogDao.upsertItems(
                items.map { it.toEntity(ctx.instanceId, ctx.companyId) }
            )
        }
        return items.size
    }

    suspend fun syncItemPrices(ctx: SyncContext, modifiedSince: String?): Int {
        RepoTrace.breadcrumb("CatalogSyncRepositoryV2", "syncItemPrices")
        val prices = api.list<ItemPriceDto>(
            doctype = ERPDocType.ItemPrice,
            filters = IncrementalSyncFilters.itemPrice(ctx, modifiedSince)
        )
        if (prices.isNotEmpty()) {
            catalogDao.upsertItemPrices(
                prices.map { it.toEntity(ctx.instanceId, ctx.companyId) }
            )
        }
        return prices.size
    }

    suspend fun syncBins(ctx: SyncContext, modifiedSince: String?): Int {
        RepoTrace.breadcrumb("CatalogSyncRepositoryV2", "syncBins")
        val bins = api.list<BinDto>(
            doctype = ERPDocType.Bin,
            filters = IncrementalSyncFilters.bin(ctx, modifiedSince)
        )
        if (bins.isNotEmpty()) {
            catalogDao.upsertInventoryBins(
                bins.map { it.toEntity(ctx.instanceId, ctx.companyId) }
            )
        }
        return bins.size
    }
}
