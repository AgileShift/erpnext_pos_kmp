package com.erpnext.pos.data.repositories

import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.views.home.InventoryAlert
import com.erpnext.pos.views.home.InventoryAlertStatus
import kotlinx.coroutines.flow.first

class InventoryAlertRepository(
    private val itemDao: ItemDao,
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
            apiService.fetchBinsForItems(warehouseId, itemCodes)
                .associate { it.itemCode to (it.projectedQty ?: it.actualQty) }
        } else {
            emptyMap()
        }

        val reorderMap = if (canFetch) {
            apiService.fetchItemReordersForItems(warehouseId, itemCodes)
                .associate { it.itemCode to ReorderInfo(it.reorderLevel, it.reorderQty) }
        } else {
            emptyMap()
        }

        val alerts = items.mapNotNull { item ->
            val qty = projectedQtyMap[item.itemCode] ?: item.actualQty
            val reorder = reorderMap[item.itemCode]
            val status = when {
                qty <= 0.0 -> InventoryAlertStatus.CRITICAL
                reorder?.reorderLevel != null && qty <= reorder.reorderLevel -> InventoryAlertStatus.LOW
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
