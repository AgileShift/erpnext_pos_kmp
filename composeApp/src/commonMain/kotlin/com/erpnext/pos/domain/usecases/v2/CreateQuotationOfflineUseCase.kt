package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.CatalogRepository
import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.InventoryRepository
import com.erpnext.pos.data.repositories.v2.QuotationRepository
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.entities.v2.QuotationCustomerLinkEntity
import com.erpnext.pos.localSource.entities.v2.QuotationEntity
import com.erpnext.pos.localSource.entities.v2.QuotationItemEntity
import com.erpnext.pos.localSource.entities.v2.QuotationTaxEntity
import com.erpnext.pos.utils.view.DateTimeProvider
import kotlin.time.ExperimentalTime

data class CreateQuotationOfflineInput(
    val instanceId: String,
    val companyId: String,
    val customerId: String,
    val customerName: String,
    val territoryId: String,
    val currency: String,
    val priceList: String,
    val warehouseId: String,
    val validUntil: String? = null,
    val contactId: String? = null,
    val addressId: String? = null,
    val items: List<CreateQuotationItemInput>,
    val taxes: List<CreateQuotationTaxInput> = emptyList()
)

data class CreateQuotationItemInput(
    val itemId: String,
    val itemName: String,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val warehouseId: String
)

data class CreateQuotationTaxInput(
    val chargeType: String,
    val accountHead: String,
    val rate: Double,
    val taxAmount: Double
)

@OptIn(ExperimentalTime::class)
class CreateQuotationOfflineUseCase(
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    private val quotationRepository: QuotationRepository,
    private val idGenerator: UUIDGenerator
) {

    suspend operator fun invoke(input: CreateQuotationOfflineInput): String {
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

        val localQuotationId = "LOCAL-${idGenerator.newId()}"
        val transactionDate = DateTimeProvider.todayDate()

        val netTotal = input.items.sumOf { it.qty * it.rate }
        val totalTaxes = input.taxes.sumOf { it.taxAmount }
        val grandTotal = netTotal + totalTaxes

        val quotation = QuotationEntity(
            quotationId = localQuotationId,
            transactionDate = transactionDate,
            validUntil = input.validUntil,
            company = input.companyId,
            partyName = input.customerId,
            customerName = input.customerName,
            territory = input.territoryId,
            status = "Draft",
            priceListCurrency = input.currency,
            sellingPriceList = input.priceList,
            netTotal = netTotal,
            grandTotal = grandTotal,
            syncStatus = SyncStatus.PENDING
        ).apply {
            instanceId = input.instanceId
            companyId = input.companyId
        }

        val itemEntities = input.items.mapIndexed { index, line ->
            QuotationItemEntity(
                quotationId = localQuotationId,
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

        val taxEntities = input.taxes.map { line ->
            QuotationTaxEntity(
                quotationId = localQuotationId,
                chargeType = line.chargeType,
                accountHead = line.accountHead,
                rate = line.rate,
                taxAmount = line.taxAmount
            ).apply {
                instanceId = input.instanceId
                companyId = input.companyId
            }
        }

        val customerLinks = listOf(
            QuotationCustomerLinkEntity(
                quotationId = localQuotationId,
                partyName = input.customerId,
                customerName = input.customerName,
                contactId = input.contactId,
                addressId = input.addressId
            ).apply {
                instanceId = input.instanceId
                companyId = input.companyId
            }
        )

        quotationRepository.insertQuotationWithDetails(
            quotation = quotation,
            items = itemEntities,
            taxes = taxEntities,
            customerLinks = customerLinks
        )

        return localQuotationId
    }
}
