package com.erpnext.pos.views.billing

import CartItem
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.usecases.FetchBillingProductsWithPriceUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.DateTimeProvider
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
    private val navManager: NavigationManager
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
            customersUseCase.invoke(null).collectLatest { c ->
                customers = c
                itemsUseCase.invoke(null).collectLatest { i ->
                    products = i
                    val currency = contextProvider.getContext()?.currency ?: "USD"
                    _state.update {
                        BillingState.Success(
                            customers = c,
                            productSearchResults = i,
                            currency = currency,
                            exchangeRate = 36.6243
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
                    query,
                    ignoreCase = true
                )
            }
        }
        _state.update {
            current.copy(
                customerSearchQuery = query, customers = filtered
            )
        }
    }

    fun onCustomerSelected(customer: CustomerBO) {
        val current = _state.value as? BillingState.Success ?: return
        _state.update { current.copy(selectedCustomer = customer) }
    }

    fun onProductSearchQueryChange(query: String) {
        val current = _state.value as? BillingState.Success ?: return
        val filtered = if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) or it.itemCode.contains(
                    query,
                    ignoreCase = true
                ) and (it.actualQty > 0)
            }
        }
        _state.update {
            current.copy(
                productSearchQuery = query,
                productSearchResults = filtered
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
                price = if (item.currency.equals("USD")) item.price * (exchangeRate
                    ?: 0.0) else item.price
            )
        } else {
            current.cartItems.map {
                if (it.itemCode == item.itemCode) it.copy(quantity = it.quantity + 1) else it
            }
        }
        _state.update { current.copy(cartItems = updated).recalculateTotals() }
    }

    fun onQuantityChanged(itemCode: String, newQuantity: Double) {
        val current = _state.value as? BillingState.Success ?: return
        val updated = current.cartItems.map {
            if (it.itemCode == itemCode) it.copy(quantity = newQuantity.coerceAtLeast(0.0)) else it
        }.filter { it.quantity > 0.0 }

        _state.update { current.copy(cartItems = updated).recalculateTotals() }
    }

    fun onRemoveItem(itemCode: String) {
        val current = _state.value as? BillingState.Success ?: return
        val updated = current.cartItems.filterNot { it.itemCode == itemCode }
        _state.update { current.copy(cartItems = updated).recalculateTotals() }
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

        val context = contextProvider.getContext() ?: error("POS context not initialized.")

        _state.update { current.copy() }

        executeUseCase(action = {
            val items = current.cartItems.map { cart ->
                SalesInvoiceItemDto(
                    itemCode = cart.itemCode,
                    itemName = cart.name,
                    qty = cart.quantity,
                    rate = cart.price,
                    amount = cart.quantity * cart.price,
                    warehouse = context.warehouse,
                    incomeAccount = context.incomeAccount
                )
            }

            val subtotal = items.sumOf { it.amount }
            val invoiceDto = SalesInvoiceDto(
                customer = customer.name,
                customerName = customer.customerName,
                customerPhone = customer.mobileNo,
                company = context.company,
                postingDate = DateTimeProvider.todayDate(),
                dueDate = DateTimeProvider.todayDate(),
                status = "Unpaid",
                grandTotal = subtotal,
                outstandingAmount = subtotal,
                totalTaxesAndCharges = 0.0,
                netTotal = subtotal,
                paidAmount = 0.0,
                items = items,
                payments = emptyList(),
                posProfile = context.profileName,
                currency = context.currency
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

    private fun BillingState.Success.recalculateTotals(): BillingState.Success {
        val newSubtotal = cartItems.sumOf { it.price * it.quantity }
        val newTaxes = 0.0
        val newDiscount = 0.0
        return copy(
            subtotal = newSubtotal,
            taxes = newTaxes,
            discount = newDiscount,
            total = newSubtotal + newTaxes - newDiscount
        )
    }
}
