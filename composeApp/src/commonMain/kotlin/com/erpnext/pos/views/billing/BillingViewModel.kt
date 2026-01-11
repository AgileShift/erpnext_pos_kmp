@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.billing

import CartItem
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.domain.usecases.FetchBillingProductsLocalUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersLocalUseCase
import com.erpnext.pos.domain.usecases.FetchDeliveryChargesUseCase
import com.erpnext.pos.domain.usecases.FetchPaymentTermsUseCase
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceLocalInput
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceLocalUseCase
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceRemoteOnlyInput
import com.erpnext.pos.domain.usecases.CreateSalesInvoiceRemoteOnlyUseCase
import com.erpnext.pos.domain.usecases.UpdateLocalInvoiceFromRemoteInput
import com.erpnext.pos.domain.usecases.UpdateLocalInvoiceFromRemoteUseCase
import com.erpnext.pos.domain.usecases.MarkSalesInvoiceSyncedUseCase
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentScheduleDto
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.sdk.toUserMessage
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.DateTimeProvider
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.usecases.AdjustLocalInventoryInput
import com.erpnext.pos.domain.usecases.AdjustLocalInventoryUseCase
import com.erpnext.pos.domain.usecases.StockDelta
import com.erpnext.pos.domain.usecases.v2.LoadSourceDocumentsInput
import com.erpnext.pos.domain.usecases.v2.LoadSourceDocumentsUseCase
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.salesflow.SalesFlowContext
import com.erpnext.pos.views.salesflow.SalesFlowContextStore
import com.erpnext.pos.views.salesflow.SalesFlowSource
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.requiresReference
import com.erpnext.pos.utils.resolvePaymentStatus
import com.erpnext.pos.utils.resolveExchangeRateBetween
import com.erpnext.pos.utils.resolveRateToInvoiceCurrency
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.domain.models.BillingTotals
import com.erpnext.pos.utils.buildPaymentModeDetailMap
import com.erpnext.pos.utils.calculateTotals
import com.erpnext.pos.utils.resolveDiscountInfo
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.views.payment.PaymentHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlin.time.ExperimentalTime

