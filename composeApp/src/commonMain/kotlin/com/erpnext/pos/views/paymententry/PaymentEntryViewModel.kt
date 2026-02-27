package com.erpnext.pos.views.paymententry

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.usecases.CreateInternalTransferInput
import com.erpnext.pos.domain.usecases.CreateInternalTransferUseCase
import com.erpnext.pos.domain.usecases.CreatePaymentOutInput
import com.erpnext.pos.domain.usecases.CreatePaymentOutUseCase
import com.erpnext.pos.domain.usecases.FetchSupplierOutstandingPurchaseInvoicesUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersLocalUseCase
import com.erpnext.pos.domain.usecases.FetchSuppliersLocalUseCase
import com.erpnext.pos.domain.usecases.FetchCompanyAccountsLocalUseCase
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentInput
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentUseCase
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.dto.InternalTransferCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryReferenceCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentOutCreateDto
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.view.DateTimeProvider
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PaymentEntryViewModel(
    private val registerPaymentUseCase: RegisterInvoicePaymentUseCase,
    private val navManager: NavigationManager,
    private val cashBoxManager: CashBoxManager,
    private val fetchCustomersLocalUseCase: FetchCustomersLocalUseCase,
    private val fetchSuppliersLocalUseCase: FetchSuppliersLocalUseCase,
    private val fetchSupplierOutstandingPurchaseInvoicesUseCase: FetchSupplierOutstandingPurchaseInvoicesUseCase,
    private val fetchCompanyAccountsLocalUseCase: FetchCompanyAccountsLocalUseCase,
    private val createPaymentOutUseCase: CreatePaymentOutUseCase,
    private val createInternalTransferUseCase: CreateInternalTransferUseCase,
    private val networkMonitor: NetworkMonitor,
    private val generalPreferences: GeneralPreferences
) : BaseViewModel() {
    private val json = Json { ignoreUnknownKeys = true }
    private val amountDraftByType = mutableMapOf<PaymentEntryType, String>()
    private val referenceDateDraftByType = mutableMapOf<PaymentEntryType, String>()

    private val _state = MutableStateFlow(PaymentEntryState())
    val state: StateFlow<PaymentEntryState> = _state
    private var accountToMode: Map<String, String> = emptyMap()
    private var modeToAccount: Map<String, String> = emptyMap()
    private var modeToCurrency: Map<String, String> = emptyMap()

    init {
        resetTypeDrafts()
        _state.update { state -> state.copy(referenceDate = draftReferenceDateFor(state.entryType)) }
        observeOnlinePolicy()
        loadAccountingDefaults()
    }

    private fun defaultReferenceDate(): String = DateTimeProvider.todayDate()

    private fun resetTypeDrafts() {
        PaymentEntryType.entries.forEach { type ->
            amountDraftByType[type] = ""
            referenceDateDraftByType[type] = defaultReferenceDate()
        }
    }

    private fun draftAmountFor(type: PaymentEntryType): String =
        amountDraftByType[type].orEmpty()

    private fun draftReferenceDateFor(type: PaymentEntryType): String =
        referenceDateDraftByType[type]?.takeIf { it.isNotBlank() } ?: defaultReferenceDate()

    fun resetFormState() {
        val current = _state.value
        resetTypeDrafts()
        _state.value = PaymentEntryState(
            entryType = PaymentEntryType.Receive,
            currencyCode = current.currencyCode,
            expenseAccount = current.expenseAccount,
            defaultReceivableAccount = current.defaultReceivableAccount,
            availableModes = current.availableModes,
            accountOptions = current.accountOptions,
            partyOptions = current.partyOptions,
            supplierPendingInvoices = emptyList(),
            isOnline = current.isOnline,
            offlineModeEnabled = current.offlineModeEnabled,
            referenceDate = draftReferenceDateFor(PaymentEntryType.Receive)
        )
    }

    private fun observeOnlinePolicy() {
        viewModelScope.launch {
            combine(
                networkMonitor.isConnected,
                generalPreferences.offlineMode
            ) { isOnline, offlineMode ->
                isOnline to offlineMode
            }.collect { (isOnline, offlineMode) ->
                _state.update {
                    it.copy(
                        isOnline = isOnline,
                        offlineModeEnabled = offlineMode
                    )
                }
            }
        }
    }

    private fun loadAccountingDefaults() {
        executeUseCase(
            action = {
                val context = cashBoxManager.getContext() ?: cashBoxManager.initializeContext()
                val modes = context?.paymentModes
                    ?.mapNotNull { it.modeOfPayment.takeIf(String::isNotBlank) }
                    ?.distinct()
                    .orEmpty()

                val accountPairs = context?.paymentModes.orEmpty()
                    .mapNotNull { option ->
                        val account = option.account?.trim().orEmpty()
                        val mode = option.modeOfPayment.trim()
                        if (account.isBlank() || mode.isBlank()) null else account to mode
                    }
                    .distinctBy { it.first.lowercase() }
                accountToMode = accountPairs.toMap()
                modeToAccount = accountPairs.associate { it.second to it.first }
                modeToCurrency = context?.paymentModes.orEmpty()
                    .mapNotNull { option ->
                        val mode = option.modeOfPayment.trim()
                        val currency = option.currency?.trim().orEmpty()
                        if (mode.isBlank() || currency.isBlank()) null else mode to currency
                    }
                    .toMap()

                val suppliers = fetchSuppliersLocalUseCase()
                val parties = suppliers.ifEmpty {
                    fetchCustomersLocalUseCase(null).firstOrNull()
                        .orEmpty()
                        .map { it.customerName.ifBlank { it.name } }
                        .distinct()
                        .sorted()
                }

                val companyAccounts = fetchCompanyAccountsLocalUseCase()
                val accountOptions = (accountPairs.map { it.first } + companyAccounts)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                _state.update {
                    it.copy(
                        currencyCode = context?.currency ?: "USD",
                        expenseAccount = context?.expenseAccount.orEmpty(),
                        defaultReceivableAccount = context?.defaultReceivableAccount.orEmpty(),
                        availableModes = modes,
                        accountOptions = accountOptions,
                        partyOptions = parties
                    )
                }
            },
            exceptionHandler = {},
            showLoading = false
        )
    }

    fun setEntryType(entryType: PaymentEntryType) {
        _state.update {
            it.copy(
                entryType = entryType,
                amount = draftAmountFor(entryType),
                referenceDate = draftReferenceDateFor(entryType),
                invoiceId = if (entryType == PaymentEntryType.Receive) it.invoiceId else "",
                sourceAccount = if (entryType == PaymentEntryType.InternalTransfer) it.sourceAccount else "",
                targetAccount = if (entryType == PaymentEntryType.InternalTransfer) it.targetAccount else "",
                supplierPendingInvoices = if (entryType == PaymentEntryType.Pay) it.supplierPendingInvoices else emptyList(),
                supplierInvoicesLoading = if (entryType == PaymentEntryType.Pay) it.supplierInvoicesLoading else false,
                supplierInvoicesError = if (entryType == PaymentEntryType.Pay) it.supplierInvoicesError else null,
                referenceNoError = null,
                referenceDateError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun setInvoiceId(invoiceId: String?) {
        _state.update { it.copy(invoiceId = invoiceId?.trim().orEmpty()) }
    }

    fun onInvoiceIdChanged(value: String) {
        _state.update { it.copy(invoiceId = value) }
    }

    fun onModeOfPaymentChanged(value: String) {
        val normalized = value.trim()
        val paymentCurrency = modeToCurrency[normalized]
            ?.takeIf { it.isNotBlank() }
            ?: _state.value.currencyCode
        val inferredAccount = modeToAccount[normalized].orEmpty()
        _state.update {
            it.copy(
                modeOfPayment = value,
                currencyCode = paymentCurrency,
                sourceAccount = if (it.entryType == PaymentEntryType.Pay && inferredAccount.isNotBlank()) {
                    inferredAccount
                } else {
                    it.sourceAccount
                },
                supplierInvoicesError = null
            )
        }
        val current = _state.value
        if (current.entryType == PaymentEntryType.Pay && current.supplierPendingInvoices.isNotEmpty()) {
            refreshSupplierInvoiceConversions(
                invoices = current.supplierPendingInvoices,
                paymentCurrency = current.currencyCode,
                amountText = current.amount
            )
        }
    }

    fun onTargetModeOfPaymentChanged(value: String) {
        _state.update { it.copy(targetModeOfPayment = value) }
    }

    fun onSourceAccountChanged(value: String) {
        val normalized = value.trim()
        val inferredMode = accountToMode[normalized].orEmpty()
        if (_state.value.entryType == PaymentEntryType.Pay && inferredMode.isNotBlank()) {
            onModeOfPaymentChanged(inferredMode)
            _state.update { state ->
                state.copy(
                    sourceAccount = value,
                    modeOfPayment = inferredMode,
                    errorMessage = null
                )
            }
        } else {
            _state.update { it.copy(sourceAccount = value, errorMessage = null) }
        }
    }

    fun onTargetAccountChanged(value: String) {
        _state.update { it.copy(targetAccount = value) }
    }

    fun onAmountChanged(value: String) {
        amountDraftByType[_state.value.entryType] = value
        _state.update {
            val updated = it.copy(amount = value, errorMessage = null)
            if (updated.entryType == PaymentEntryType.Pay) {
                updated.copy(
                    supplierPendingInvoices = reallocateSupplierInvoices(
                        updated.supplierPendingInvoices,
                        value
                    )
                )
            } else {
                updated
            }
        }
    }

    fun onConceptChanged(value: String) {
        _state.update { it.copy(concept = value) }
    }

    fun onPartyChanged(value: String) {
        _state.update {
            it.copy(
                party = value,
                supplierPendingInvoices = emptyList(),
                supplierInvoicesLoading = false,
                supplierInvoicesError = null
            )
        }
        if (_state.value.entryType == PaymentEntryType.Pay) {
            loadSupplierOutstandingInvoices(value)
        }
    }

    fun onSupplierInvoiceToggled(invoiceName: String) {
        _state.update {
            val updatedList = it.supplierPendingInvoices.map { row ->
                if (row.invoiceName != invoiceName) row else row.copy(selected = !row.selected)
            }
            it.copy(
                supplierPendingInvoices = reallocateSupplierInvoices(updatedList, it.amount),
                supplierInvoicesError = null,
                errorMessage = null
            )
        }
    }

    fun onReferenceNoChanged(value: String) {
        _state.update { it.copy(referenceNo = value, referenceNoError = null, errorMessage = null) }
    }

    fun onReferenceDateChanged(value: String) {
        referenceDateDraftByType[_state.value.entryType] = value
        _state.update { it.copy(referenceDate = value, referenceDateError = null, errorMessage = null) }
    }

    fun onNotesChanged(value: String) {
        _state.update { it.copy(notes = value) }
    }

    fun onSubmit() {
        val current = _state.value

        if (current.offlineModeEnabled || !current.isOnline) {
            val message = when {
                current.offlineModeEnabled && !current.isOnline ->
                    "Gastos y transferencias requieren conexión. Desactiva Modo offline y reconecta Internet."

                current.offlineModeEnabled ->
                    "Gastos y transferencias requieren conexión. Desactiva Modo offline en Configuraciones."

                else ->
                    "Sin conexión a Internet. Conéctate para registrar gastos o transferencias."
            }
            _state.update { it.copy(errorMessage = message) }
            return
        }

        val amount = current.amount.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(errorMessage = "Ingresa un monto válido.") }
            return
        }

        val sourceMode: String
        val targetMode: String

        if (current.entryType == PaymentEntryType.InternalTransfer) {
            val sourceAccount = current.sourceAccount.trim()
            val targetAccount = current.targetAccount.trim()
            if (sourceAccount.isBlank()) {
                _state.update { it.copy(errorMessage = "Selecciona la cuenta origen.") }
                return
            }
            if (targetAccount.isBlank()) {
                _state.update { it.copy(errorMessage = "Selecciona la cuenta destino.") }
                return
            }
            if (targetAccount.equals(sourceAccount, ignoreCase = true)) {
                _state.update { it.copy(errorMessage = "Origen y destino deben ser distintos.") }
                return
            }

            sourceMode = accountToMode[sourceAccount].orEmpty()
            targetMode = accountToMode[targetAccount].orEmpty()
            if (sourceMode.isBlank() || targetMode.isBlank()) {
                _state.update {
                    it.copy(
                        errorMessage = "No se pudo mapear la cuenta a un modo de pago. Verifica métodos de pago del perfil POS."
                    )
                }
                return
            }
        } else {
            targetMode = current.targetModeOfPayment.trim()
            if (current.entryType == PaymentEntryType.Pay) {
                val selectedAccount = current.sourceAccount.trim()
                if (selectedAccount.isBlank()) {
                    _state.update {
                        it.copy(
                            errorMessage = "Selecciona la cuenta de pago (Paid From)."
                        )
                    }
                    return
                }
                sourceMode = accountToMode[selectedAccount].orEmpty()
                if (sourceMode.isBlank()) {
                    _state.update {
                        it.copy(
                            errorMessage = "La cuenta seleccionada no tiene método de pago asociado."
                        )
                    }
                    return
                }
            } else {
                sourceMode = current.modeOfPayment.trim()
                if (sourceMode.isBlank()) {
                    _state.update { it.copy(errorMessage = "Selecciona o ingresa el modo de pago.") }
                    return
                }
            }
        }

        if (current.entryType == PaymentEntryType.Receive && current.invoiceId.isBlank()) {
            _state.update { it.copy(errorMessage = "La entrada solo está permitida para cobro de factura.") }
            return
        }
        if (current.entryType == PaymentEntryType.Pay && current.party.isBlank()) {
            _state.update { it.copy(errorMessage = "Selecciona un proveedor para el gasto.") }
            return
        }
        val payFromAccount = if (current.entryType == PaymentEntryType.Pay) {
            current.sourceAccount.trim()
        } else {
            ""
        }
        if (current.entryType == PaymentEntryType.Pay && payFromAccount.isBlank()) {
            _state.update {
                it.copy(errorMessage = "Selecciona una cuenta de pago válida para registrar el gasto.")
            }
            return
        }

        val selectedSupplierReferences = if (current.entryType == PaymentEntryType.Pay) {
            current.supplierPendingInvoices.filter { it.selected && it.allocatedAmountInvoiceCurrency > 0.0 }
        } else {
            emptyList()
        }
        val selectedSupplierRows = if (current.entryType == PaymentEntryType.Pay) {
            current.supplierPendingInvoices.filter { it.selected }
        } else {
            emptyList()
        }
        if (current.entryType == PaymentEntryType.Pay &&
            current.supplierPendingInvoices.isNotEmpty() &&
            selectedSupplierReferences.isEmpty()
        ) {
            _state.update {
                it.copy(
                    errorMessage = if (selectedSupplierRows.any { row -> row.conversionError }) {
                        "No se pudo convertir las facturas seleccionadas. Verifica la tasa de cambio."
                    } else {
                        "Selecciona al menos una factura pendiente del proveedor."
                    }
                )
            }
            return
        }
        if (current.entryType == PaymentEntryType.Pay && selectedSupplierReferences.isNotEmpty()) {
            if (selectedSupplierReferences.any { it.conversionError }) {
                _state.update {
                    it.copy(errorMessage = "No se pudo convertir una o más facturas a la moneda de la cuenta de pago.")
                }
                return
            }
            val selectedOutstandingPaymentCurrency = selectedSupplierRows
                .filterNot { it.conversionError }
                .sumOf(::resolveOutstandingInPaymentCurrency)
            if (amount + 0.009 < selectedOutstandingPaymentCurrency) {
                _state.update {
                    it.copy(
                        errorMessage = "El monto debe ser igual o mayor al saldo adeudado de las facturas seleccionadas."
                    )
                }
                return
            }
        }
        val selectedOutstandingPaymentCurrency = selectedSupplierRows
            .filterNot { it.conversionError }
            .sumOf(::resolveOutstandingInPaymentCurrency)
        val changeInFavor = if (
            current.entryType == PaymentEntryType.Pay &&
            selectedSupplierReferences.isNotEmpty() &&
            amount > selectedOutstandingPaymentCurrency
        ) {
            roundMoney(amount - selectedOutstandingPaymentCurrency)
        } else {
            0.0
        }

        _state.update {
            it.copy(
                isSubmitting = true,
                referenceNoError = null,
                referenceDateError = null,
                errorMessage = null,
                successMessage = null
            )
        }

        executeUseCase(
            action = {
                val narration = buildNarration(current)
                when (current.entryType) {
                    PaymentEntryType.Receive -> {
                        if (current.invoiceId.isNotBlank()) {
                            registerPaymentUseCase(
                                RegisterInvoicePaymentInput(
                                    invoiceId = current.invoiceId,
                                    modeOfPayment = sourceMode,
                                    amount = amount
                                )
                            )
                        } else {
                            cashBoxManager.registerCashMovement(
                                modeOfPayment = sourceMode,
                                amount = amount,
                                isIncoming = true,
                                note = narration
                            )
                        }
                    }

                    PaymentEntryType.Pay -> {
                        val paidFromAccount = payFromAccount
                            .ifBlank { modeToAccount[sourceMode].orEmpty() }
                        if (paidFromAccount.isBlank()) {
                            error("No se encontró la cuenta contable para el modo de pago seleccionado.")
                        }
                        val context =
                            cashBoxManager.getContext() ?: cashBoxManager.initializeContext()
                            ?: error("No hay contexto POS activo.")
                        val availableFunds = cashBoxManager.getAvailableFundsForMode(sourceMode)
                        if (availableFunds + 0.009 < amount) {
                            error(
                                "Fondos insuficientes para $sourceMode. Disponible: ${
                                    roundMoney(
                                        availableFunds
                                    )
                                } ${current.currencyCode}."
                            )
                        }
                        val fromCurrency = modeToCurrency[sourceMode]
                            ?.takeIf { it.isNotBlank() }
                            ?: context.companyCurrency
                        val toCurrency = context.companyCurrency
                        val rate = if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
                            1.0
                        } else {
                            cashBoxManager.resolveExchangeRateBetween(
                                fromCurrency = fromCurrency,
                                toCurrency = toCurrency,
                                allowNetwork = true
                            )
                                ?: error("No se pudo obtener tipo de cambio $fromCurrency -> $toCurrency.")
                        }
                        val receivedAmount = amount * rate
                        val payload = PaymentOutCreateDto(
                            paymentType = "Pay",
                            partyType = "Supplier",
                            party = current.party.trim(),
                            company = context.company,
                            postingDate = DateTimeProvider.todayDate(),
                            modeOfPayment = sourceMode,
                            paidAmount = amount,
                            receivedAmount = receivedAmount,
                            paidFrom = paidFromAccount,
                            references = selectedSupplierReferences.map { invoice ->
                                PaymentEntryReferenceCreateDto(
                                    referenceDoctype = "Purchase Invoice",
                                    referenceName = invoice.invoiceName,
                                    totalAmount = invoice.totalAmountInvoiceCurrency.takeIf { it > 0.0 },
                                    outstandingAmount = invoice.outstandingAmountInvoiceCurrency.takeIf { it > 0.0 },
                                    allocatedAmount = invoice.allocatedAmountInvoiceCurrency
                                )
                            },
                            referenceNo = current.referenceNo.trim().takeIf { it.isNotBlank() },
                            referenceDate = current.referenceDate.trim().takeIf { it.isNotBlank() }
                        )
                        val requestId = UUIDGenerator().newId()
                        createPaymentOutUseCase(
                            CreatePaymentOutInput(
                                clientRequestId = requestId,
                                payload = payload
                            )
                        )
                        cashBoxManager.registerSupplierPaymentOutflow(
                            modeOfPayment = sourceMode,
                            amount = amount,
                            note = narration
                        )
                    }

                    PaymentEntryType.InternalTransfer -> {
                        val context =
                            cashBoxManager.getContext() ?: cashBoxManager.initializeContext()
                            ?: error("No hay contexto POS activo.")
                        val paidFromAccount = current.sourceAccount.trim()
                        val paidToAccount = current.targetAccount.trim()
                        val fromCurrency = modeToCurrency[sourceMode]
                            ?.takeIf { it.isNotBlank() }
                            ?: context.companyCurrency
                        val toCurrency = modeToCurrency[targetMode]
                            ?.takeIf { it.isNotBlank() }
                            ?: context.companyCurrency
                        val rate = if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
                            1.0
                        } else {
                            cashBoxManager.resolveExchangeRateBetween(
                                fromCurrency = fromCurrency,
                                toCurrency = toCurrency,
                                allowNetwork = true
                            )
                                ?: error("No se pudo obtener tipo de cambio $fromCurrency -> $toCurrency.")
                        }
                        val receivedAmount = amount * rate
                        val referenceNo = current.referenceNo.trim().takeIf { it.isNotBlank() }
                            ?: "TR-${Clock.System.now().toEpochMilliseconds()}"
                        val requestId = UUIDGenerator().newId()
                        createInternalTransferUseCase(
                            CreateInternalTransferInput(
                                clientRequestId = requestId,
                                payload = InternalTransferCreateDto(
                                    company = context.company,
                                    postingDate = DateTimeProvider.todayDate(),
                                    modeOfPayment = sourceMode,
                                    paidAmount = amount,
                                    receivedAmount = receivedAmount,
                                    paidFrom = paidFromAccount,
                                    paidTo = paidToAccount,
                                    referenceNo = referenceNo,
                                    referenceDate = current.referenceDate.trim().takeIf { it.isNotBlank() }
                                )
                            )
                        )
                        cashBoxManager.registerInternalTransfer(
                            sourceModeOfPayment = sourceMode,
                            targetModeOfPayment = targetMode,
                            amount = amount,
                            note = narration
                        )
                    }
                }
                _state.update {
                    val successText = when (current.entryType) {
                        PaymentEntryType.InternalTransfer ->
                            "Transferencia interna registrada por $amount de ${current.sourceAccount} a ${current.targetAccount}."

                        PaymentEntryType.Pay ->
                            if (selectedSupplierReferences.isNotEmpty()) {
                                if (changeInFavor > 0.0) {
                                    "Pago a proveedor registrado por $amount desde $sourceMode. Vuelto a favor: ${roundMoney(changeInFavor)} ${current.currencyCode}."
                                } else {
                                    "Pago a proveedor registrado por $amount desde $sourceMode."
                                }
                            } else {
                                "Gasto registrado por $amount desde $sourceMode."
                            }

                        PaymentEntryType.Receive ->
                            if (current.invoiceId.isNotBlank()) {
                                "Cobro registrado para factura ${current.invoiceId}."
                            } else {
                                "Entrada registrada por $amount en $sourceMode."
                            }
                    }
                    it.copy(
                        isSubmitting = false,
                        amount = "",
                        targetModeOfPayment = "",
                        sourceAccount = "",
                        targetAccount = "",
                        concept = "",
                        party = if (current.entryType == PaymentEntryType.Pay) "" else it.party,
                        supplierPendingInvoices = if (current.entryType == PaymentEntryType.Pay) emptyList() else it.supplierPendingInvoices,
                        supplierInvoicesLoading = false,
                        supplierInvoicesError = null,
                        referenceNo = "",
                        referenceDate = DateTimeProvider.todayDate(),
                        referenceNoError = null,
                        referenceDateError = null,
                        notes = "",
                        successMessage = successText
                    )
                }
                amountDraftByType[current.entryType] = ""
                referenceDateDraftByType[current.entryType] = DateTimeProvider.todayDate()
            },
            exceptionHandler = { throwable ->
                _state.update {
                    val fallback = when (current.entryType) {
                        PaymentEntryType.InternalTransfer -> "No se pudo registrar la transferencia interna."
                        PaymentEntryType.Pay -> "No se pudo registrar el gasto."
                        PaymentEntryType.Receive ->
                            if (current.invoiceId.isNotBlank()) {
                                "No se pudo registrar el cobro para la factura ${current.invoiceId}."
                            } else {
                                "No se pudo registrar la entrada."
                            }
                    }
                    val fieldErrors = resolveFieldErrors(throwable.message)
                    it.copy(
                        isSubmitting = false,
                        referenceNoError = fieldErrors.referenceNoError,
                        referenceDateError = fieldErrors.referenceDateError,
                        errorMessage = fieldErrors.userMessage ?: throwable.message ?: fallback
                    )
                }
            },
            loadingMessage = if (current.entryType == PaymentEntryType.InternalTransfer) {
                "Registrando transferencia interna..."
            } else if (current.entryType == PaymentEntryType.Pay) {
                "Registrando gasto..."
            } else if (current.invoiceId.isNotBlank()) {
                "Registrando cobro..."
            } else {
                "Registrando entrada..."
            }
        )
    }

    private fun buildNarration(state: PaymentEntryState): String {
        val segments = buildList {
            state.concept.trim().takeIf { it.isNotBlank() }?.let { add("Concepto: $it") }
            state.party.trim().takeIf { it.isNotBlank() }?.let { add("Tercero: $it") }
            state.referenceNo.trim().takeIf { it.isNotBlank() }?.let { add("Ref: $it") }
            state.notes.trim().takeIf { it.isNotBlank() }?.let { add("Nota: $it") }
        }
        return segments.joinToString(" | ")
    }

    private fun loadSupplierOutstandingInvoices(party: String) {
        val supplier = party.trim()
        if (supplier.isBlank()) return
        executeUseCase(
            action = {
                _state.update {
                    it.copy(
                        supplierInvoicesLoading = true,
                        supplierInvoicesError = null,
                        errorMessage = null
                    )
                }
                val invoices = fetchSupplierOutstandingPurchaseInvoicesUseCase(supplier)
                    .filter { it.outstandingAmount > 0.0 && !isSupplierInvoiceClosed(it.status) }
                    .sortedBy { it.postingDate.orEmpty() }
                    .map { dto ->
                        SupplierPendingInvoiceUi(
                            invoiceName = dto.name,
                            status = dto.status.orEmpty(),
                            postingDate = dto.postingDate.orEmpty(),
                            dueDate = dto.dueDate.orEmpty(),
                            invoiceCurrency = dto.currency.orEmpty(),
                            paymentCurrency = _state.value.currencyCode,
                            totalAmountInvoiceCurrency = dto.grandTotal,
                            outstandingAmountInvoiceCurrency = dto.outstandingAmount
                        )
                    }
                val paymentCurrency = _state.value.currencyCode
                val convertedInvoices = enrichSupplierInvoicesWithConversion(invoices, paymentCurrency)
                _state.update { state ->
                    if (state.entryType != PaymentEntryType.Pay || !state.party.trim().equals(supplier, ignoreCase = false)) {
                        state
                    } else {
                        state.copy(
                            supplierInvoicesLoading = false,
                            supplierInvoicesError = null,
                            supplierPendingInvoices = reallocateSupplierInvoices(convertedInvoices, state.amount)
                        )
                    }
                }
            },
            exceptionHandler = { throwable ->
                _state.update { state ->
                    if (state.entryType != PaymentEntryType.Pay) {
                        state
                    } else {
                        state.copy(
                            supplierInvoicesLoading = false,
                            supplierPendingInvoices = emptyList(),
                            supplierInvoicesError = throwable.message ?: "No se pudieron cargar facturas pendientes del proveedor."
                        )
                    }
                }
            },
            showLoading = false
        )
    }

    private fun reallocateSupplierInvoices(
        invoices: List<SupplierPendingInvoiceUi>,
        amountText: String
    ): List<SupplierPendingInvoiceUi> {
        var remaining = amountText.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        return invoices.map { row ->
            if (!row.selected) {
                row.copy(
                    allocatedAmountPaymentCurrency = 0.0,
                    allocatedAmountInvoiceCurrency = 0.0
                )
            } else if (row.conversionError) {
                row.copy(
                    allocatedAmountPaymentCurrency = 0.0,
                    allocatedAmountInvoiceCurrency = 0.0
                )
            } else {
                val rate = row.paymentToInvoiceRate ?: 1.0
                val maxPaymentAmount = row.outstandingAmountPaymentCurrency
                    ?: if (rate > 0.0) row.outstandingAmountInvoiceCurrency / rate else 0.0
                val allocatedPayment = minOf(remaining, maxPaymentAmount.coerceAtLeast(0.0))
                val allocatedInvoice = (allocatedPayment * rate)
                    .coerceAtMost(row.outstandingAmountInvoiceCurrency.coerceAtLeast(0.0))
                remaining = (remaining - allocatedPayment).coerceAtLeast(0.0)
                row.copy(
                    allocatedAmountPaymentCurrency = roundMoney(allocatedPayment),
                    allocatedAmountInvoiceCurrency = roundMoney(allocatedInvoice)
                )
            }
        }
    }

    private fun refreshSupplierInvoiceConversions(
        invoices: List<SupplierPendingInvoiceUi>,
        paymentCurrency: String,
        amountText: String
    ) {
        executeUseCase(
            action = {
                _state.update { it.copy(supplierInvoicesLoading = true, supplierInvoicesError = null) }
                val converted = enrichSupplierInvoicesWithConversion(invoices, paymentCurrency)
                _state.update { state ->
                    state.copy(
                        supplierInvoicesLoading = false,
                        supplierInvoicesError = null,
                        supplierPendingInvoices = reallocateSupplierInvoices(converted, amountText)
                    )
                }
            },
            exceptionHandler = { throwable ->
                _state.update {
                    it.copy(
                        supplierInvoicesLoading = false,
                        supplierInvoicesError = throwable.message
                            ?: "No se pudieron recalcular conversiones de facturas."
                    )
                }
            },
            showLoading = false
        )
    }

    private suspend fun enrichSupplierInvoicesWithConversion(
        invoices: List<SupplierPendingInvoiceUi>,
        paymentCurrency: String
    ): List<SupplierPendingInvoiceUi> {
        return invoices.map { row ->
            val invoiceCurrency = row.invoiceCurrency.ifBlank { paymentCurrency }
            if (paymentCurrency.isBlank() || invoiceCurrency.equals(paymentCurrency, ignoreCase = true)) {
                row.copy(
                    paymentCurrency = paymentCurrency,
                    paymentToInvoiceRate = 1.0,
                    outstandingAmountPaymentCurrency = roundMoney(row.outstandingAmountInvoiceCurrency),
                    conversionError = false
                )
            } else {
                val rate = resolveRateBetweenCurrencies(
                    fromCurrency = paymentCurrency,
                    toCurrency = invoiceCurrency
                )
                if (rate == null || rate <= 0.0) {
                    row.copy(
                        paymentCurrency = paymentCurrency,
                        paymentToInvoiceRate = null,
                        outstandingAmountPaymentCurrency = null,
                        conversionError = true
                    )
                } else {
                    row.copy(
                        paymentCurrency = paymentCurrency,
                        paymentToInvoiceRate = rate,
                        outstandingAmountPaymentCurrency = roundMoney(
                            row.outstandingAmountInvoiceCurrency / rate
                        ),
                        conversionError = false
                    )
                }
            }
        }
    }

    private suspend fun resolveRateBetweenCurrencies(
        fromCurrency: String,
        toCurrency: String
    ): Double? {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return 1.0
        val direct = cashBoxManager.resolveExchangeRateBetween(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            allowNetwork = true
        )
        if (direct != null && direct > 0.0) return direct
        val reverse = cashBoxManager.resolveExchangeRateBetween(
            fromCurrency = toCurrency,
            toCurrency = fromCurrency,
            allowNetwork = true
        )
        return reverse?.takeIf { it > 0.0 }?.let { 1.0 / it }
    }

    private fun roundMoney(value: Double): Double =
        kotlin.math.round(value * 100.0) / 100.0

    private fun resolveOutstandingInPaymentCurrency(row: SupplierPendingInvoiceUi): Double {
        val paymentOutstanding = row.outstandingAmountPaymentCurrency
        if (paymentOutstanding != null) return paymentOutstanding.coerceAtLeast(0.0)

        val rate = row.paymentToInvoiceRate
        if (rate != null && rate > 0.0) {
            return (row.outstandingAmountInvoiceCurrency / rate).coerceAtLeast(0.0)
        }
        return row.outstandingAmountInvoiceCurrency.coerceAtLeast(0.0)
    }

    private fun isSupplierInvoiceClosed(status: String?): Boolean {
        val normalized = status?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return false
        return normalized == "paid" || normalized == "cancelled" || normalized == "canceled"
    }

    private data class PaymentEntryFieldErrors(
        val referenceNoError: String? = null,
        val referenceDateError: String? = null,
        val userMessage: String? = null
    )

    private fun resolveFieldErrors(rawMessage: String?): PaymentEntryFieldErrors {
        val text = rawMessage?.trim().orEmpty()
        if (text.isBlank()) return PaymentEntryFieldErrors()

        val extractedMessage = runCatching {
            val root = json.parseToJsonElement(text).jsonObject
            root["message"]?.jsonObject
                ?.get("error")?.jsonObject
                ?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull()?.trim().orEmpty()

        val effective = extractedMessage.ifBlank { text }
        val normalized = effective.lowercase()
        val mentionsReferenceNo = normalized.contains("nro de referencia") || normalized.contains("numero de referencia")
        val mentionsReferenceDate = normalized.contains("fecha de referencia")

        if (!mentionsReferenceNo && !mentionsReferenceDate) {
            return PaymentEntryFieldErrors(userMessage = extractedMessage.ifBlank { null })
        }

        return PaymentEntryFieldErrors(
            referenceNoError = if (mentionsReferenceNo) "Número de referencia requerido para transacción bancaria." else null,
            referenceDateError = if (mentionsReferenceDate) "Fecha de referencia requerida para transacción bancaria." else null,
            userMessage = extractedMessage.ifBlank { effective }
        )
    }

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }
}
