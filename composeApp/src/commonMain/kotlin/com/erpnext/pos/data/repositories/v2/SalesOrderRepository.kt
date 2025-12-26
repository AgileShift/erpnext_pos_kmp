package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.PendingSync
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.SalesOrderDao
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderCreateDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderItemCreateDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SalesOrderRepository(
    private val salesOrderDao: SalesOrderDao,
    private val customerRepository: CustomerRepository
) {

    suspend fun pull(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<PendingSync<SalesOrderCreateDto>> {
        return buildPendingCreatePayloads(ctx.instanceId, ctx.companyId)
    }

    suspend fun insertSalesOrderWithItems(
        order: SalesOrderEntity,
        items: List<SalesOrderItemEntity>
    ) {
        salesOrderDao.insertSalesOrderWithItems(order, items)
    }

    suspend fun getPendingSalesOrdersWithItems(
        instanceId: String,
        companyId: String
    ) = salesOrderDao.getPendingSalesOrdersWithItems(instanceId, companyId)

    @OptIn(ExperimentalTime::class)
    suspend fun markSynced(
        instanceId: String,
        companyId: String,
        salesOrderId: String,
        remoteName: String?,
        remoteModified: String?
    ) {
        val now = Clock.System.now().epochSeconds
        salesOrderDao.updateSyncStatus(
            instanceId,
            companyId,
            salesOrderId,
            syncStatus = "SYNCED",
            lastSyncedAt = now,
            updatedAt = now
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markFailed(
        instanceId: String,
        companyId: String,
        salesOrderId: String
    ) {
        val now = Clock.System.now().epochSeconds
        salesOrderDao.updateSyncStatus(
            instanceId,
            companyId,
            salesOrderId,
            syncStatus = "FAILED",
            lastSyncedAt = null,
            updatedAt = now
        )
    }

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<PendingSync<SalesOrderCreateDto>> {
        return salesOrderDao.getPendingSalesOrdersWithItems(instanceId, companyId).map { snapshot ->
            val customerId = customerRepository.resolveRemoteCustomerId(
                instanceId,
                companyId,
                snapshot.salesOrder.customerId
            )
            PendingSync(
                localId = snapshot.salesOrder.salesOrderId,
                payload = SalesOrderCreateDto(
                    company = snapshot.salesOrder.company,
                    transactionDate = snapshot.salesOrder.transactionDate,
                    customerId = customerId,
                    deliveryDate = snapshot.salesOrder.deliveryDate,
                    customerName = snapshot.salesOrder.customerName,
                    territory = snapshot.salesOrder.territory,
                    sellingPriceList = snapshot.salesOrder.sellingPriceList,
                    currency = snapshot.salesOrder.priceListCurrency,
                    items = snapshot.items.map { item ->
                        SalesOrderItemCreateDto(
                            itemCode = item.itemCode,
                            qty = item.qty,
                            rate = item.rate,
                            uom = item.uom,
                            warehouse = item.warehouse
                        )
                    }
                )
            )
        }
    }
}