class BillingViewModel(
    private val customersUseCase: FetchCustomersLocalUseCase,
    private val itemsUseCase: FetchBillingProductsLocalUseCase,
    private val adjustLocalInventoryUseCase: AdjustLocalInventoryUseCase,
    private val contextProvider: CashBoxManager,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val paymentTermsUseCase: FetchPaymentTermsUseCase,
    private val deliveryChargesUseCase: FetchDeliveryChargesUseCase,
    private val navManager: NavigationManager,
    private val salesFlowStore: SalesFlowContextStore,
    private val loadSourceDocumentsUseCase: LoadSourceDocumentsUseCase,
    private val createSalesInvoiceLocalUseCase: CreateSalesInvoiceLocalUseCase,
    private val createSalesInvoiceRemoteOnlyUseCase: CreateSalesInvoiceRemoteOnlyUseCase,
    private val updateLocalInvoiceFromRemoteUseCase: UpdateLocalInvoiceFromRemoteUseCase,
    private val markSalesInvoiceSyncedUseCase: MarkSalesInvoiceSyncedUseCase,
    private val api: APIService,
    private val paymentHandler: PaymentHandler,
) : BaseViewModel() {

    private val _state: MutableStateFlow<BillingState> = MutableStateFlow(BillingState.Loading)
    val state = _state.asStateFlow()

    private var customers: List<CustomerBO> = emptyList()
    private var products: List<ItemBO> = emptyList()
    private var pendingSalesFlowContext: SalesFlowContext? = null

    /**
     * Mapa de definiciones de modo de pago.
     * OJO: buildPaymentModeDetailMap() agrega claves por:
     * - mode_of_payment
     * - name
     */
    private var paymentModeDetails: Map<String, ModeOfPaymentEntity> = emptyMap()

    init {
        observeSalesFlowContext()
        loadInitialData()
    }

    private fun observeSalesFlowContext() {
        viewModelScope.launch {
            salesFlowStore.context.collect { context ->
                if (context != null) {
                    applySalesFlowContext(context)
                    salesFlowStore.clear()
                }
            }
        }
    }

    fun loadInitialData() {
        executeUseCase(action = {
            val context = contextProvider.requireContext()
            val paymentTerms =
                runCatching { paymentTermsUseCase.invoke(Unit) }.getOrElse { emptyList() }
            val deliveryCharges =
                runCatching { deliveryChargesUseCase.invoke(Unit) }.getOrElse { emptyList() }

            customersUseCase.invoke(null).collectLatest { c ->
                customers = c
                itemsUseCase.invoke(null).collectLatest { i ->
                    products = i.filter { it.price > 0.0 && it.actualQty > 0.0 }

                    val invoiceCurrency = context.currency.ifBlank { "USD" }.trim()

                    val modeDefinitions =
                        runCatching { modeOfPaymentDao.getAllModes(context.company) }
                            .getOrElse { emptyList() }

                    val modeTypes = modeDefinitions.associateBy { it.modeOfPayment }
                    paymentModeDetails = buildPaymentModeDetailMap(modeDefinitions)

                    val paymentModeCurrencyByMode = buildMap {
                        modeDefinitions.forEach { def ->
                            val currency = def.currency?.trim()?.uppercase().orEmpty()
                            if (currency.isNotBlank()) {
                                put(def.modeOfPayment, currency)
                                put(def.name, currency)
                            }
                        }
                    }

                    val paymentModes = context.paymentModes.ifEmpty {
                        modeOfPaymentDao.getAll(context.company).map { mode ->
                            POSPaymentModeOption(
                                name = mode.name,
                                modeOfPayment = mode.modeOfPayment,
                                type = modeTypes[mode.modeOfPayment]?.type,
                            )
                        }
                    }

                    // Cache de tasas "paymentCurrency -> invoiceCurrency"
                    val exchangeRateByCurrency = mapOf(invoiceCurrency.uppercase() to 1.0)

                    val contextSelection = pendingSalesFlowContext
                    val selectedCustomer = contextSelection?.customerId?.let { customerId ->
                        customers.firstOrNull { it.name == customerId }
                    }

                    _state.update {
                        BillingState.Success(
                            customers = customers,
                            selectedCustomer = selectedCustomer,
                            customerSearchQuery = selectedCustomer?.customerName.orEmpty(),
                            productSearchResults = products,
                            currency = invoiceCurrency,
                            paymentModes = paymentModes,
                            allowedCurrencies = context.allowedCurrencies,
                            exchangeRate = contextProvider.getContext()?.exchangeRate ?: 1.0,
                            paymentTerms = paymentTerms,
                            deliveryCharges = deliveryCharges,
                            exchangeRateByCurrency = exchangeRateByCurrency,
                            paymentModeCurrencyByMode = paymentModeCurrencyByMode,
                            salesFlowContext = contextSelection
                        )
                    }
                    pendingSalesFlowContext = null
                }
            }
        }, exceptionHandler = {
            _state.value = BillingState.Error(
                it.toUserMessage("No se pudo cargar la información de facturación.")
            )
        }, showLoading = false)
    }

    private fun requireSuccessState(): BillingState.Success? {
        return when (val current = _state.value) {
            is BillingState.Success -> current
            is BillingState.Error -> current.previous?.also { _state.value = it }
            else -> null
        }
    }

    private fun applySalesFlowContext(context: SalesFlowContext) {
        val current = requireSuccessState()
        if (current == null) {
            pendingSalesFlowContext = context
            return
        }

        val selectedCustomer = context.customerId?.let { customerId ->
            customers.firstOrNull { it.name == customerId }
        }

        _state.update {
            current.copy(
                selectedCustomer = selectedCustomer ?: current.selectedCustomer,
                customerSearchQuery = selectedCustomer?.customerName ?: current.customerSearchQuery,
                salesFlowContext = context
            )
        }
    }

    // -------------------------------------------------------------------------
    // ✅ Moneda automática por modo + tasa automática
    // -------------------------------------------------------------------------


    /*private fun resolvePaymentCurrencyForMode(
        modeOfPayment: String,
        invoiceCurrency: String,
        paymentModeCurrencyByMode: Map<String, String>?
    ): String {
        val inv = normalizeCurrency(invoiceCurrency) ?: "USD"

        val fromDef = paymentModeDetails[modeOfPayment]?.currency
        val c1 = normalizeCurrency(fromDef)
        if (c1 != null) return c1

        val c2 = normalizeCurrency(paymentModeCurrencyByMode?.get(modeOfPayment))
        if (c2 != null) return c2

        return inv
    }*/


    // -------------------------------------------------------------------------
    // Cliente / carrito
    // -------------------------------------------------------------------------

    fun onCustomerSearchQueryChange(query: String) {
        val current = requireSuccessState() ?: return
        val filtered = if (query.isBlank()) {
            customers
        } else {
            customers.filter {
                it.customerName.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true)
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
        val updatedFlowContext = current.salesFlowContext?.withCustomer(
            customerId = customer.name,
            customerName = customer.customerName
        )
        _state.update {
            current.copy(
                selectedCustomer = customer,
                customerSearchQuery = customer.customerName,
                salesFlowContext = updatedFlowContext,
                sourceDocuments = emptyList(),
                sourceDocumentsError = null
            )
        }
    }

    fun linkSourceDocument(sourceType: SalesFlowSource, sourceId: String) {
        val current = requireSuccessState() ?: return
        val selectedDoc = current.sourceDocuments.firstOrNull {
            it.sourceType == sourceType && it.id == sourceId
        }

        if (selectedDoc == null) {
            val updated = (current.salesFlowContext ?: SalesFlowContext())
                .withSource(sourceType, sourceId)
            _state.update { current.copy(salesFlowContext = updated) }
            return
        }

        executeUseCase(action = {
            val context = contextProvider.requireContext()
            val baseCurrency = current.currency?.trim().orEmpty()
                .ifBlank { context.currency.trim().ifBlank { "USD" } }
            val sourceCurrency = selectedDoc.totals?.currency
            val rate = resolveSourceExchangeRate(
                sourceCurrency = sourceCurrency,
                baseCurrency = baseCurrency,
                exchangeRateByCurrency = current.exchangeRateByCurrency,
                fallbackRate = context.exchangeRate
            )

            val convertedDoc = convertSourceDocument(
                source = selectedDoc,
                baseCurrency = baseCurrency,
                rate = rate
            )

            val updatedCustomer = convertedDoc.customerId?.let { id ->
                customers.firstOrNull { it.name == id }
            } ?: current.selectedCustomer

            val updatedContext = (current.salesFlowContext ?: SalesFlowContext())
                .withCustomer(
                    customerId = updatedCustomer?.name ?: convertedDoc.customerId,
                    customerName = updatedCustomer?.customerName ?: convertedDoc.customerName
                )
                .withSource(sourceType, sourceId)

            val cartItems = convertedDoc.items.map { item ->
                CartItem(
                    itemCode = item.itemCode,
                    name = item.itemName ?: item.itemCode,
                    currency = baseCurrency.toCurrencySymbol(),
                    quantity = item.qty,
                    price = item.rate
                )
            }

            val next = current.copy(
                selectedCustomer = updatedCustomer,
                customerSearchQuery = updatedCustomer?.customerName ?: current.customerSearchQuery,
                salesFlowContext = updatedContext,
                cartItems = cartItems,
                discountCode = "",
                manualDiscountAmount = 0.0,
                manualDiscountPercent = 0.0,
                shippingAmount = 0.0,
                selectedDeliveryCharge = null,
                isCreditSale = false,
                selectedPaymentTerm = null,
                paymentLines = emptyList(),
                paidAmountBase = 0.0,
                balanceDueBase = 0.0,
                changeDueBase = 0.0,
                paymentErrorMessage = null,
                cartErrorMessage = null,
                sourceDocument = convertedDoc,
                isSourceDocumentApplied = true,
                exchangeRateByCurrency = current.exchangeRateByCurrency
                    .plus(baseCurrency.uppercase() to 1.0)
                    .let { cache ->
                        val sourceKey = sourceCurrency?.trim()?.uppercase().orEmpty()
                        if (sourceKey.isBlank() || sourceKey == baseCurrency.uppercase()) cache
                        else cache.plus(sourceKey to rate)
                    }
            )

            _state.update { recalculateTotals(next) }
        }, exceptionHandler = { e ->
            _state.update {
                current.copy(
                    cartErrorMessage = e.toUserMessage("No se pudo aplicar el documento de origen.")
                )
            }
        })
    }

    fun clearSourceDocument() {
        val current = requireSuccessState() ?: return
        val updated = current.salesFlowContext?.copy(sourceType = null, sourceId = null)
        val reset = resetFromSource(current).copy(salesFlowContext = updated)
        _state.update { reset }
    }

    fun loadSourceDocuments(sourceType: SalesFlowSource) {
        val current = requireSuccessState() ?: return
        val customerId = current.selectedCustomer?.name
        if (customerId.isNullOrBlank()) {
            _state.update {
                current.copy(
                    sourceDocuments = emptyList(),
                    isLoadingSourceDocuments = false,
                    sourceDocumentsError = "Selecciona un cliente primero."
                )
            }
            return
        }

        _state.update { current.copy(isLoadingSourceDocuments = true, sourceDocumentsError = null) }
        executeUseCase(
            action = {
                val docs = loadSourceDocumentsUseCase(
                    LoadSourceDocumentsInput(customerId = customerId, sourceType = sourceType)
                )
                _state.update {
                    current.copy(
                        sourceDocuments = docs,
                        isLoadingSourceDocuments = false,
                        sourceDocumentsError = null
                    )
                }
            },
            exceptionHandler = { throwable ->
                _state.update {
                    current.copy(
                        sourceDocuments = emptyList(),
                        isLoadingSourceDocuments = false,
                        sourceDocumentsError = throwable.message
                            ?: "No se pudieron cargar los documentos."
                    )
                }
            }
        )
    }

    fun onProductSearchQueryChange(query: String) {
        val current = requireSuccessState() ?: return
        val filtered = if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.itemCode.contains(query, ignoreCase = true)
            }
        }
        _state.update { current.copy(productSearchQuery = query, productSearchResults = filtered) }
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
                    cartErrorMessage = buildQtyErrorMessage(
                        item.name,
                        maxQty
                    )
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
            recalculateTotals(
                current.copy(
                    cartItems = updated,
                    cartErrorMessage = null
                )
            )
        }
    }

    fun onQuantityChanged(itemCode: String, newQuantity: Double) {
        val current = requireSuccessState() ?: return
        val product = products.firstOrNull { it.itemCode == itemCode }
        val maxQty = product?.actualQty

        if (maxQty != null && newQuantity > maxQty) {
            _state.update {
                current.copy(
                    cartErrorMessage = buildQtyErrorMessage(
                        product.name,
                        maxQty
                    )
                )
            }
            return
        }

        val updated = current.cartItems.map {
            if (it.itemCode == itemCode) it.copy(quantity = newQuantity.coerceAtLeast(0.0)) else it
        }.filter { it.quantity > 0.0 }

        _state.update {
            recalculateTotals(
                current.copy(
                    cartItems = updated,
                    cartErrorMessage = null
                )
            )
        }
    }

    fun onRemoveItem(itemCode: String) {
        val current = requireSuccessState() ?: return
        val updated = current.cartItems.filterNot { it.itemCode == itemCode }
        _state.update {
            recalculateTotals(
                current.copy(
                    cartItems = updated,
                    cartErrorMessage = null
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // ✅ PAGOS: moneda + tasa automáticas (UI ya no decide)
    // -------------------------------------------------------------------------

    fun onAddPaymentLine(line: PaymentLine) {
        val current = requireSuccessState() ?: return

        val modeOption = current.paymentModes.firstOrNull { it.modeOfPayment == line.modeOfPayment }
        if (requiresReference(modeOption) && line.referenceNumber.isNullOrBlank()) {
            _state.update {
                current.copy(
                    paymentErrorMessage = "El número de referencia es obligatorio para pagos ${line.modeOfPayment}."
                )
            }
            return
        }

        val context = contextProvider.requireContext()
        val invoiceCurrency = normalizeCurrency(context.currency)
            ?: normalizeCurrency(current.currency)
            ?: "USD"

        executeUseCase(
            action = {
                val result = paymentHandler.resolvePaymentLine(
                    line = line,
                    invoiceCurrencyInput = invoiceCurrency,
                    paymentModeCurrencyByMode = current.paymentModeCurrencyByMode,
                    paymentModeDetails = paymentModeDetails,
                    exchangeRateByCurrency = current.exchangeRateByCurrency,
                    round = ::roundToCurrency
                )

                _state.update { st ->
                    val s = (st as? BillingState.Success) ?: return@update st
                    s.copy(exchangeRateByCurrency = result.exchangeRateByCurrency)
                        .withPaymentLines(s.paymentLines + result.line)
                }
            },
            exceptionHandler = { e ->
                _state.update {
                    current.copy(
                        paymentErrorMessage = e.toUserMessage("No se pudo calcular moneda/tasa del pago.")
                    )
                }
            }
        )
    }

    fun onRemovePaymentLine(index: Int) {
        val current = requireSuccessState() ?: return
        if (index !in current.paymentLines.indices) return
        val updated = current.paymentLines.filterIndexed { idx, _ -> idx != index }
        _state.update { current.withPaymentLines(updated) }
    }

    fun onPaymentCurrencySelected(currency: String) {
        val current = requireSuccessState() ?: return
        val baseCurrency = normalizeCurrency(contextProvider.requireContext().currency) ?: "USD"
        val paymentCurrency = normalizeCurrency(currency) ?: return
        if (paymentCurrency.equals(baseCurrency, ignoreCase = true)) {
            _state.update {
                current.copy(
                    exchangeRateByCurrency = current.exchangeRateByCurrency + (baseCurrency to 1.0)
                )
            }
            return
        }

        executeUseCase(
            action = {
                val rate = resolveRateToInvoiceCurrency(
                    api = api,
                    paymentCurrency = paymentCurrency,
                    invoiceCurrency = baseCurrency,
                    cache = current.exchangeRateByCurrency
                )
                _state.update {
                    current.copy(
                        exchangeRateByCurrency = current.exchangeRateByCurrency
                            .plus(baseCurrency to 1.0)
                            .plus(paymentCurrency to rate)
                    )
                }
            },
            exceptionHandler = {
                // No bloqueamos UI si falla la tasa; el pago resolverá tasa al guardar.
            }
        )
    }

    fun onCreditSaleChanged(isCreditSale: Boolean) {
        val current = requireSuccessState() ?: return
        if (isCreditSale && current.paymentTerms.isEmpty()) return

        _state.update {
            current.copy(
                isCreditSale = isCreditSale,
                selectedPaymentTerm = if (isCreditSale) current.selectedPaymentTerm else null,
                //paymentLines = if (isCreditSale) emptyList() else current.paymentLines,
                paymentLines = current.paymentLines,
                paymentErrorMessage = null
            ).recalculatePaymentTotals()
        }
    }

    fun onPaymentTermSelected(term: PaymentTermBO?) {
        val current = requireSuccessState() ?: return
        _state.update { current.copy(selectedPaymentTerm = term) }
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

    fun onClearSuccessMessage() {
        val current = requireSuccessState() ?: return
        if (current.successMessage == null) return
        _state.update { current.copy(successMessage = null) }
    }

    fun onFinalizeSale() {
        val current = requireSuccessState() ?: return
        val validationError = validateFinalizeSale(current)
        if (validationError != null) {
            _state.update { BillingState.Error(validationError, current) }
            return
        }
        setFinalizingSale(true)

        val customer = current.selectedCustomer ?: error("Debes seleccionar un cliente.")
        val context = contextProvider.getContext() ?: error("El contexto POS no está inicializado.")

        executeUseCase(action = {
            val rawTotals = calculateTotals(current)
            val totals = rawTotals.copy(
                subtotal = roundToCurrency(rawTotals.subtotal),
                taxes = roundToCurrency(rawTotals.taxes),
                discount = roundToCurrency(rawTotals.discount),
                shipping = roundToCurrency(rawTotals.shipping),
                total = roundToCurrency(rawTotals.total)
            )
            val discountInfo = resolveDiscountInfo(current, totals.subtotal)
            val discountPercent = discountInfo.percent?.takeIf { it > 0.0 }

            val isCreditSale = current.isCreditSale
            val paymentLines = current.paymentLines
            val baseCurrency = context.currency.ifBlank { current.currency ?: "USD" }

            val postingDate = DateTimeProvider.todayDate()
            val dueDate = resolveDueDate(isCreditSale, postingDate, current.selectedPaymentTerm)
            val paymentSchedule =
                buildPaymentSchedule(isCreditSale, current.selectedPaymentTerm, dueDate)

            val paymentStatus = resolvePaymentStatus(
                total = totals.total,
                paymentLines = paymentLines,
                round = ::roundToCurrency
            )

            val invoiceCurrency = normalizeCurrency(baseCurrency) ?: "USD"
            val receivableCurrency = normalizeCurrency(context.partyAccountCurrency)
                ?: invoiceCurrency
            val conversionRate = resolveInvoiceConversionRate(
                invoiceCurrency = invoiceCurrency,
                receivableCurrency = receivableCurrency,
                context = context
            )

            val invoiceDto = buildSalesInvoiceDto(
                current = current,
                customer = customer,
                context = context,
                totals = totals,
                discountPercent = discountPercent,
                discountAmount = discountInfo.amount,
                paidAmount = paymentStatus.paidAmount,
                outstandingAmount = paymentStatus.outstandingAmount,
                status = paymentStatus.status,
                paymentSchedule = paymentSchedule,
                paymentLines = paymentLines,
                baseCurrency = baseCurrency,
                conversionRate = conversionRate,
                postingDate = postingDate,
                dueDate = dueDate
            )

            val localInvoiceName = "LOCAL-${UUIDGenerator().newId()}"
            createSalesInvoiceLocalUseCase(
                CreateSalesInvoiceLocalInput(
                    localInvoiceName = localInvoiceName,
                    invoice = invoiceDto
                )
            )

            val created = runCatching {
                createSalesInvoiceRemoteOnlyUseCase(
                    CreateSalesInvoiceRemoteOnlyInput(invoiceDto.copy(name = null))
                )
            }.getOrNull()

            var invoiceNameForLocal = localInvoiceName
            if (created?.name != null) {
                val updateResult = runCatching {
                    updateLocalInvoiceFromRemoteUseCase(
                        UpdateLocalInvoiceFromRemoteInput(
                            localInvoiceName = localInvoiceName,
                            remoteInvoice = created
                        )
                    )
                }
                if (updateResult.isSuccess) {
                    invoiceNameForLocal = created.name
                }
            }

            val paymentResult = paymentHandler.registerPayments(
                paymentLines = paymentLines,
                createdInvoice = created,
                invoiceNameForLocal = invoiceNameForLocal,
                postingDate = postingDate,
                context = context,
                customer = customer,
                exchangeRateByCurrency = current.exchangeRateByCurrency,
                paymentModeDetails = paymentModeDetails,
                baseAmountCurrency = baseCurrency
            )
            invoiceNameForLocal = paymentResult.invoiceNameForLocal
            val remotePaymentsSucceeded = paymentResult.remotePaymentsSucceeded
            if (remotePaymentsSucceeded) {
                runCatching {
                    markSalesInvoiceSyncedUseCase(invoiceNameForLocal)
                }
            }

            runCatching {
                adjustLocalInventoryUseCase(
                    AdjustLocalInventoryInput(
                        warehouse = context.warehouse ?: "",
                        deltas = current.cartItems.map {
                            StockDelta(
                                itemCode = it.itemCode,
                                qty = it.quantity
                            )
                        }
                    )
                )
            }.onFailure {
                _state.update { st ->
                    val s = st as? BillingState.Success ?: return@update st
                    s.copy(
                        successMessage = "Factura ${(created?.name ?: localInvoiceName)} creada, pero fallo la actualizacion de inventario local. Reintenta sincronizacion/recarga."
                    )
                }
            }

            val soldByCode = current.cartItems
                .groupBy { it.itemCode }
                .mapValues { (_, list) -> list.sumOf { it.quantity } }

            products = products.map { p ->
                val sold = soldByCode[p.itemCode] ?: 0.0
                if (sold <= 0.0) p
                else p.copy(actualQty = (p.actualQty - sold).coerceAtLeast(0.0))
            }.filter { it.price > 0.0 && it.actualQty > 0.0 }

            val q = current.productSearchQuery
            val refreshedResults = if (q.isBlank()) products else products.filter {
                it.name.contains(q, ignoreCase = true) || it.itemCode.contains(q, ignoreCase = true)
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
                    productSearchResults = refreshedResults,
                    paymentLines = emptyList(),
                    paidAmountBase = 0.0,
                    balanceDueBase = 0.0,
                    changeDueBase = 0.0,
                    paymentErrorMessage = null,
                    cartErrorMessage = null,
                    successMessage = when {
                        created == null -> {
                            "Factura $localInvoiceName guardada localmente (pendiente de sincronizacion)."
                        }

                        paymentLines.isNotEmpty() -> {
                            val label = created.name ?: localInvoiceName
                            "Factura $label creada. Pagos guardados localmente."
                        }

                        else -> {
                            val label = created.name ?: localInvoiceName
                            "Factura $label creada correctamente."
                        }
                    },
                    sourceDocument = null,
                    isSourceDocumentApplied = false
                )
            }
        }, exceptionHandler = { e ->
            val errorMessage = e.toUserMessage("No se pudo crear la factura.")
            _state.update { currentState ->
                val previous = currentState as? BillingState.Success
                BillingState.Error(errorMessage, previous)
            }
        }, finallyHandler = {
            setFinalizingSale(false)
        })
    }

    private fun setFinalizingSale(active: Boolean) {
        _state.update { current ->
            val success = current as? BillingState.Success ?: return@update current
            if (success.isFinalizingSale == active) return@update current
            success.copy(isFinalizingSale = active)
        }
    }

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }

    fun onOpenLab() {
        navManager.navigateTo(NavRoute.BillingLab)
    }

    private fun recalculateTotals(current: BillingState.Success): BillingState.Success {
        val totals = calculateTotals(current)
        return current.copy(
            subtotal = totals.subtotal,
            taxes = totals.taxes,
            discount = totals.discount,
            total = totals.total
        ).recalculatePaymentTotals()
    }

    private suspend fun resolveSourceExchangeRate(
        sourceCurrency: String?,
        baseCurrency: String,
        exchangeRateByCurrency: Map<String, Double>,
        fallbackRate: Double
    ): Double {
        val from = sourceCurrency?.trim()?.uppercase().takeIf { !it.isNullOrBlank() }
        val to = baseCurrency.trim().uppercase()
        if (from == null || from == to) return 1.0

        exchangeRateByCurrency[from]?.takeIf { it > 0.0 }?.let { return it }

        if (from == "USD" && to != "USD" && fallbackRate > 0.0) {
            return fallbackRate
        }

        return resolveExchangeRateBetween(
            api = api,
            fromCurrency = from,
            toCurrency = to
        ) ?: error("No se pudo resolver la tasa de cambio $from -> $to")
    }

    private fun convertSourceDocument(
        source: com.erpnext.pos.domain.models.SourceDocumentOption,
        baseCurrency: String,
        rate: Double
    ): com.erpnext.pos.domain.models.SourceDocumentOption {
        if (rate == 1.0) return source
        val convertedTotals = source.totals?.let { totals ->
            totals.copy(
                netTotal = totals.netTotal?.let { it * rate },
                grandTotal = totals.grandTotal?.let { it * rate },
                taxTotal = totals.taxTotal?.let { it * rate },
                currency = baseCurrency
            )
        }
        val convertedItems = source.items.map { item ->
            item.copy(
                rate = item.rate * rate,
                amount = item.amount * rate
            )
        }
        return source.copy(
            items = convertedItems,
            totals = convertedTotals
        )
    }

    private fun resetFromSource(current: BillingState.Success): BillingState.Success {
        return current.copy(
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
            paymentLines = emptyList(),
            paidAmountBase = 0.0,
            balanceDueBase = 0.0,
            changeDueBase = 0.0,
            paymentErrorMessage = null,
            cartErrorMessage = null,
            sourceDocument = null,
            isSourceDocumentApplied = false
        )
    }

    private fun buildQtyErrorMessage(itemName: String, maxQty: Double): String {
        return "Solo hay ${formatQty(maxQty)} disponibles para $itemName."
    }

    private fun formatQty(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
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
        discountAmount: Double,
        paidAmount: Double,
        outstandingAmount: Double,
        status: String,
        paymentSchedule: List<SalesInvoicePaymentScheduleDto>,
        paymentLines: List<PaymentLine>,
        baseCurrency: String,
        conversionRate: Double?,
        postingDate: String,
        dueDate: String
    ): SalesInvoiceDto {
        val items = buildInvoiceItems(current, context, discountPercent, discountAmount)
        val paymentMetadata =
            buildInvoiceRemarks(current, paymentLines, totals.shipping, baseCurrency)

        val paymentMode = paymentLines.firstOrNull()?.modeOfPayment
            ?: current.selectedPaymentTerm?.modeOfPayment
            ?: current.paymentModes.firstOrNull()?.modeOfPayment
            ?: error("No hay modo de pago disponible para crear la factura.")

        val payments = listOf(SalesInvoicePaymentDto(paymentMode, 0.0))

        return SalesInvoiceDto(
            customer = customer.name,
            customerName = customer.customerName,
            customerPhone = customer.mobileNo,
            company = context.company,
            postingDate = postingDate,
            currency = baseCurrency,
            conversionRate = conversionRate?.takeIf { it > 0.0 },
            partyAccountCurrency = context.partyAccountCurrency,
            dueDate = dueDate,
            status = status,
            grandTotal = totals.total,
            outstandingAmount = outstandingAmount,
            totalTaxesAndCharges = totals.taxes,
            netTotal = totals.total,
            paidAmount = paidAmount,
            items = items,
            payments = payments,
            paymentSchedule = paymentSchedule,
            paymentTerms = if (current.isCreditSale) current.selectedPaymentTerm?.name else null,
            posProfile = context.profileName,
            remarks = paymentMetadata,
            customExchangeRate = conversionRate?.takeIf { it > 0.0 },
            updateStock = true,
            docStatus = 1
        )
    }

    private fun buildInvoiceItems(
        current: BillingState.Success,
        context: POSContext,
        discountPercent: Double?,
        discountAmount: Double
    ): MutableList<SalesInvoiceItemDto> {
        val source = current.salesFlowContext
        val sourceId = source?.sourceId
        val salesOrderId = if (source?.sourceType == SalesFlowSource.SalesOrder) sourceId else null
        val deliveryNoteId =
            if (source?.sourceType == SalesFlowSource.DeliveryNote) sourceId else null

        val items = current.cartItems.map { cart ->
            SalesInvoiceItemDto(
                itemCode = cart.itemCode,
                itemName = cart.name,
                qty = cart.quantity,
                rate = cart.price,
                amount = cart.quantity * cart.price,
                discountPercentage = discountPercent,
                warehouse = context.warehouse,
                incomeAccount = context.incomeAccount,
                salesOrder = salesOrderId,
                deliveryNote = deliveryNoteId
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

        return items
    }

    private fun buildInvoiceRemarks(
        current: BillingState.Success,
        paymentLines: List<PaymentLine>,
        shippingAmount: Double,
        baseCurrency: String
    ): String? {
        return buildList {
            current.salesFlowContext?.let { context ->
                val label = context.sourceLabel()
                if (label != null && context.sourceType != SalesFlowSource.Customer) {
                    val sourceText = context.sourceId?.let { "Source: $label (ID: $it)" }
                        ?: "Origen: $label"
                    add(sourceText)
                }
            }
            addAll(
                paymentLines.mapNotNull { line ->
                    if (line.currency.equals(baseCurrency, ignoreCase = true)) null
                    else "Moneda de pago (${line.modeOfPayment}): ${line.currency}, tipo de cambio: ${line.exchangeRate}"
                }
            )
            addAll(
                paymentLines.mapNotNull { line ->
                    line.referenceNumber?.takeIf { it.isNotBlank() }?.let {
                        "Referencia (${line.modeOfPayment}): $it"
                    }
                }
            )
            if (current.discountCode.isNotBlank()) add("Código de descuento: ${current.discountCode}")
            if (shippingAmount > 0.0) add("Envío: $shippingAmount")
        }.joinToString(separator = "; ").takeIf { it.isNotBlank() }
    }

    private suspend fun resolveInvoiceConversionRate(
        invoiceCurrency: String,
        receivableCurrency: String,
        context: POSContext
    ): Double? {
        val invoice = normalizeCurrency(invoiceCurrency) ?: return null
        val receivable = normalizeCurrency(receivableCurrency) ?: return null
        if (invoice.equals(receivable, ignoreCase = true)) return 1.0

        val ctxCurrency = normalizeCurrency(context.currency)
        val ctxRate = context.exchangeRate
        if (ctxCurrency != null && ctxRate > 0.0) {
            if (invoice.equals(ctxCurrency, true) && receivable.equals("USD", true)) {
                return 1 / ctxRate
            }
            if (invoice.equals("USD", true) && receivable.equals(ctxCurrency, true)) {
                return ctxRate
            }
        }

        return contextProvider.resolveExchangeRateBetween(invoice, receivable)
    }

    private fun resolveDueDate(
        isCreditSale: Boolean,
        postingDate: String,
        term: PaymentTermBO?
    ): String {
        if (!isCreditSale) return postingDate
        val resolvedTerm = term ?: error("El término de pago es obligatorio para ventas a crédito.")
        val withMonths = DateTimeProvider.addMonths(postingDate, resolvedTerm.creditMonths ?: 0)
        return DateTimeProvider.addDays(withMonths, resolvedTerm.creditDays ?: 0)
    }

    private fun buildPaymentSchedule(
        isCreditSale: Boolean,
        term: PaymentTermBO?,
        dueDate: String
    ): List<SalesInvoicePaymentScheduleDto> {
        if (!isCreditSale) return emptyList()
        val resolvedTerm = term ?: error("El término de pago es obligatorio para ventas a crédito.")
        return listOf(
            SalesInvoicePaymentScheduleDto(
                paymentTerm = resolvedTerm.name,
                invoicePortion = resolvedTerm.invoicePortion ?: 100.0,
                dueDate = dueDate,
                modeOfPayment = resolvedTerm.modeOfPayment
            )
        )
    }


    /*private fun buildPaymentModeDetailMap(
        definitions: List<ModeOfPaymentEntity>
    ): Map<String, ModeOfPaymentEntity> {
        val map = mutableMapOf<String, ModeOfPaymentEntity>()
        definitions.forEach { definition ->
            map[definition.modeOfPayment] = definition
            map[definition.name] = definition
        }
        return map
    }*/

    private fun validateFinalizeSale(current: BillingState.Success): String? {
        if (current.selectedCustomer == null) return "Selecciona un cliente antes de finalizar la venta."
        if (current.cartItems.isEmpty()) return "Agrega al menos un artículo al carrito."
        /*if (!current.isCreditSale && current.paidAmountBase < current.total) {
            return "El monto pagado debe cubrir el total antes de finalizar la venta."
        }*/
        if (current.isCreditSale && current.selectedPaymentTerm == null)
            return "Selecciona un término de pago para finalizar una venta a crédito."

        // No crédito: debe pagar todo
        val total = roundToCurrency(current.total)
        val paid = roundToCurrency(current.paidAmountBase)
        val tolerance = 0.01

        if (!current.isCreditSale && paid + tolerance < total)
            return "El monto pagado debe cubrir el total antes de finalizar la venta."

        // Crédito: puede pagar parcial, pero no exceder total
        if (current.isCreditSale && paid > total + tolerance)
            return "El pago no puede exceder el total en una venta a crédito."

        /*if (current.isCreditSale && current.paymentLines.isNotEmpty()) {
            return "Las ventas a crédito no pueden incluir líneas de pago."
        }*/

        if (!current.isCreditSale && current.paymentLines.isEmpty())
            return "Agrega al menos un pago o marca la venta como crédito."

        return null
    }
}
