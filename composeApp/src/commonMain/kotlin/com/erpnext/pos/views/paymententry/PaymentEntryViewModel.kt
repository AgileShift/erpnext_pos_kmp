package com.erpnext.pos.views.paymententry

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.usecases.CreateInternalTransferInput
import com.erpnext.pos.domain.usecases.CreateInternalTransferUseCase
import com.erpnext.pos.domain.usecases.CreatePaymentOutInput
import com.erpnext.pos.domain.usecases.CreatePaymentOutUseCase
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
        _state.update { it.copy(modeOfPayment = value) }
    }

    fun onTargetModeOfPaymentChanged(value: String) {
        _state.update { it.copy(targetModeOfPayment = value) }
    }

    fun onSourceAccountChanged(value: String) {
        _state.update { it.copy(sourceAccount = value) }
    }

    fun onTargetAccountChanged(value: String) {
        _state.update { it.copy(targetAccount = value) }
    }

    fun onAmountChanged(value: String) {
        amountDraftByType[_state.value.entryType] = value
        _state.update { it.copy(amount = value, errorMessage = null) }
    }

    fun onConceptChanged(value: String) {
        _state.update { it.copy(concept = value) }
    }

    fun onPartyChanged(value: String) {
        _state.update { it.copy(party = value) }
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
            sourceMode = current.modeOfPayment.trim()
            targetMode = current.targetModeOfPayment.trim()
            if (sourceMode.isBlank()) {
                _state.update { it.copy(errorMessage = "Selecciona o ingresa el modo de pago.") }
                return
            }
        }

        if (current.entryType == PaymentEntryType.Receive && current.invoiceId.isBlank()) {
            _state.update { it.copy(errorMessage = "La entrada solo está permitida para cobro de factura.") }
            return
        }
        if (current.entryType == PaymentEntryType.Pay && current.concept.isBlank()) {
            _state.update { it.copy(errorMessage = "El concepto del gasto es requerido.") }
            return
        }
        if (current.entryType == PaymentEntryType.Pay && current.party.isBlank()) {
            _state.update { it.copy(errorMessage = "Selecciona un proveedor para el gasto.") }
            return
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
                        val paidFromAccount = modeToAccount[sourceMode].orEmpty()
                        if (paidFromAccount.isBlank()) {
                            error("No se encontró la cuenta contable para el modo de pago seleccionado.")
                        }
                        val context =
                            cashBoxManager.getContext() ?: cashBoxManager.initializeContext()
                            ?: error("No hay contexto POS activo.")
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
                        cashBoxManager.registerCashMovement(
                            modeOfPayment = sourceMode,
                            amount = amount,
                            isIncoming = false,
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
                            "Gasto registrado por $amount desde $sourceMode."

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
