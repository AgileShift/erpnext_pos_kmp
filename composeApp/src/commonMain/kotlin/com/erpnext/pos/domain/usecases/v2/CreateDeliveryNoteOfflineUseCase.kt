package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.CatalogRepository
import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.DeliveryNoteRepository
import com.erpnext.pos.data.repositories.v2.InventoryRepository
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteItemEntity
import com.erpnext.pos.localSource.entities.v2.DeliveryNoteLinkEntity
import com.erpnext.pos.utils.view.DateTimeProvider
import kotlin.time.ExperimentalTime

data class CreateDeliveryNoteOfflineInput(
    val instanceId: String,
    val companyId: String,
    val customerId: String,
    val customerName: String,
    val territoryId: String,
    val warehouseId: String,
    val salesOrderId: String? = null,
    val salesInvoiceId: String? = null,
    val items: List<CreateDeliveryNoteItemInput>
)

data class CreateDeliveryNoteItemInput(
    val itemId: String,
    val itemName: String,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val warehouseId: String
)

@OptIn(ExperimentalTime::class)
class CreateDeliveryNoteOfflineUseCase(
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    private val deliveryNoteRepository: DeliveryNoteRepository,
    private val idGenerator: UUIDGenerator
) {

    suspend operator fun invoke(input: CreateDeliveryNoteOfflineInput): String {
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

        val localDeliveryNoteId = "LOCAL-${idGenerator.newId()}"
        val postingDate = DateTimeProvider.todayDate()

        val note = DeliveryNoteEntity(
            deliveryNoteId = localDeliveryNoteId,
            postingDate = postingDate,
            company = input.companyId,
            customerId = input.customerId,
            customerName = input.customerName,
            territory = input.territoryId,
            status = "Draft",
            setWarehouse = input.warehouseId,
            syncStatus = SyncStatus.PENDING
        ).apply {
            instanceId = input.instanceId
            companyId = input.companyId
        }

        val itemEntities = input.items.mapIndexed { index, line ->
            DeliveryNoteItemEntity(
                deliveryNoteId = localDeliveryNoteId,
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

        val linkEntities = listOfNotNull(
            if (input.salesOrderId != null || input.salesInvoiceId != null) {
                DeliveryNoteLinkEntity(
                    deliveryNoteId = localDeliveryNoteId,
                    salesOrderId = input.salesOrderId,
                    salesInvoiceId = input.salesInvoiceId
                ).apply {
                    instanceId = input.instanceId
                    companyId = input.companyId
                }
            } else {
                null
            }
        )

        deliveryNoteRepository.insertDeliveryNoteWithDetails(
            note = note,
            items = itemEntities,
            links = linkEntities
        )

        input.items.forEach { line ->
            val item = itemsById.getValue(line.itemId)
            if (item.isStockItem) {
                inventoryRepository.adjustStockLocal(
                    instanceId = input.instanceId,
                    companyId = input.companyId,
                    warehouseId = input.warehouseId,
                    itemId = line.itemId,
                    deltaQty = -line.qty
                )
            }
        }

        return localDeliveryNoteId
    }
}
