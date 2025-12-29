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
import com.erpnext.pos.domain.usecases.CreatePaymentEntryInput
import com.erpnext.pos.domain.usecases.CreatePaymentEntryUseCase
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceInput
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceUseCase
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentScheduleDto
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryReferenceCreateDto
import com.erpnext.pos.remoteSource.sdk.toUserMessage
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.DateTimeProvider
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.billing.BillingCalculationHelper.resolveDiscountInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

class BillingViewModel(
    val customersUseCase: FetchCustomersUseCase,
    val itemsUseCase: FetchBillingProductsWithPriceUseCase,
    val contextProvider: CashBoxManager,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val paymentTermsUseCase: FetchPaymentTermsUseCase,
    private val deliveryChargesUseCase: FetchDeliveryChargesUseCase,
    private val navManager: NavigationManager,
    private val createSalesInvoiceUseCase: CreateSalesInvoiceUseCase,
    private val createPaymentEntryUseCase: CreatePaymentEntryUseCase,
    private val api: APIService,
) : BaseViewModel() {

    private val _state: MutableStateFlow<BillingState> = MutableStateFlow(BillingState.Loading)
    val state = _state.asStateFlow()

    private var customers: List<CustomerBO> = emptyList()
    private var products: List<ItemBO> = emptyList()

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
            _state.value = BillingState.Error(
                it.toUserMessage("No se pudo cargar la información de facturación.")
            )
        })
    }

    private fun requireSuccessState(): BillingState.Success? {
        return when (val current = _state.value) {
            is BillingState.Success -> current
            is BillingState.Error -> current.previous?.also { _state.value = it }
            else -> null
        }
    }

    fun onCustomerSearchQueryChange(query: String) {
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
        _state.update {
            current.copy(
                selectedCustomer = customer, customerSearchQuery = customer.customerName
            )
        }
    }

    fun onProductSearchQueryChange(query: String) {
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
        val existing = current.cartItems.firstOrNull { it.itemCode == item.itemCode }
        val exchangeRate = current.exchangeRate
        val maxQty = item.actualQty
        val desiredQty = (existing?.quantity ?: 0.0) + 1.0
        if (desiredQty > maxQty) {
            _state.update {
                current.copy(
                    cartErrorMessage = buildQtyErrorMessage(item.name, maxQty)
                )
            }
            return
        }
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
        _state.update {
            recalculateTotals(current.copy(cartItems = updated, cartErrorMessage = null))
        }
    }

    fun onQuantityChanged(itemCode: String, newQuantity: Double) {
        val current = requireSuccessState() ?: return
        val product = products.firstOrNull { it.itemCode == itemCode }
        val maxQty = product?.actualQty
        if (maxQty != null && newQuantity > maxQty) {
            _state.update {
                current.copy(
                    cartErrorMessage = buildQtyErrorMessage(product.name, maxQty)
                )
            }
            return
        }
        val updated = current.cartItems.map {
            if (it.itemCode == itemCode) it.copy(quantity = newQuantity.coerceAtLeast(0.0)) else it
        }.filter { it.quantity > 0.0 }

        _state.update {
            recalculateTotals(current.copy(cartItems = updated, cartErrorMessage = null))
        }
    }

    fun onRemoveItem(itemCode: String) {
        val current = requireSuccessState() ?: return
        val updated = current.cartItems.filterNot { it.itemCode == itemCode }
        _state.update {
            recalculateTotals(current.copy(cartItems = updated, cartErrorMessage = null))
        }
    }

    fun onAddPaymentLine(line: PaymentLine) {
        val current = requireSuccessState() ?: return
        val modeOption = current.paymentModes.firstOrNull {
            it.modeOfPayment == line.modeOfPayment
        }
        if (requiresReference(modeOption) && line.referenceNumber.isNullOrBlank()) {
            _state.update {
                current.copy(
                    paymentErrorMessage = "El número de referencia es obligatorio para pagos ${line.modeOfPayment}."
                )
            }
            return
        }

        val updated = current.paymentLines + line.toBaseAmount()
        _state.update { current.withPaymentLines(updated) }
    }

    fun onUpdatePaymentLine(index: Int, line: PaymentLine) {
        val current = requireSuccessState() ?: return
        if (index !in current.paymentLines.indices) return
        val updated = current.paymentLines.mapIndexed { idx, existing ->
            if (idx == index) line.toBaseAmount() else existing
        }
        _state.update { current.withPaymentLines(updated) }
    }

    fun onRemovePaymentLine(index: Int) {
        val current = requireSuccessState() ?: return
        if (index !in current.paymentLines.indices) return
        val updated = current.paymentLines.filterIndexed { idx, _ -> idx != index }
        _state.update { current.withPaymentLines(updated) }
    }

    fun onCreditSaleChanged(isCreditSale: Boolean) {
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
        _state.update {
            current.copy(
                selectedPaymentTerm = term
            )
        }
    }

    fun onDiscountCodeChanged(code: String) {
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
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
        val current = requireSuccessState() ?: return
        if (current.successMessage == null) return
        _state.update { current.copy(successMessage = null) }
    }

    fun onFinalizeSale() {
        val current = requireSuccessState() ?: return
        val customer = current.selectedCustomer ?: run {
            _state.update {
                BillingState.Error(
                    "Selecciona un cliente antes de finalizar la venta.",
                    current
                )
            }
            return
        }
        if (current.cartItems.isEmpty()) {
            _state.update {
                BillingState.Error(
                    "Agrega al menos un artículo al carrito.",
                    current
                )
            }
            return
        }
        if (!current.isCreditSale && current.paidAmountBase < current.total) {
            _state.update {
                BillingState.Error(
                    "El monto pagado debe cubrir el total antes de finalizar la venta.",
                    current
                )
            }
            return
        }
        if (current.isCreditSale && current.selectedPaymentTerm == null) {
            _state.update {
                BillingState.Error(
                    "Selecciona un término de pago para finalizar una venta a crédito.",
                    current
                )
            }
            return
        }
        // Business rule: credit sales are registered as unpaid invoices and cannot include
        // immediate payment lines. Payments are posted later via Payment Entry.
        if (current.isCreditSale && current.paymentLines.isNotEmpty()) {
            _state.update {
                BillingState.Error(
                    "Las ventas a crédito no pueden incluir líneas de pago.",
                    current
                )
            }
            return
        }

        val context = contextProvider.getContext() ?: error("El contexto POS no está inicializado.")

        _state.update { current.copy() }

        executeUseCase(action = {
            val deliveryCharge = current.selectedDeliveryCharge
            val shippingAmount = deliveryCharge?.defaultRate?.coerceAtLeast(0.0) ?: 0.0

            val totals = BillingCalculationHelper.calculateTotals(current)
            val discountInfo = resolveDiscountInfo(current, totals.subtotal)

            val discountPercent = discountInfo.percent?.takeIf { it > 0.0 }
            val isCreditSale = current.isCreditSale
            val paymentLines = if (isCreditSale) emptyList() else current.paymentLines
            val baseCurrency = context.currency.ifBlank { current.currency ?: "USD" }

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


            if (discountPercent == null && discountInfo.amount > 0.0) {
                items.add(
                    SalesInvoiceItemDto(
                        itemCode = DISCOUNT_ITEM_CODE,
                        itemName = "Discount",
                        qty = 1.0,
                        rate = -discountInfo.amount,
                        amount = -discountInfo.amount,
                        warehouse = context.warehouse,
                        incomeAccount = context.incomeAccount
                    )
                )
            }

            val total = (totals.subtotal - discountInfo.amount + shippingAmount).coerceAtLeast(0.0)
            // ERPNext sales cycle:
            // - Credit sales must include payment_terms + payment_schedule with due_date derived from the term.
            // - Paid amount stays at 0 and status remains "Unpaid" until a Payment Entry is created.
            val paidAmount = if (isCreditSale) 0.0 else paymentLines.sumOf { it.baseAmount }
            val outstandingAmount = (totals.total - paidAmount).coerceAtLeast(0.0)
            val status =
                if (isCreditSale || outstandingAmount > 0.0) "Unpaid" else if (outstandingAmount == 0.0 || outstandingAmount < totals.total) "Unpaid" else "Paid"

            val postingDate = DateTimeProvider.todayDate()
            val dueDate = if (isCreditSale) {
                val term = current.selectedPaymentTerm
                    ?: error("El término de pago es obligatorio para ventas a crédito.")
                val withMonths = DateTimeProvider.addMonths(postingDate, term.creditMonths ?: 0)
                DateTimeProvider.addDays(withMonths, term.creditDays ?: 0)
            } else {
                postingDate
            }
            val paymentSchedule = if (isCreditSale) {
                val term = current.selectedPaymentTerm
                    ?: error("El término de pago es obligatorio para ventas a crédito.")
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
            val invoiceDto = buildSalesInvoiceDto(
                current = current,
                customer = customer,
                context = context,
                totals = totals,
                discountPercent = discountPercent,
                paidAmount = paidAmount,
                outstandingAmount = outstandingAmount,
                status = status,
                paymentSchedule = paymentSchedule,
                paymentLines = paymentLines,
                baseCurrency = baseCurrency,
                postingDate = postingDate,
                dueDate = dueDate
            )

            val created = createSalesInvoiceUseCase(CreateSalesInvoiceInput(invoiceDto))

            if (!current.isCreditSale && paymentLines.isNotEmpty()) {
                val invoiceId = created.name
                    ?: error("No se devolvió el ID de la factura después de crearla.")
                var remainingOutstanding = totals.total
                paymentLines.forEach { line ->
                    val allocation = minOf(line.baseAmount, remainingOutstanding)
                    if (allocation <= 0.0) return@forEach
                    val paymentEntry = buildPaymentEntryDto(
                        line = line,
                        context = context,
                        customer = customer,
                        postingDate = postingDate,
                        invoiceId = invoiceId,
                        invoiceTotal = totals.total,
                        paidFromAccount = created.debitTo,
                        partyAccountCurrency = created.currency,
                        exchangeRateByCurrency = current.exchangeRateByCurrency,
                        outstandingAmount = remainingOutstanding,
                        allocatedAmount = allocation,
                    )
                    createPaymentEntryUseCase(CreatePaymentEntryInput(paymentEntry))
                    remainingOutstanding -= allocation
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
                    cartErrorMessage = null,
                    successMessage = "Factura ${created.name ?: ""} creada correctamente."
                )
            }
        }, exceptionHandler = { e ->
            val errorMessage = e.toUserMessage("No se pudo crear la factura.")
            _state.update { currentState ->
                val previous = currentState as? BillingState.Success
                BillingState.Error(errorMessage, previous)
            }
        })
    }

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }

    private fun recalculateTotals(current: BillingState.Success): BillingState.Success {
        /*
         * Billing totals walkthrough (expected results)
         * Scenario:
         * - Customer: "Acme Retail".
         * - Base currency: USD.
         * - Items:
         *   1) "Coffee Beans" @ 50.00 USD x1 = 50.00
         *   2) "Paper Cups"  @ 30.00 USD x2 = 60.00
         *   Subtotal = 110.00
         * - Manual discount: 10% (no discount code) -> 110.00 * 10% = 11.00
         * - Shipping: 5.00
         * - Total = subtotal + taxes(0) - discount + shipping
         *         = 110.00 - 11.00 + 5.00 = 104.00
         *
         * Multi-currency payments (base amount uses enteredAmount * exchangeRate):
         * - Cash: 50.00 USD @ 1.0 = 50.00 base
         * - Card: 40.00 EUR @ 1.10 = 44.00 base
         *   PaidAmountBase = 50.00 + 44.00 = 94.00
         *   BalanceDueBase = total - paid = 104.00 - 94.00 = 10.00
         *   ChangeDueBase = max(paid - total, 0) = 0.00
         */
        val totals = BillingCalculationHelper.calculateTotals(current)
        return current.copy(
            subtotal = totals.subtotal,
            taxes = totals.taxes,
            discount = totals.discount,
            total = totals.total
        ).recalculatePaymentTotals()
    }

    private fun buildQtyErrorMessage(itemName: String, maxQty: Double): String {
        return "Solo hay ${formatQty(maxQty)} disponibles para $itemName."
    }

    private fun formatQty(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    companion object {
        private const val DISCOUNT_ITEM_CODE = "Discount"
    }

    private fun buildSalesInvoiceDto(
        current: BillingState.Success,
        customer: CustomerBO,
        context: POSContext,
        totals: BillingTotals,
        discountPercent: Double?,
        paidAmount: Double,
        outstandingAmount: Double,
        status: String,
        paymentSchedule: List<SalesInvoicePaymentScheduleDto>,
        paymentLines: List<PaymentLine>,
        baseCurrency: String,
        postingDate: String,
        dueDate: String
    ): SalesInvoiceDto {
        val items = buildInvoiceItems(
            current = current,
            context = context,
            discountPercent = discountPercent,
        )
        val paymentMetadata =
            buildPaymentMetadata(current, paymentLines, totals.shipping, baseCurrency)
        return SalesInvoiceDto(
            customer = customer.name,
            customerName = customer.customerName,
            customerPhone = customer.mobileNo,
            company = context.company,
            postingDate = postingDate,
            currency = baseCurrency,
            dueDate = dueDate,
            status = status,
            grandTotal = totals.total,
            outstandingAmount = outstandingAmount,
            totalTaxesAndCharges = totals.taxes,
            netTotal = totals.total,
            paidAmount = paidAmount,
            items = items,
            payments = listOf(
                SalesInvoicePaymentDto(paymentLines[0].modeOfPayment, 0.0)
            ),
            paymentSchedule = paymentSchedule,
            paymentTerms = if (current.isCreditSale) current.selectedPaymentTerm?.name else null,
            posProfile = context.profileName,
            remarks = paymentMetadata,
            updateStock = true,
            docStatus = 1
        )
    }

    private fun buildInvoiceItems(
        current: BillingState.Success,
        context: POSContext,
        discountPercent: Double?,
    ): MutableList<SalesInvoiceItemDto> {
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

        return items
    }

    private fun buildPaymentMetadata(
        current: BillingState.Success,
        paymentLines: List<PaymentLine>,
        shippingAmount: Double,
        baseCurrency: String
    ): String? {
        return buildList {
            addAll(
                paymentLines.mapNotNull { line ->
                    if (line.currency.equals(baseCurrency, ignoreCase = true)) {
                        null
                    } else {
                        "Moneda de pago (${line.modeOfPayment}): ${line.currency}, tipo de cambio: ${line.exchangeRate}"
                    }
                }
            )
            addAll(
                paymentLines.mapNotNull { line ->
                    line.referenceNumber?.takeIf { it.isNotBlank() }?.let {
                        "Referencia (${line.modeOfPayment}): $it"
                    }
                }
            )
            if (current.discountCode.isNotBlank()) {
                add("Código de descuento: ${current.discountCode}")
            }
            if (shippingAmount > 0.0) {
                add("Envío: $shippingAmount")
            }
        }.joinToString(separator = "; ").takeIf { it.isNotBlank() }
    }

    private suspend fun buildPaymentEntryDto(
        line: PaymentLine,
        context: POSContext,
        customer: CustomerBO,
        postingDate: String,
        invoiceId: String,
        invoiceTotal: Double,
        outstandingAmount: Double,
        paidFromAccount: String?,
        partyAccountCurrency: String?,
        exchangeRateByCurrency: Map<String, Double>,
        allocatedAmount: Double,
    ): PaymentEntryCreateDto {
        val baseCurrency = context.currency
        val isForeignCurrency = !line.currency.equals(baseCurrency, ignoreCase = true)
        val paidFromResolved = paidFromAccount?.takeIf { it.isNotBlank() }

        val targetExchangeRate = resolveTargetExchangeRate(
            baseCurrency = baseCurrency,
            partyAccountCurrency = partyAccountCurrency,
            paymentCurrency = line.currency,
            paymentExchangeRate = line.exchangeRate,
            exchangeRateByCurrency = exchangeRateByCurrency,
            isForeignCurrency = isForeignCurrency
        )
        return PaymentEntryCreateDto(
            company = context.company,
            postingDate = postingDate,
            paymentType = "Receive",
            partyType = "Customer",
            partyId = customer.name,
            modeOfPayment = line.modeOfPayment,
            paidAmount = allocatedAmount,
            receivedAmount = allocatedAmount,
            paidFrom = paidFromResolved,
            sourceExchangeRate = if (isForeignCurrency) 1.0 else null,
            targetExchangeRate = targetExchangeRate,
            referenceNo = line.referenceNumber?.takeIf { it.isNotBlank() },
            references = listOf(
                PaymentEntryReferenceCreateDto(
                    referenceDoctype = "Sales Invoice",
                    referenceName = invoiceId,
                    totalAmount = invoiceTotal,
                    outstandingAmount = outstandingAmount,
                    allocatedAmount = allocatedAmount
                )
            )
        )
    }

    private suspend fun resolveTargetExchangeRate(
        baseCurrency: String,
        partyAccountCurrency: String?,
        paymentCurrency: String,
        paymentExchangeRate: Double,
        exchangeRateByCurrency: Map<String, Double>,
        isForeignCurrency: Boolean
    ): Double? {
        if (partyAccountCurrency.isNullOrBlank() ||
            partyAccountCurrency.equals(baseCurrency, ignoreCase = true)
        ) {
            return if (isForeignCurrency) paymentExchangeRate else null
        }

        if (partyAccountCurrency.equals(paymentCurrency, ignoreCase = true)) {
            return paymentExchangeRate
        }

        val normalizedParty = partyAccountCurrency.trim().uppercase()
        val cachedRate = exchangeRateByCurrency[normalizedParty]
        if (cachedRate != null && cachedRate > 0.0) {
            return cachedRate
        }

        val directRate = api.getExchangeRate(
            fromCurrency = normalizedParty,
            toCurrency = baseCurrency
        )
        val reverseRate = api.getExchangeRate(
            fromCurrency = baseCurrency,
            toCurrency = normalizedParty
        )?.takeIf { it > 0.0 }?.let { 1 / it }

        return directRate ?: reverseRate ?: if (isForeignCurrency) paymentExchangeRate else null
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
