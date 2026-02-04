@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.billing

import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.domain.usecases.FetchBillingProductsLocalUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersLocalUseCase
import com.erpnext.pos.domain.usecases.FetchDeliveryChargesLocalUseCase
import com.erpnext.pos.domain.usecases.FetchPaymentTermsLocalUseCase
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
import com.erpnext.pos.utils.PaymentStatus
import com.erpnext.pos.utils.resolvePaymentStatus
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.domain.models.BillingTotals
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.utils.buildPaymentModeDetailMap
import com.erpnext.pos.utils.calculateTotals
import com.erpnext.pos.utils.resolveDiscountInfo
import com.erpnext.pos.utils.RoundedTotal
import com.erpnext.pos.utils.resolveRoundedTotal
import com.erpnext.pos.utils.roundForCurrency
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.buildCurrencySpecs
import com.erpnext.pos.views.payment.PaymentHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class BillingViewModel(
    private val customersUseCase: FetchCustomersLocalUseCase,
    private val itemsUseCase: FetchBillingProductsLocalUseCase,
    private val adjustLocalInventoryUseCase: AdjustLocalInventoryUseCase,
    private val contextProvider: CashBoxManager,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val paymentTermsUseCase: FetchPaymentTermsLocalUseCase,
    private val deliveryChargesUseCase: FetchDeliveryChargesLocalUseCase,
    private val navManager: NavigationManager,
    private val salesFlowStore: SalesFlowContextStore,
    private val loadSourceDocumentsUseCase: LoadSourceDocumentsUseCase,
    private val createSalesInvoiceLocalUseCase: CreateSalesInvoiceLocalUseCase,
    private val createSalesInvoiceRemoteOnlyUseCase: CreateSalesInvoiceRemoteOnlyUseCase,
    private val updateLocalInvoiceFromRemoteUseCase: UpdateLocalInvoiceFromRemoteUseCase,
    private val markSalesInvoiceSyncedUseCase: MarkSalesInvoiceSyncedUseCase,
    private val paymentHandler: PaymentHandler,
    private val billingResetController: BillingResetController,
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
        viewModelScope.launch {
            billingResetController.events.collectLatest {
                resetSale()
            }
        }
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
                    val allowNegativeStock = context.allowNegativeStock
                    products = i.filter {
                        it.price > 0.0 && (allowNegativeStock || it.actualQty > 0.0)
                    }

                    val invoiceCurrency = context.currency.trim()
                    val baseCurrency = context.companyCurrency.trim().uppercase()
                        .takeIf { it.isNotBlank() } ?: invoiceCurrency.trim().uppercase()

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
                                allowInReturns = true,
                            )
                        }
                    }

                    // Cache de tasas currency -> invoiceCurrency
                    val exchangeRateByCurrency = buildExchangeRateMap(
                        invoiceCurrency,
                        context.allowedCurrencies,
                        extraCodes = listOf(baseCurrency)
                    )

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
                            baseCurrency = baseCurrency,
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
        val posCurrency = contextProvider.getContext()?.currency
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
                price = resolveItemPriceForInvoiceCurrency(
                    item = item,
                    invoiceCurrency = current.currency ?: posCurrency,
                    posCurrency = posCurrency,
                    exchangeRate = exchangeRate
                )
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

        val invoiceCurrency = normalizeCurrency(current.currency)

        executeUseCase(
            action = {
                val result = paymentHandler.resolvePaymentLine(
                    line = line,
                    invoiceCurrencyInput = invoiceCurrency,
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
        val baseCurrency = normalizeCurrency(current.currency)
        val paymentCurrency = normalizeCurrency(currency)
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
                val rate = resolveRateToInvoiceCurrencyLocal(
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
        if (current.successMessage == null && current.successDialogMessage == null) return
        _state.update {
            current.copy(
                successMessage = null,
                successDialogMessage = null,
                successDialogInvoice = null,
                successDialogId = 0L
            )
        }
    }


    fun onFinalizeSale() {
        viewModelScope.launch {
            val current = requireSuccessState() ?: return@launch
            val validationError = validateFinalizeSale(current)
            if (validationError != null) {
                _state.update { BillingState.Error(validationError, current) }
                return@launch
            }
            setFinalizingSale(true)

            val customer = current.selectedCustomer ?: error("Debes seleccionar un cliente.")
            val context =
                contextProvider.getContext() ?: error("El contexto POS no está inicializado.")

            executeUseCase(action = {
                val invoiceCurrency = context.currency.ifBlank { current.currency ?: "USD" }
                val companyCurrency = context.companyCurrency.ifBlank { invoiceCurrency }
                val rounderCash: (Double) -> Double = { value ->
                    roundForCurrency(value, invoiceCurrency)
                }

                val rawTotals = calculateTotals(current)
                val totals = rawTotals.copy(
                    subtotal = rounderCash(rawTotals.subtotal),
                    taxes = rounderCash(rawTotals.taxes),
                    discount = rounderCash(rawTotals.discount),
                    shipping = rounderCash(rawTotals.shipping),
                    total = rounderCash(rawTotals.total)
                )
                val discountInfo = resolveDiscountInfo(current, totals.subtotal)
                val discountPercent = discountInfo.percent?.takeIf { it > 0.0 }

                val isCreditSale = current.isCreditSale
                val paymentLines = current.paymentLines

                val postingDate = DateTimeProvider.todayDate()
                val dueDate = resolveDueDate(isCreditSale, postingDate, current.selectedPaymentTerm)
                val paymentSchedule =
                    buildPaymentSchedule(isCreditSale, current.selectedPaymentTerm, dueDate)

                val rounding = resolveRoundedTotal(totals.total, invoiceCurrency)
                val paymentStatus = resolvePaymentStatus(
                    total = rounding.roundedTotal,
                    paymentLines = paymentLines,
                    round = ::roundToCurrency
                )
                val currencyKey = normalizeCurrency(invoiceCurrency).uppercase()
                val roundingTolerance = when (currencyKey) {
                    "NIO" -> 1.0
                    "USD" -> 0.01
                    else -> 0.01
                }
                val fullyPaid = paymentLines.isNotEmpty() &&
                    roundToCurrency(paymentStatus.paidAmount) + roundingTolerance >=
                    roundToCurrency(rounding.roundedTotal)
                val usePosInvoice = !isCreditSale && paymentStatus.status == "Paid" && fullyPaid

                val invoiceCurrencyNormalized = normalizeCurrency(invoiceCurrency)
                val companyCurrencyNormalized = normalizeCurrency(companyCurrency)
                val conversionRate = resolveInvoiceConversionRate(
                    invoiceCurrency = invoiceCurrencyNormalized,
                    companyCurrency = companyCurrencyNormalized,
                    context = context
                )
                if (!invoiceCurrencyNormalized.equals(
                        companyCurrencyNormalized,
                        ignoreCase = true
                    ) &&
                    (conversionRate == null || conversionRate == 1.0)
                ) {
                    error("No se pudo resolver la tasa de cambio $invoiceCurrencyNormalized -> $companyCurrencyNormalized.")
                }

                val activeCashbox = runCatching {
                    contextProvider.getActiveCashboxWithDetails()?.cashbox
                }.getOrNull()
                val openingEntryId = activeCashbox?.openingEntryId

                val invoiceDto = buildSalesInvoiceDto(
                    current = current,
                    customer = customer,
                    context = context,
                    totals = totals,
                    discountPercent = discountPercent,
                    discountAmount = discountInfo.amount,
                    paymentSchedule = paymentSchedule,
                    paymentLines = paymentLines,
                    paymentStatus = paymentStatus,
                    invoiceCurrency = invoiceCurrency,
                    rounding = rounding,
                    conversionRate = conversionRate,
                    postingDate = postingDate,
                    dueDate = dueDate,
                    posOpeningEntry = openingEntryId,
                    usePosInvoice = usePosInvoice
                )

                val localInvoiceName = "LOCAL-${UUIDGenerator().newId()}"
                createSalesInvoiceLocalUseCase(
                    CreateSalesInvoiceLocalInput(
                        localInvoiceName = localInvoiceName,
                        invoice = invoiceDto
                    )
                )

                val createdResult = runCatching {
                    createSalesInvoiceRemoteOnlyUseCase(
                        CreateSalesInvoiceRemoteOnlyInput(invoiceDto.copy(name = null))
                    )
                }
                val created = createdResult.getOrNull()
                val remoteErrorMessage = createdResult.exceptionOrNull()
                    ?.toUserMessage("No se pudo sincronizar la factura.")

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
                    posOpeningEntry = openingEntryId
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
                }.filter {
                    val allowNegativeStock =
                        contextProvider.getContext()?.allowNegativeStock == true
                    it.price > 0.0 && (allowNegativeStock || it.actualQty > 0.0)
                }

                val q = current.productSearchQuery
                val refreshedResults = if (q.isBlank()) products else products.filter {
                    it.name.contains(q, ignoreCase = true) || it.itemCode.contains(
                        q,
                        ignoreCase = true
                    )
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
                                if (remoteErrorMessage.isNullOrBlank()) {
                                    "Factura $localInvoiceName guardada localmente (pendiente de sincronizacion)."
                                } else {
                                    "Factura $localInvoiceName guardada localmente (pendiente de sincronizacion). $remoteErrorMessage"
                                }
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
                        successDialogMessage = when {
                            created == null -> "Venta guardada localmente."
                            paymentLines.isNotEmpty() -> "Tu pago se ha realizado con exito"
                            current.isCreditSale -> "Venta a credito registrada"
                            else -> "Venta registrada correctamente"
                        },
                        successDialogInvoice = created?.name ?: localInvoiceName,
                        successDialogId = Clock.System.now().toEpochMilliseconds(),
                        sourceDocument = null,
                        isSourceDocumentApplied = false
                    )
                }
            }, exceptionHandler = { e ->
                // En modo prueba necesitamos mensajes útiles: agregamos contexto del flujo.
                _state.update { currentState ->
                    val previous = currentState as? BillingState.Success
                    val errorMessage = buildFinalizeErrorMessage(previous, e)
                    BillingState.Error(
                        errorMessage,
                        previous,
                        showSyncRates = shouldSuggestRateSync(e)
                    )
                }
            }, finallyHandler = {
                setFinalizingSale(false)
            })
        }
    }

    fun onSyncExchangeRates() {
        val current = _state.value
        val base = (current as? BillingState.Success)
            ?: (current as? BillingState.Error)?.previous
            ?: return
        val ctx = contextProvider.getContext() ?: return
        val invoiceCurrency = normalizeCurrency(base.currency ?: ctx.currency)
        val companyCurrency = normalizeCurrency(ctx.companyCurrency)
        executeUseCase(
            action = {
                val rate = contextProvider.resolveExchangeRateBetween(
                    invoiceCurrency,
                    companyCurrency,
                    allowNetwork = true
                )
                if (!invoiceCurrency.equals(companyCurrency, ignoreCase = true) &&
                    (rate == null || rate <= 0.0 || rate == 1.0)
                ) {
                    error("No se pudo sincronizar la tasa $invoiceCurrency -> $companyCurrency.")
                }
                val updated = base.copy(
                    exchangeRateByCurrency = base.exchangeRateByCurrency
                        .plus(invoiceCurrency to 1.0)
                        .plus(companyCurrency to (rate ?: 1.0))
                )
                _state.value = updated
            },
            exceptionHandler = { e ->
                val message = e.toUserMessage("No se pudo sincronizar tasas de cambio.")
                _state.value = BillingState.Error(message, base, showSyncRates = true)
            }
        )
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

    private fun recalculateTotals(current: BillingState.Success): BillingState.Success {
        val totals = calculateTotals(current)
        return current.copy(
            subtotal = totals.subtotal,
            taxes = totals.taxes,
            discount = totals.discount,
            total = totals.total
        ).recalculatePaymentTotals()
    }

    private fun buildFinalizeErrorMessage(
        current: BillingState.Success?,
        error: Throwable
    ): String {
        // Mensaje base para el usuario.
        val baseMessage = error.toUserMessage("No se pudo crear la factura.")
        // Construimos contexto adicional para identificar el punto del error.
        val sourceInfo = current?.salesFlowContext?.sourceLabel()?.let { label ->
            current.salesFlowContext.sourceId?.let { id -> "$label ($id)" } ?: label
        } ?: "N/A"
        val customerInfo = current?.selectedCustomer?.name ?: "N/A"
        val totalInfo = current?.total?.let { roundToCurrency(it) } ?: 0.0
        val paidInfo = current?.paidAmountBase?.let { roundToCurrency(it) } ?: 0.0
        val linesInfo = current?.paymentLines?.size ?: 0
        val creditInfo = current?.isCreditSale ?: false
        val errorType = error::class.simpleName ?: "Error"
        return buildString {
            append(baseMessage)
            append(" | Tipo: ").append(errorType)
            append(" | Cliente: ").append(customerInfo)
            append(" | Origen: ").append(sourceInfo)
            append(" | Total: ").append(totalInfo)
            append(" | Pagado: ").append(paidInfo)
            append(" | Pagos: ").append(linesInfo)
            append(" | Crédito: ").append(creditInfo)
            if (shouldSuggestRateSync(error)) {
                append(" | Sugerencia: sincroniza tasas de cambio e intenta de nuevo")
            }
        }
    }

    private fun shouldSuggestRateSync(error: Throwable): Boolean {
        val message = error.message ?: return false
        return message.contains("tasa de cambio", ignoreCase = true) ||
                message.contains("exchange rate", ignoreCase = true)
    }

    private suspend fun buildExchangeRateMap(
        baseCurrency: String,
        allowed: List<POSCurrencyOption>,
        extraCodes: List<String> = emptyList()
    ): Map<String, Double> {
        val base = baseCurrency.trim().uppercase()
        val map = mutableMapOf(base to 1.0)
        val allCodes =
            allowed.mapNotNull { it.code.trim().uppercase().takeIf { c -> c.isNotBlank() } }
                .toMutableSet()
        allCodes += "USD" // asegurar USD siempre presente
        extraCodes.forEach { code ->
            val normalized = code.trim().uppercase()
            if (normalized.isNotBlank()) allCodes += normalized
        }

        for (code in allCodes) {
            if (code == base) continue
            val direct =
                contextProvider.resolveExchangeRateBetween(code, base, allowNetwork = false)
            val rate = when {
                direct != null && direct > 0.0 -> direct
                else -> contextProvider.resolveExchangeRateBetween(base, code, allowNetwork = false)
                    ?.takeIf { it > 0.0 }
                    ?.let { 1.0 / it }
            }
            if (rate != null && rate > 0.0) {
                map[code] = rate
            }
        }
        return map
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

        val direct = contextProvider.resolveExchangeRateBetween(from, to, allowNetwork = false)
        if (direct != null && direct > 0.0) return direct
        val reverse = contextProvider.resolveExchangeRateBetween(to, from, allowNetwork = false)
            ?.takeIf { it > 0.0 }?.let { 1.0 / it }
        return reverse ?: error("No se pudo resolver la tasa de cambio $from -> $to")
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

    fun resetSale() {
        val current = requireSuccessState() ?: return
        val reset = resetFromSource(current).copy(
            selectedCustomer = null,
            customerSearchQuery = "",
            productSearchQuery = "",
            salesFlowContext = null,
            successMessage = null,
            successDialogMessage = null,
            successDialogInvoice = null,
            isFinalizingSale = false
        )
        _state.update { reset }
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
        paymentSchedule: List<SalesInvoicePaymentScheduleDto>,
        paymentLines: List<PaymentLine>,
        paymentStatus: PaymentStatus,
        invoiceCurrency: String,
        rounding: RoundedTotal,
        conversionRate: Double?,
        postingDate: String,
        dueDate: String,
        posOpeningEntry: String?,
        usePosInvoice: Boolean
    ): SalesInvoiceDto {
        val items = buildInvoiceItems(current, context, discountPercent, discountAmount)
        val paymentMetadata =
            buildInvoiceRemarks(current, paymentLines, totals.shipping, invoiceCurrency)

        val rawPayments = if (usePosInvoice) {
            val paymentMode = paymentLines.firstOrNull()?.modeOfPayment
                ?: current.paymentModes.firstOrNull()?.modeOfPayment
                ?: error("No hay modo de pago disponible para crear la factura POS.")
            paymentLines.map { line ->
                SalesInvoicePaymentDto(
                    modeOfPayment = line.modeOfPayment.ifBlank { paymentMode },
                    amount = line.baseAmount,
                    paymentReference = line.referenceNumber,
                    account = paymentModeDetails[line.modeOfPayment]?.account
                )
            }
        } else {
            emptyList()
        }
        val resolvedPaid = if (usePosInvoice) {
            roundForCurrency(paymentStatus.paidAmount, invoiceCurrency)
        } else {
            roundToCurrency(paymentStatus.paidAmount)
        }
        val payments = if (usePosInvoice) {
            adjustPosPaymentsToMatchTotal(
                payments = rawPayments,
                targetAmount = resolvedPaid,
                invoiceCurrency = invoiceCurrency
            )
        } else {
            rawPayments
        }
        val resolvedStatus = if (usePosInvoice) "Paid" else paymentStatus.status
        // En ERPNext: paid/outstanding están en moneda de factura (invoice currency).
        val resolvedOutstanding = if (usePosInvoice) {
            0.0
        } else {
            roundToCurrency(paymentStatus.outstandingAmount)
        }
        val resolvedChange = if (usePosInvoice) {
            roundForCurrency(
                (resolvedPaid - rounding.roundedTotal).coerceAtLeast(0.0),
                invoiceCurrency
            )
        } else {
            roundToCurrency((resolvedPaid - rounding.roundedTotal).coerceAtLeast(0.0))
        }

        val hasRounding = kotlin.math.abs(rounding.roundingAdjustment) > 0.0001
        val roundedTotalField = if (hasRounding) rounding.roundedTotal else null
        val roundingAdjustmentField =
            if (hasRounding) rounding.roundingAdjustment else null

        return SalesInvoiceDto(
            customer = customer.name,
            customerName = customer.customerName,
            customerPhone = customer.mobileNo,
            company = context.company,
            postingDate = postingDate,
            currency = invoiceCurrency,
            conversionRate = conversionRate?.takeIf { it > 0.0 },
            partyAccountCurrency = context.partyAccountCurrency,
            dueDate = dueDate,
            status = resolvedStatus,
            grandTotal = totals.total,
            roundedTotal = roundedTotalField,
            roundingAdjustment = roundingAdjustmentField,
            outstandingAmount = if (usePosInvoice) resolvedOutstanding else null,
            totalTaxesAndCharges = totals.taxes,
            netTotal = totals.total,
            paidAmount = if (usePosInvoice) resolvedPaid else null,
            changeAmount = if (usePosInvoice) resolvedChange.takeIf { it > 0.0 } else null,
            items = items,
            payments = payments,
            paymentSchedule = paymentSchedule,
            paymentTerms = if (current.isCreditSale) current.selectedPaymentTerm?.name else null,
            posProfile = context.profileName,
            posOpeningEntry = posOpeningEntry,
            remarks = paymentMetadata,
            customExchangeRate = null,
            updateStock = true,
            docStatus = 0,
            isPos = usePosInvoice,
            doctype = if (usePosInvoice) "POS Invoice" else "Sales Invoice"
        )
    }

    private fun adjustPosPaymentsToMatchTotal(
        payments: List<SalesInvoicePaymentDto>,
        targetAmount: Double,
        invoiceCurrency: String
    ): List<SalesInvoicePaymentDto> {
        if (payments.isEmpty()) return payments
        val totalPaid = payments.sumOf { it.amount }
        val diff = roundForCurrency(targetAmount - totalPaid, invoiceCurrency)
        if (diff == 0.0) return payments

        val specs = buildCurrencySpecs()
        val currency = normalizeCurrency(invoiceCurrency)
        val minorUnits = specs[currency]?.minorUnits ?: 2
        val tolerance = 1.0 / 10.0.pow(minorUnits.toDouble())
        if (kotlin.math.abs(diff) > tolerance) {
            val lastIndex = payments.lastIndex
            val adjustedLast = (targetAmount - payments.dropLast(1).sumOf { it.amount })
                .coerceAtLeast(0.0)
            val updatedLast = payments[lastIndex].copy(
                amount = roundForCurrency(adjustedLast, invoiceCurrency)
            )
            return payments.toMutableList().apply { this[lastIndex] = updatedLast }
        }

        val lastIndex = payments.lastIndex
        val updatedLast = payments[lastIndex].copy(
            amount = roundForCurrency(payments[lastIndex].amount + diff, invoiceCurrency)
        )
        return payments.toMutableList().apply { this[lastIndex] = updatedLast }
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
        companyCurrency: String,
        context: POSContext
    ): Double? {
        return com.erpnext.pos.utils.CurrencyService.resolveInvoiceToReceivableRateUnified(
            invoiceCurrency = invoiceCurrency,
            receivableCurrency = companyCurrency,
            conversionRate = null,
            customExchangeRate = null,
            posCurrency = context.currency,
            posExchangeRate = context.exchangeRate,
            rateResolver = { from, to ->
                contextProvider.resolveExchangeRateBetween(from, to, allowNetwork = false)
            }
        )
    }

    private suspend fun resolveRateToInvoiceCurrencyLocal(
        paymentCurrency: String,
        invoiceCurrency: String,
        cache: Map<String, Double>
    ): Double {
        val pay = normalizeCurrency(paymentCurrency)
        val inv = normalizeCurrency(invoiceCurrency)
        if (pay == inv) return 1.0
        cache[pay]?.takeIf { it > 0.0 }?.let { return it }
        val direct = contextProvider.resolveExchangeRateBetween(pay, inv, allowNetwork = false)
        if (direct != null && direct > 0.0) return direct
        val reverse = contextProvider.resolveExchangeRateBetween(inv, pay, allowNetwork = false)
            ?.takeIf { it > 0.0 }?.let { 1 / it }
        return reverse ?: error("No se pudo resolver tasa $pay -> $inv")
    }

    private fun resolveItemPriceForInvoiceCurrency(
        item: ItemBO,
        invoiceCurrency: String?,
        posCurrency: String?,
        exchangeRate: Double?
    ): Double {
        val itemCurrency = normalizeCurrency(item.currency)
        val invoice = normalizeCurrency(invoiceCurrency)
        if (itemCurrency.isBlank() || invoice.isBlank()) return item.price
        if (itemCurrency.equals(invoice, ignoreCase = true)) return item.price

        val rate = exchangeRate?.takeIf { it > 0.0 } ?: return item.price
        val pos = normalizeCurrency(posCurrency)
        if (pos != null) {
            if (itemCurrency.equals("USD", true) && invoice.equals(pos, true)) {
                return item.price * rate
            }
            if (itemCurrency.equals(pos, true) && invoice.equals("USD", true)) {
                return item.price / rate
            }
        }
        return item.price
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

    private suspend fun validateFinalizeSale(current: BillingState.Success): String? {
        val posContext = runCatching { contextProvider.requireContext() }.getOrNull()
            ?: return "No hay contexto POS activo."
        if (posContext.profileName.isBlank()) {
            return "No hay POS Profile activo."
        }
        val openingEntryId = contextProvider.getActiveCashboxWithDetails()?.cashbox?.openingEntryId
        if (openingEntryId.isNullOrBlank()) {
            return "No hay apertura de caja activa."
        }
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

        current.cartItems.forEach { item ->
            val product = products.firstOrNull { it.itemCode == item.itemCode }
            val available = product?.actualQty ?: 0.0
            val allowNegativeStock = contextProvider.getContext()?.allowNegativeStock == true
            if (!allowNegativeStock && available <= 0.0) {
                return "El artículo ${item.name} no tiene stock disponible."
            }
            if (!allowNegativeStock && item.quantity > available) {
                return buildQtyErrorMessage(item.name, available)
            }
        }

        return null
    }
}
