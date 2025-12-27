package com.erpnext.pos.views.billing

import CartItem
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.usecases.FetchBillingProductsWithPriceUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.DateTimeProvider
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlin.collections.filter

class BillingViewModel(
    val customersUseCase: FetchCustomersUseCase,
    val itemsUseCase: FetchBillingProductsWithPriceUseCase,
    val contextProvider: CashBoxManager,
    private val invoiceRepository: SalesInvoiceRepository,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val navManager: NavigationManager
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

            customersUseCase.invoke(null).collectLatest { c ->
                customers = c
                itemsUseCase.invoke(null).collectLatest { i ->
                    products = i.filter { it.price > 0.0 && it.actualQty > 0.0 }
                    val currency = context.currency.ifBlank { "USD" }
                    val paymentModes = context.paymentModes.ifEmpty {
                        modeOfPaymentDao.getAll(context.profileName).map { mode ->
                            POSPaymentModeOption(
                                name = mode.name,
                                modeOfPayment = mode.modeOfPayment,
                                isDefault = mode.default
                            )
                        }
                    }.ifEmpty {
                        listOf(
                            POSPaymentModeOption(
                                name = "Cash",
                                modeOfPayment = "Cash"
                            ),
                            POSPaymentModeOption(
                                name = "Card",
                                modeOfPayment = "Card"
                            ),
                            POSPaymentModeOption(
                                name = "Transfer",
                                modeOfPayment = "Transfer"
                            )
                        )
                    }
                    _state.update {
                        BillingState.Success(
                            customers = customers,
                            productSearchResults = products,
                            currency = currency,
                            paymentModes = paymentModes,
                            allowedCurrencies = context.allowedCurrencies,
                            exchangeRate = contextProvider.getContext()?.exchangeRate ?: 1.0
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
        val modeCurrency = current.paymentModes.firstOrNull {
            it.modeOfPayment == line.modeOfPayment
        }?.currency
        if (modeCurrency != null && !modeCurrency.equals(line.currency, ignoreCase = true)) {
            _state.update {
                current.copy(
                    paymentErrorMessage = "Currency ${line.currency} is not allowed for ${line.modeOfPayment}."
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
        if (current.paidAmountBase < current.total) {
            _state.update { BillingState.Error("Paid amount must cover the total before finalizing the sale.") }
            return
        }

        val context = contextProvider.getContext() ?: error("POS context not initialized.")

        _state.update { current.copy() }

        executeUseCase(action = {
            val subtotal = current.cartItems.sumOf { it.price * it.quantity }
            val discountInfo = resolveDiscountInfo(current, subtotal)
            val shippingAmount = current.shippingAmount.coerceAtLeast(0.0)
            val discountPercent = discountInfo.percent?.takeIf { it > 0.0 }
            val discountAmount = discountInfo.amount
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

            if (shippingAmount > 0.0) {
                items.add(
                    SalesInvoiceItemDto(
                        itemCode = SHIPPING_ITEM_CODE,
                        itemName = "Shipping Charge",
                        qty = 1.0,
                        rate = shippingAmount,
                        amount = shippingAmount,
                        warehouse = context.warehouse,
                        incomeAccount = context.incomeAccount
                    )
                )
            }

            val total = (subtotal - discountAmount + shippingAmount).coerceAtLeast(0.0)
            val payments = current.paymentLines.map { line ->
                SalesInvoicePaymentDto(
                    modeOfPayment = line.modeOfPayment,
                    amount = line.baseAmount,
                    type = "Receive"
                )
            }
            val paidAmount = payments.sumOf { it.amount }
            val outstandingAmount = (total - paidAmount).coerceAtLeast(0.0)
            val status = when {
                paidAmount <= 0.0 -> "Unpaid"
                outstandingAmount <= 0.0 -> "Paid"
                else -> "Partly Paid"
            }
            val paymentMetadata = buildList {
                addAll(
                    current.paymentLines.map { line ->
                        "Payment currency: ${line.currency}, Exchange rate: ${line.exchangeRate}"
                    }
                )
                if (current.discountCode.isNotBlank()) {
                    add("Discount code: ${current.discountCode}")
                }
                if (shippingAmount > 0.0) {
                    add("Shipping: $shippingAmount")
                }
            }.joinToString(separator = "; ").takeIf { it.isNotBlank() }
            val primaryPaymentLine = current.paymentLines.firstOrNull()
            val invoiceDto = SalesInvoiceDto(
                customer = customer.name,
                customerName = customer.customerName,
                customerPhone = customer.mobileNo,
                company = context.company,
                postingDate = DateTimeProvider.todayDate(),
                dueDate = DateTimeProvider.todayDate(),
                status = status,
                grandTotal = total,
                outstandingAmount = outstandingAmount,
                totalTaxesAndCharges = 0.0,
                netTotal = total,
                paidAmount = paidAmount,
                items = items,
                payments = payments,
                posProfile = context.profileName,
                currency = context.currency,
                remarks = paymentMetadata,
                customPaymentCurrency = primaryPaymentLine?.currency,
                customExchangeRate = primaryPaymentLine?.exchangeRate
            )

            val created = invoiceRepository.createRemoteInvoice(invoiceDto)
            val entity = created.toEntity()
            invoiceRepository.saveInvoiceLocally(
                invoice = entity.invoice, items = entity.items, payments = entity.payments
            )

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
                        total = 0.0
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
        private const val SHIPPING_ITEM_CODE = "Shipping Charge"
    }
}

private fun PaymentLine.toBaseAmount(): PaymentLine {
    return copy(baseAmount = enteredAmount * exchangeRate)
}
