package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
import com.erpnext.pos.localSource.relations.v2.SalesOrderWithItems

@Dao
interface SalesOrderDao {

    @Query(
        """
        SELECT * FROM sales_orders
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
    """
    )
    suspend fun getSalesOrders(
        instanceId: String,
        companyId: String
    ): List<SalesOrderEntity>

    @Query(
        """
        SELECT * FROM sales_orders
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND salesOrderId = :salesOrderId
          AND is_deleted = 0
        LIMIT 1
    """
    )
    suspend fun getSalesOrder(
        instanceId: String,
        companyId: String,
        salesOrderId: String
    ): SalesOrderEntity?

    @Transaction
    suspend fun getSalesOrderWithItems(
        instanceId: String,
        companyId: String,
        salesOrderId: String
    ): SalesOrderWithItems? {
        val order = getSalesOrder(instanceId, companyId, salesOrderId) ?: return null
        return SalesOrderWithItems(
            salesOrder = order,
            items = getSalesOrderItems(instanceId, companyId, salesOrderId)
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesOrder(order: SalesOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SalesOrderItemEntity>)

    @Transaction
    suspend fun insertSalesOrderWithItems(
        order: SalesOrderEntity,
        items: List<SalesOrderItemEntity>
    ) {
        insertSalesOrder(order)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }

    @Transaction
    suspend fun getPendingSalesOrdersWithItems(
        instanceId: String,
        companyId: String
    ): List<SalesOrderWithItems> {
        return getPendingSalesOrders(instanceId, companyId).map { order ->
            SalesOrderWithItems(
                salesOrder = order,
                items = getSalesOrderItems(instanceId, companyId, order.salesOrderId)
            )
        }
    }

    @Query(
        """
        SELECT * FROM sales_order_items
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND salesOrderId = :salesOrderId
    """
    )
    suspend fun getSalesOrderItems(
        instanceId: String,
        companyId: String,
        salesOrderId: String
    ): List<SalesOrderItemEntity>

    @Query(
        """
        SELECT * FROM sales_orders
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
          AND syncStatus = 'PENDING'
    """
    )
    suspend fun getPendingSalesOrders(
        instanceId: String,
        companyId: String
    ): List<SalesOrderEntity>

    @Query(
        """
      UPDATE sales_orders
      SET syncStatus = :syncStatus,
          lastSyncedAt = :lastSyncedAt,
          updated_at = :updatedAt
      WHERE instanceId = :instanceId
        AND companyId = :companyId
        AND salesOrderId = :salesOrderId
    """
    )
    suspend fun updateSyncStatus(
        instanceId: String,
        companyId: String,
        salesOrderId: String,
        syncStatus: String,
        lastSyncedAt: Long?,
        updatedAt: Long
    )
}
