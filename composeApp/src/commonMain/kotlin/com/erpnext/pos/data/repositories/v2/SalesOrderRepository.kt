package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.SalesOrderDao
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderCreateDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderItemCreateDto

class SalesOrderRepository(
    private val salesOrderDao: SalesOrderDao
) {

    suspend fun pull(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<SalesOrderCreateDto> {
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

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<SalesOrderCreateDto> {
        return salesOrderDao.getPendingSalesOrdersWithItems(instanceId, companyId).map { snapshot ->
            SalesOrderCreateDto(
                company = snapshot.order.company,
                transactionDate = snapshot.order.transactionDate,
                customerId = snapshot.order.customerId,
                deliveryDate = snapshot.order.deliveryDate,
                customerName = snapshot.order.customerName,
                territory = snapshot.order.territory,
                sellingPriceList = snapshot.order.sellingPriceList,
                currency = snapshot.order.priceListCurrency,
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
        }
    }
}
