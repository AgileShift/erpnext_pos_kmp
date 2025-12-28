package com.erpnext.pos.views.billing

import CartItem
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.usecases.FetchBillingProductsWithPriceUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchDeliveryChargesUseCase
import com.erpnext.pos.domain.usecases.FetchPaymentTermsUseCase
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentScheduleDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryReferenceCreateDto
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.DateTimeProvider
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

class BillingViewModel(
    val customersUseCase: FetchCustomersUseCase,
    val itemsUseCase: FetchBillingProductsWithPriceUseCase,
    val contextProvider: CashBoxManager,
    private val invoiceRepository: SalesInvoiceRepository,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val paymentTermsUseCase: FetchPaymentTermsUseCase,
    private val deliveryChargesUseCase: FetchDeliveryChargesUseCase,
    private val navManager: NavigationManager,
    private val api: APIService,
) : BaseViewModel() {

    private val _state: MutableStateFlow<BillingState> = MutableStateFlow(BillingState.Loading)
    val state = _state.asStateFlow()

    private var customers: List<CustomerBO> = emptyList()
    private var products: List<ItemBO> = emptyList()

    private enum class DiscountSource {
        None,
        Manual,
        Code
    }

    private data class DiscountInfo(
        val amount: Double,
        val percent: Double?,
        val source: DiscountSource
    )

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        executeUseCase(action = {
            val context = contextProvider.requireContext()
            val paymentTerms = runCatching {
                paymentTermsUseCase.invoke(Unit)
            }.getOrElse { emptyList() }
            val deliveryCharges = runCatching {
                deliveryChargesUseCase.invoke(Unit)
            }.getOrElse { emptyList() }

            customersUseCase.invoke(null).collectLatest { c ->
                customers = c
                itemsUseCase.invoke(null).collectLatest { i ->
                    products = i.filter { it.price > 0.0 && it.actualQty > 0.0 }
                    val currency = context.currency.ifBlank { "USD" }
                    val modeTypes = runCatching { modeOfPaymentDao.getAllModes() }
                        .getOrElse { emptyList() }
                        .associateBy { it.modeOfPayment }
                    val paymentModes = context.paymentModes.ifEmpty {
                        modeOfPaymentDao.getAll(context.profileName).map { mode ->
                            POSPaymentModeOption(
                                name = mode.name,
                                modeOfPayment = mode.modeOfPayment,
                                type = modeTypes[mode.modeOfPayment]?.type,
                            )
                        }
                    }.ifEmpty {
                        listOf(
                            POSPaymentModeOption(
                                name = "Cash",
                                modeOfPayment = "Cash",
                                type = "Cash"
                            ),
                            POSPaymentModeOption(
                                name = "Card",
                                modeOfPayment = "Card",
                                type = "Card"
                            ),
                            POSPaymentModeOption(
                                name = "Transfer",
                                modeOfPayment = "Transfer",
                                type = "Bank"
                            )
                        )
                    }
                    val exchangeRateByCurrency = buildMap {
                        val baseCurrency = currency.uppercase()
                        put(baseCurrency, 1.0)
                        if (!baseCurrency.equals("USD", ignoreCase = true)) {
                            put("USD", context.exchangeRate)
                        }
                    }
                    _state.update {
                        BillingState.Success(
                            customers = customers,
                            productSearchResults = products,
                            currency = currency,
                            paymentModes = paymentModes,
                            allowedCurrencies = context.allowedCurrencies,
                            exchangeRate = contextProvider.getContext()?.exchangeRate ?: 1.0,
                            paymentTerms = paymentTerms,
                            deliveryCharges = deliveryCharges,
                            exchangeRateByCurrency = exchangeRateByCurrency
                        )
                    }
                }
            }
        }, exceptionHandler = {
            _state.value = BillingState.Error(it.message ?: "Unknown error")
        })
    }

    fun onCustomerSearchQueryChange(query: String) {
        val current = _state.value as? BillingState.Success ?: return
        val filtered = if (query.isBlank()) {
            customers
        } else {
            customers.filter {
                it.customerName.contains(query, ignoreCase = true) || it.name.contains(
                    query, ignoreCase = true
                )
            }
        }
        val updatedSelection = current.selectedCustomer?.takeIf {
            it.customerName.equals(query, ignoreCase = true)
        }
        _state.update {
            current.copy(
                customerSearchQuery = query,
                customers = filtered,
                selectedCustomer = updatedSelection
            )
        }
    }

    fun onCustomerSelected(customer: CustomerBO) {
        val current = _state.value as? BillingState.Success ?: return
        _state.update {
            current.copy(
                selectedCustomer = customer, customerSearchQuery = customer.customerName
            )
        }
    }

    fun onProductSearchQueryChange(query: String) {
        val current = _state.value as? BillingState.Success ?: return
        val filtered = if (query.isBlank()) {
            products
        } else {
            products.filter {
                (it.name.contains(query, ignoreCase = true) || it.itemCode.contains(
                    query,
                    ignoreCase = true
                ))
            }
        }
        _state.update {
            current.copy(
                productSearchQuery = query, productSearchResults = filtered
            )
        }
    }

    fun onProductAdded(item: ItemBO) {
        val current = _state.value as? BillingState.Success ?: return
        val existing = current.cartItems.firstOrNull { it.itemCode == item.itemCode }
        val exchangeRate = current.exchangeRate
        val updated = if (existing == null) {
            current.cartItems + CartItem(
                itemCode = item.itemCode,
                name = item.name,
                currency = item.currency?.toCurrencySymbol()
                    ?: current.currency?.toCurrencySymbol(),
                quantity = 1.0,
                price = if (item.currency.equals(current.currency)) item.price else item.price * (exchangeRate
                    ?: 0.0)
            )
        } else {
            current.cartItems.map {
                if (it.itemCode == item.itemCode) it.copy(quantity = it.quantity + 1) else it
            }
        }
        _state.update { recalculateTotals(current.copy(cartItems = updated)) }
    }

    fun onQuantityChanged(itemCode: String, newQuantity: Double) {
        val current = _state.value as? BillingState.Success ?: return
        val updated = current.cartItems.map {
            if (it.itemCode == itemCode) it.copy(quantity = newQuantity.coerceAtLeast(0.0)) else it
        }.filter { it.quantity > 0.0 }

        _state.update { recalculateTotals(current.copy(cartItems = updated)) }
    }

    fun onRemoveItem(itemCode: String) {
        val current = _state.value as? BillingState.Success ?: return
        val updated = current.cartItems.filterNot { it.itemCode == itemCode }
        _state.update { recalculateTotals(current.copy(cartItems = updated)) }
    }

    fun onAddPaymentLine(line: PaymentLine) {
        val current = _state.value as? BillingState.Success ?: return
        val modeOption = current.paymentModes.firstOrNull {
            it.modeOfPayment == line.modeOfPayment
        }
        if (requiresReference(modeOption) && line.referenceNumber.isNullOrBlank()) {
            _state.update {
                current.copy(
                    paymentErrorMessage = "Reference number is required for ${line.modeOfPayment} payments."
                )
            }
            return
        }

        val updated = current.paymentLines + line.toBaseAmount()
        _state.update { current.withPaymentLines(updated) }
    }

    fun onUpdatePaymentLine(index: Int, line: PaymentLine) {
        val current = _state.value as? BillingState.Success ?: return
        if (index !in current.paymentLines.indices) return
        val updated = current.paymentLines.mapIndexed { idx, existing ->
            if (idx == index) line.toBaseAmount() else existing
        }
        _state.update { current.withPaymentLines(updated) }
    }

    fun onRemovePaymentLine(index: Int) {
        val current = _state.value as? BillingState.Success ?: return
        if (index !in current.paymentLines.indices) return
        val updated = current.paymentLines.filterIndexed { idx, _ -> idx != index }
        _state.update { current.withPaymentLines(updated) }
    }

    fun onCreditSaleChanged(isCreditSale: Boolean) {
        val current = _state.value as? BillingState.Success ?: return
        if (isCreditSale && current.paymentTerms.isEmpty()) {
            return
        }
        _state.update {
            // A sale is treated as credit when credit mode is enabled and payment terms are available.
            // Payment terms are only enabled once ERPNext provides templates for the POS profile.
            current.copy(
                isCreditSale = isCreditSale,
                selectedPaymentTerm = if (isCreditSale) current.selectedPaymentTerm else null,
                paymentLines = if (isCreditSale) emptyList() else current.paymentLines,
                paymentErrorMessage = null
            ).recalculatePaymentTotals()
        }
    }

    fun onPaymentTermSelected(term: com.erpnext.pos.domain.models.PaymentTermBO?) {
        val current = _state.value as? BillingState.Success ?: return
        _state.update {
            current.copy(
                selectedPaymentTerm = term
            )
        }
    }

    fun onDiscountCodeChanged(code: String) {
        val current = _state.value as? BillingState.Success ?: return
        _state.update {
            recalculateTotals(
                current.copy(
                    discountCode = code,
                    manualDiscountAmount = if (code.isNotBlank()) 0.0 else current.manualDiscountAmount,
                    manualDiscountPercent = if (code.isNotBlank()) 0.0 else current.manualDiscountPercent
                )
            )
        }
    }

    fun onManualDiscountAmountChanged(value: String) {
        val current = _state.value as? BillingState.Success ?: return
        val amount = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        _state.update {
            recalculateTotals(
                current.copy(
                    manualDiscountAmount = amount,
                    manualDiscountPercent = if (amount > 0.0) 0.0 else current.manualDiscountPercent
                )
            )
        }
    }

    fun onManualDiscountPercentChanged(value: String) {
        val current = _state.value as? BillingState.Success ?: return
        val percent = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        _state.update {
            recalculateTotals(
                current.copy(
                    manualDiscountPercent = percent,
                    manualDiscountAmount = if (percent > 0.0) 0.0 else current.manualDiscountAmount
                )
            )
        }
    }

    fun onDeliveryChargeSelected(charge: DeliveryChargeBO?) {
        val current = _state.value as? BillingState.Success ?: return
        val amount = charge?.defaultRate?.coerceAtLeast(0.0) ?: 0.0
        _state.update {
            recalculateTotals(
                current.copy(
                    selectedDeliveryCharge = charge,
                    shippingAmount = amount
                )
            )
        }
    }

    fun onPaymentCurrencySelected(currency: String) {
        val current = _state.value as? BillingState.Success ?: return
        val baseCurrency = current.currency?.takeIf { it.isNotBlank() } ?: return
        val normalized = currency.trim().uppercase()
        if (normalized.equals(baseCurrency, ignoreCase = true)) {
            if (current.exchangeRateByCurrency[normalized] == 1.0) return
            _state.update {
                current.copy(
                    exchangeRateByCurrency = current.exchangeRateByCurrency + (normalized to 1.0)
                )
            }
            return
        }
        if (current.exchangeRateByCurrency.containsKey(normalized)) return

        executeUseCase(
            action = {
                val directRate = api.getExchangeRate(
                    fromCurrency = normalized,
                    toCurrency = baseCurrency
                )
                val reverseRate = api.getExchangeRate(
                    fromCurrency = baseCurrency,
                    toCurrency = normalized
                )?.takeIf { it > 0.0 }?.let { 1 / it }
                val rate = directRate ?: reverseRate
                if (rate != null && rate > 0.0) {
                    _state.update {
                        current.copy(
                            exchangeRateByCurrency = current.exchangeRateByCurrency + (normalized to rate)
                        )
                    }
                }
            },
            exceptionHandler = { }
        )
    }

    fun onClearSuccessMessage() {
        val current = _state.value as? BillingState.Success ?: return
        if (current.successMessage == null) return
        _state.update { current.copy(successMessage = null) }
    }

    fun onFinalizeSale() {
        val current = _state.value as? BillingState.Success ?: return
        val customer = current.selectedCustomer ?: run {
            _state.update { BillingState.Error("Select a customer before finalizing the sale.") }
            return
        }
        if (current.cartItems.isEmpty()) {
            _state.update { BillingState.Error("Add at least one item to the cart.") }
            return
        }
        if (!current.isCreditSale && current.paidAmountBase < current.total) {
            _state.update { BillingState.Error("Paid amount must cover the total before finalizing the sale.") }
            return
        }
        if (current.isCreditSale && current.selectedPaymentTerm == null) {
            _state.update { BillingState.Error("Select a payment term to finalize a credit sale.") }
            return
        }
        if (current.isCreditSale && current.paymentLines.isNotEmpty()) {
            _state.update { BillingState.Error("Credit sales cannot include payment lines.") }
            return
        }

        val context = contextProvider.getContext() ?: error("POS context not initialized.")

        _state.update { current.copy() }

        executeUseCase(action = {
            val subtotal = current.cartItems.sumOf { it.price * it.quantity }
            val discountInfo = resolveDiscountInfo(current, subtotal)
            val deliveryCharge = current.selectedDeliveryCharge
            val shippingAmount = deliveryCharge?.defaultRate?.coerceAtLeast(0.0) ?: 0.0
            val discountPercent = discountInfo.percent?.takeIf { it > 0.0 }
            val discountAmount = discountInfo.amount
            val isCreditSale = current.isCreditSale
            val paymentLines = if (isCreditSale) emptyList() else current.paymentLines
            val items = current.cartItems.map { cart ->
                SalesInvoiceItemDto(
                    itemCode = cart.itemCode,
                    itemName = cart.name,
                    qty = cart.quantity,
                    rate = cart.price,
                    amount = cart.quantity * cart.price,
                    discountPercentage = discountPercent,
                    warehouse = context.warehouse,
                    incomeAccount = context.incomeAccount
                )
            }.toMutableList()

            if (discountPercent == null && discountAmount > 0.0) {
                items.add(
                    SalesInvoiceItemDto(
                        itemCode = DISCOUNT_ITEM_CODE,
                        itemName = "Discount",
                        qty = 1.0,
                        rate = -discountAmount,
                        amount = -discountAmount,
                        warehouse = context.warehouse,
                        incomeAccount = context.incomeAccount
                    )
                )
            }

            val total = (subtotal - discountAmount + shippingAmount).coerceAtLeast(0.0)
            val payments = if (isCreditSale) {
                emptyList()
            } else {
                paymentLines.map { line ->
                    SalesInvoicePaymentDto(
                        modeOfPayment = line.modeOfPayment,
                        amount = line.baseAmount,
                        paymentReference = line.referenceNumber
                    )
                }
            }
            // ERPNext sales cycle:
            // - Credit sales must include payment_terms + payment_schedule with due_date derived from the term.
            // - Paid amount stays at 0 and status remains "Unpaid" until a Payment Entry is created.
            val paidAmount = if (isCreditSale) 0.0 else paymentLines.sumOf { it.baseAmount }
            val outstandingAmount = (total - paidAmount).coerceAtLeast(0.0)
            val status = if (isCreditSale || outstandingAmount > 0.0) "Unpaid" else "Paid"
            val paymentMetadata = buildList {
                addAll(
                    paymentLines.mapNotNull { line ->
                        if (line.currency.equals(context.currency, ignoreCase = true)) {
                            null
                        } else {
                            "Payment currency (${line.modeOfPayment}): ${line.currency}, Exchange rate: ${line.exchangeRate}"
                        }
                    }
                )
                addAll(
                    paymentLines.mapNotNull { line ->
                        line.referenceNumber?.takeIf { it.isNotBlank() }?.let {
                            "Reference (${line.modeOfPayment}): $it"
                        }
                    }
                )
                if (current.discountCode.isNotBlank()) {
                    add("Discount code: ${current.discountCode}")
                }
                deliveryCharge?.let { charge ->
                    add("Delivery charge: ${charge.label} ($shippingAmount)")
                }
            }.joinToString(separator = "; ").takeIf { it.isNotBlank() }
            val postingDate = DateTimeProvider.todayDate()
            val dueDate = if (isCreditSale) {
                val term = current.selectedPaymentTerm
                    ?: error("Payment term is required for credit sales.")
                val withMonths = DateTimeProvider.addMonths(postingDate, term.creditMonths ?: 0)
                DateTimeProvider.addDays(withMonths, term.creditDays ?: 0)
            } else {
                postingDate
            }
            val paymentSchedule = if (isCreditSale) {
                val term = current.selectedPaymentTerm
                    ?: error("Payment term is required for credit sales.")
                listOf(
                    SalesInvoicePaymentScheduleDto(
                        paymentTerm = term.name,
                        invoicePortion = term.invoicePortion ?: 100.0,
                        dueDate = dueDate,
                        modeOfPayment = term.modeOfPayment
                    )
                )
            } else {
                emptyList()
            }
            val invoiceDto = SalesInvoiceDto(
                customer = customer.name,
                customerName = customer.customerName,
                customerPhone = customer.mobileNo,
                company = context.company,
                postingDate = postingDate,
                currency = context.currency,
                dueDate = dueDate,
                status = status,
                grandTotal = total,
                outstandingAmount = outstandingAmount,
                totalTaxesAndCharges = 0.0,
                netTotal = total,
                paidAmount = paidAmount,
                items = items,
                paymentSchedule = paymentSchedule,
                paymentTerms = if (isCreditSale) current.selectedPaymentTerm?.name else null,
                posaDeliveryCharges = deliveryCharge?.label,
                posProfile = context.profileName,
                remarks = paymentMetadata
            )

            val created = invoiceRepository.createRemoteInvoice(invoiceDto)
            val entity = created.toEntity()
            invoiceRepository.saveInvoiceLocally(
                invoice = entity.invoice, items = entity.items, payments = entity.payments
            )

            if (!current.isCreditSale && paymentLines.isNotEmpty()) {
                val invoiceId = created.name
                    ?: error("Invoice ID was not returned after creation.")
                paymentLines.forEach { line ->
                    val baseAmount = line.enteredAmount * line.exchangeRate
                    val paymentEntry = PaymentEntryCreateDto(
                        company = context.company,
                        postingDate = postingDate,
                        paymentType = "Receive",
                        partyType = "Customer",
                        partyId = customer.name,
                        modeOfPayment = line.modeOfPayment,
                        paidAmount = baseAmount,
                        receivedAmount = baseAmount,
                        referenceNo = line.referenceNumber?.takeIf { it.isNotBlank() },
                        references = listOf(
                            PaymentEntryReferenceCreateDto(
                                referenceDoctype = "Sales Invoice",
                                referenceName = invoiceId,
                                totalAmount = total,
                                outstandingAmount = outstandingAmount,
                                allocatedAmount = baseAmount
                            )
                        )
                    )
                    api.createPaymentEntry(paymentEntry)
                }
            }

            _state.update {
                current.copy(
                    selectedCustomer = null,
                    cartItems = emptyList(),
                    subtotal = 0.0,
                    taxes = 0.0,
                    discount = 0.0,
                    discountCode = "",
                    manualDiscountAmount = 0.0,
                    manualDiscountPercent = 0.0,
                    shippingAmount = 0.0,
                    selectedDeliveryCharge = null,
                    total = 0.0,
                    isCreditSale = false,
                    selectedPaymentTerm = null,
                    customerSearchQuery = "",
                    productSearchQuery = "",
                    customers = customers,
                    productSearchResults = products,
                    paymentLines = emptyList(),
                    paidAmountBase = 0.0,
                    balanceDueBase = 0.0,
                    changeDueBase = 0.0,
                    paymentErrorMessage = null,
                    successMessage = "Invoice ${created.name ?: ""} created successfully."
                )
            }
        }, exceptionHandler = { e ->
            _state.update { BillingState.Error(e.message ?: "Unable to create invoice.") }
        })
    }

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }

    private fun recalculateTotals(current: BillingState.Success): BillingState.Success {
        val subtotal = current.cartItems.sumOf { it.price * it.quantity }
        val taxes = 0.0
        val discountInfo = resolveDiscountInfo(current, subtotal)
        val shippingAmount = current.shippingAmount.coerceAtLeast(0.0)
        val total = (subtotal + taxes - discountInfo.amount + shippingAmount).coerceAtLeast(0.0)
        return current.copy(
            subtotal = subtotal,
            taxes = taxes,
            discount = discountInfo.amount,
            total = total
        ).recalculatePaymentTotals()
    }

    private fun resolveDiscountInfo(
        current: BillingState.Success,
        subtotal: Double
    ): DiscountInfo {
        val hasPercent = current.manualDiscountPercent > 0.0
        val hasAmount = current.manualDiscountAmount > 0.0
        val source = when {
            current.discountCode.isNotBlank() -> DiscountSource.Code
            hasPercent || hasAmount -> DiscountSource.Manual
            else -> DiscountSource.None
        }
        val percent = current.manualDiscountPercent.takeIf { it > 0.0 }?.coerceAtMost(100.0)
        val amount = when {
            percent != null -> subtotal * (percent / 100.0)
            hasAmount -> current.manualDiscountAmount
            else -> 0.0
        }.coerceIn(0.0, subtotal)

        val effectiveAmount = when (source) {
            DiscountSource.None -> 0.0
            DiscountSource.Manual -> amount
            DiscountSource.Code -> amount
        }
        return DiscountInfo(
            amount = effectiveAmount,
            percent = percent.takeIf { effectiveAmount > 0.0 },
            source = source
        )
    }

    companion object {
        private const val DISCOUNT_ITEM_CODE = "Discount"
    }

    private fun requiresReference(mode: POSPaymentModeOption?): Boolean {
        val type = mode?.type?.trim().orEmpty()
        return type.equals("Bank", ignoreCase = true) ||
                type.equals("Card", ignoreCase = true) ||
                mode?.modeOfPayment?.contains("bank", ignoreCase = true) == true ||
                mode?.modeOfPayment?.contains("card", ignoreCase = true) == true
    }
}

private fun PaymentLine.toBaseAmount(): PaymentLine {
    return copy(baseAmount = enteredAmount * exchangeRate)
}
