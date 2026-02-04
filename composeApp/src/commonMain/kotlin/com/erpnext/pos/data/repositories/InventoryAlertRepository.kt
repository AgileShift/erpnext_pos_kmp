package com.erpnext.pos.data.repositories

import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.dao.ItemReorderDao
import com.erpnext.pos.localSource.entities.ItemReorderEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.views.home.InventoryAlert
import com.erpnext.pos.views.home.InventoryAlertStatus
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class InventoryAlertRepository(
    private val itemDao: ItemDao,
    private val itemReorderDao: ItemReorderDao,
    private val apiService: APIService,
    private val sessionRefresher: SessionRefresher,
    private val networkMonitor: NetworkMonitor
) {

    data class ReorderInfo(
        val reorderLevel: Double?,
        val reorderQty: Double?
    )

    suspend fun loadAlerts(
        warehouseId: String,
        limit: Int
    ): List<InventoryAlert> {
        val items = itemDao.getAllItems()
            .filter { it.isStocked && !it.isDeleted }

        val itemCodes = items.map { it.itemCode }
        val canFetch = networkMonitor.isConnected.first() && sessionRefresher.ensureValidSession()

        val projectedQtyMap = if (canFetch) {
            runCatching {
                apiService.fetchBinsForItems(warehouseId, itemCodes)
                    .associate { it.itemCode to (it.projectedQty ?: it.actualQty) }
            }.onFailure {
                AppLogger.warn("InventoryAlertRepository: fetchBinsForItems failed", it)
            }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }

        val localReorderMap = itemReorderDao.getByItems(
            warehouseId = warehouseId,
            itemIds = itemCodes
        ).associate { it.itemId to ReorderInfo(it.reorderLevel, it.reorderQty) }

        val reorderMap = if (canFetch) {
            runCatching {
                val fetched = apiService.fetchItemReordersForItems(warehouseId, itemCodes)
                itemReorderDao.deleteByWarehouse(warehouseId)
                if (fetched.isNotEmpty()) {
                    val now = Clock.System.now().toEpochMilliseconds()
                    itemReorderDao.upsertAll(
                        fetched.map {
                            ItemReorderEntity(
                                itemId = it.itemCode,
                                warehouseId = warehouseId,
                                reorderLevel = it.reorderLevel,
                                reorderQty = it.reorderQty,
                                updatedAt = now,
                                companyId = "",
                                instanceId = "",
                                lastSyncedAt = now
                            )
                        }
                    )
                }
                fetched.associate { it.itemCode to ReorderInfo(it.reorderLevel, it.reorderQty) }
            }.onFailure {
                AppLogger.warn("InventoryAlertRepository: fetchItemReordersForItems failed", it)
            }.getOrElse { localReorderMap }
        } else {
            localReorderMap
        }

        val alerts = items.mapNotNull { item ->
            val qty = projectedQtyMap[item.itemCode] ?: item.actualQty
            val reorder = reorderMap[item.itemCode]
            val level = reorder?.reorderLevel
            val status = when {
                qty <= 0.0 -> InventoryAlertStatus.CRITICAL
                level != null && level > 0.0 && qty <= (level * 0.35) -> InventoryAlertStatus.CRITICAL
                level != null && qty <= level -> InventoryAlertStatus.LOW
                else -> null
            } ?: return@mapNotNull null

            InventoryAlert(
                itemCode = item.itemCode,
                itemName = item.name,
                qty = qty,
                status = status,
                reorderLevel = reorder?.reorderLevel,
                reorderQty = reorder?.reorderQty
            )
        }

        return alerts
            .sortedWith(
                compareBy<InventoryAlert> { it.status != InventoryAlertStatus.CRITICAL }
                    .thenBy { it.qty }
            )
            .take(limit)
    }
}
