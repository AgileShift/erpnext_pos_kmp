package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
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
    suspend fun getSalesOrderWithItems(
        instanceId: String,
        companyId: String,
        salesOrderId: String
    ): SalesOrderWithItems?
}
