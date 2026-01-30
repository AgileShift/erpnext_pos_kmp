package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.SalesInvoiceRepository
import com.erpnext.pos.data.repositories.v2.CatalogRepository
import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.InventoryRepository
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.utils.view.DateTimeProvider
import kotlin.time.ExperimentalTime

data class CreateInvoiceOfflineInput(
    val instanceId: String,
    val companyId: String,
    val customerId: String,
    val customerName: String,
    val territoryId: String,
    val currency: String,
    val priceList: String,
    val warehouseId: String,
    val salesPersonId: String,
    val items: List<CreateInvoiceItemInput>,
    val payments: List<CreateInvoicePaymentInput>
)

data class CreateInvoiceItemInput(
    val itemId: String,
    val itemName: String,
    val qty: Double,
    val uom: String,
    val rate: Double,
    val warehouseId: String
)

data class CreateInvoicePaymentInput(
    val modeOfPayment: String,
    val amount: Double
)

/**
 * Mantra:
 *  Quiero crear una factura ahora, sin internet, y que el sistema se encargue luego.
 */
@OptIn(ExperimentalTime::class)
class CreateInvoiceOfflineUseCase(
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    private val salesInvoiceRepository: SalesInvoiceRepository,
    private val idGenerator: UUIDGenerator
) {

    suspend operator fun invoke(input: CreateInvoiceOfflineInput): String {

        // 1️⃣ Validar Customer
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

        // 2️⃣ Cargar catálogo y stock
        val itemsById = catalogRepository
            .getItems(input.instanceId, input.companyId)
            .associateBy { it.itemId }

        val stockByItem: Map<String, Float> =
            inventoryRepository.getStockByWarehouse(
                input.instanceId,
                input.companyId,
                input.warehouseId
            )

        // 3️⃣ Validaciones por ítem
        input.items.forEach { line ->
            val item = requireNotNull(itemsById[line.itemId]) {
                "Item not found: ${line.itemId}"
            }

            require(line.qty > 0f) { "Invalid qty for item ${line.itemId}" }
            require(line.rate >= 0f) { "Invalid rate for item ${line.itemId}" }

            if (item.isStockItem && !item.allowNegativeStock) {
                val stock = stockByItem[line.itemId] ?: 0f
                require(stock + 0.0001f >= line.qty) {
                    "Insufficient stock for item ${line.itemId} (stock=$stock, qty=${line.qty})"
                }
            }
        }

        // 4️⃣ Construir SalesInvoiceEntity
        val localInvoiceId = "LOCAL-${idGenerator.newId()}"

        val postingDate = DateTimeProvider.todayDate()
        val postingTime = DateTimeProvider.currentTime()
        val dueDate = postingDate // TODO: payment terms

        val total = input.items.sumOf { it.qty * it.rate }
        val paidAmount = input.payments.sumOf { it.amount }.coerceAtLeast(0.0)
        val outstanding = (total - paidAmount).coerceAtLeast(0.0)
        val status = when {
            outstanding <= 0.0001 -> "Paid"
            paidAmount <= 0.0001 -> "Unpaid"
            else -> "Partly Paid"
        }
        val isPosInvoice = outstanding <= 0.0001

        val invoice = SalesInvoiceEntity(
            invoiceId = localInvoiceId,
            territoryId = input.territoryId,
            namingSeries = "POS-OFFLINE",
            docStatus = "Draft",
            status = status,
            postingDate = postingDate,
            postingTime = postingTime,
            customerId = input.customerId,
            customerName = input.customerName,
            company = input.companyId,
            territory = input.territoryId,
            salesPerson = input.salesPersonId,
            currency = input.currency,
            conversionRate = 1f,
            total = total,
            totalTaxesAndCharges = 0f,
            grandTotal = total,
            roundedTotal = null,
            outstandingAmount = outstanding,
            isPos = isPosInvoice,
            dueDate = dueDate,
            paymentTerms = null,
            updateStock = true,
            setWarehouse = input.warehouseId,
            priceList = input.priceList,
            disableRoundedTotal = false,
            syncStatus = SyncStatus.PENDING,
        ).apply {
            this.instanceId = input.instanceId
            this.companyId = input.companyId
        }

        // 5️⃣ Items
        val itemEntities = input.items.mapIndexed { index, line ->
            SalesInvoiceItemEntity(
                invoiceId = localInvoiceId,
                rowId = index,
                itemCode = line.itemId,
                itemName = line.itemName,
                qty = line.qty,
                uom = line.uom,
                rate = line.rate,
                amount = line.qty * line.rate,
                warehouse = line.warehouseId,
                priceListRate = line.rate
            ).apply {
                this.instanceId = input.instanceId
                this.companyId = input.companyId
            }
        }

        val paymentEntities: List<SalesInvoicePaymentEntity> =
            input.payments.map {
                SalesInvoicePaymentEntity(
                    paymentId = "PAY-${UUIDGenerator().newId()}",
                    invoiceId = localInvoiceId,
                    paymentMode = it.modeOfPayment,
                    amount = it.amount
                ).apply {
                    instanceId = input.instanceId
                    companyId = input.companyId
                }
            }

        // 6️⃣ Persistir (en transacción)
        salesInvoiceRepository.insertInvoiceWithItemsAndPayments(
            invoice = invoice,
            items = itemEntities,
            payments = paymentEntities
        )

        // 7️⃣ Ajuste de stock local (opcional pero recomendado)
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

        return localInvoiceId
    }
}
