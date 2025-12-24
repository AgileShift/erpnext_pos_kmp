package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.CatalogRepository
import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.InventoryRepository
import com.erpnext.pos.data.repositories.v2.SalesOrderRepository
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.entities.v2.SalesOrderEntity
import com.erpnext.pos.localSource.entities.v2.SalesOrderItemEntity
import com.erpnext.pos.utils.view.DateTimeProvider
import kotlin.time.ExperimentalTime

data class CreateSalesOrderOfflineInput(
    val instanceId: String,
    val companyId: String,
    val customerId: String,
    val customerName: String,
    val territoryId: String,
    val currency: String,
    val priceList: String,
    val warehouseId: String,
    val deliveryDate: String? = null,
    val items: List<CreateSalesOrderItemInput>
)

data class CreateSalesOrderItemInput(
    val itemId: String,
    val itemName: String,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val warehouseId: String
)

@OptIn(ExperimentalTime::class)
class CreateSalesOrderOfflineUseCase(
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val idGenerator: UUIDGenerator
) {

    suspend operator fun invoke(input: CreateSalesOrderOfflineInput): String {
        val customer = requireNotNull(
            customerRepository.getCustomerDetail(
                input.instanceId,
                input.companyId,
                input.customerId
            )
        ) { "Customer not found: ${input.customerId}" }

        require(!customer.customer.disabled) {
            "Customer is disabled"
        }

        val itemsById = catalogRepository
            .getItems(input.instanceId, input.companyId)
            .associateBy { it.itemId }

        val stockByItem = inventoryRepository.getStockByWarehouse(
            input.instanceId,
            input.companyId,
            input.warehouseId
        )

        input.items.forEach { line ->
            val item = requireNotNull(itemsById[line.itemId]) {
                "Item not found: ${line.itemId}"
            }
            require(!item.disabled) { "Item is disabled: ${line.itemId}" }
            require(line.qty > 0f) { "Invalid qty for item ${line.itemId}" }
            require(line.rate >= 0f) { "Invalid rate for item ${line.itemId}" }

            if (item.isStockItem && !item.allowNegativeStock) {
                val stock = stockByItem[line.itemId] ?: 0f
                require(stock + 0.0001f >= line.qty) {
                    "Insufficient stock for item ${line.itemId} (stock=$stock, qty=${line.qty})"
                }
            }
        }

        val localSalesOrderId = "LOCAL-${idGenerator.newId()}"
        val transactionDate = DateTimeProvider.todayDate()

        val netTotal = input.items.sumOf { it.qty * it.rate }

        val order = SalesOrderEntity(
            salesOrderId = localSalesOrderId,
            transactionDate = transactionDate,
            deliveryDate = input.deliveryDate,
            company = input.companyId,
            customerId = input.customerId,
            customerName = input.customerName,
            territory = input.territoryId,
            status = "Draft",
            deliveryStatus = "Not Delivered",
            billingStatus = "Not Billed",
            priceListCurrency = input.currency,
            sellingPriceList = input.priceList,
            netTotal = netTotal,
            grandTotal = netTotal,
            syncStatus = SyncStatus.PENDING
        ).apply {
            instanceId = input.instanceId
            companyId = input.companyId
        }

        val itemEntities = input.items.mapIndexed { index, line ->
            SalesOrderItemEntity(
                salesOrderId = localSalesOrderId,
                rowId = index,
                itemCode = line.itemId,
                itemName = line.itemName,
                qty = line.qty,
                uom = line.uom,
                rate = line.rate,
                amount = line.qty * line.rate,
                warehouse = line.warehouseId
            ).apply {
                instanceId = input.instanceId
                companyId = input.companyId
            }
        }

        salesOrderRepository.insertSalesOrderWithItems(order, itemEntities)

        return localSalesOrderId
    }
}
