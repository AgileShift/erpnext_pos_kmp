package com.erpnext.pos.views.paymententry

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.usecases.FetchCustomersLocalUseCase
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentInput
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentUseCase
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentEntryViewModel(
    private val registerPaymentUseCase: RegisterInvoicePaymentUseCase,
    private val navManager: NavigationManager,
    private val cashBoxManager: CashBoxManager,
    private val fetchCustomersLocalUseCase: FetchCustomersLocalUseCase,
    private val networkMonitor: NetworkMonitor,
    private val generalPreferences: GeneralPreferences
) : BaseViewModel() {

    private val _state = MutableStateFlow(PaymentEntryState())
    val state: StateFlow<PaymentEntryState> = _state
    private var accountToMode: Map<String, String> = emptyMap()

    init {
        observeOnlinePolicy()
        loadAccountingDefaults()
    }

    private fun observeOnlinePolicy() {
        viewModelScope.launch {
            combine(networkMonitor.isConnected, generalPreferences.offlineMode) { isOnline, offlineMode ->
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

                val parties = fetchCustomersLocalUseCase(null).firstOrNull()
                    .orEmpty()
                    .map { it.customerName.ifBlank { it.name } }
                    .distinct()
                    .sorted()

                _state.update {
                    it.copy(
                        currencyCode = context?.currency ?: "USD",
                        expenseAccount = context?.expenseAccount.orEmpty(),
                        defaultReceivableAccount = context?.defaultReceivableAccount.orEmpty(),
                        availableModes = modes,
                        accountOptions = accountPairs.map { it.first }.sorted(),
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
                invoiceId = if (entryType == PaymentEntryType.Receive) it.invoiceId else "",
                sourceAccount = if (entryType == PaymentEntryType.InternalTransfer) it.sourceAccount else "",
                targetAccount = if (entryType == PaymentEntryType.InternalTransfer) it.targetAccount else "",
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
        _state.update { it.copy(amount = value) }
    }

    fun onConceptChanged(value: String) {
        _state.update { it.copy(concept = value) }
    }

    fun onPartyChanged(value: String) {
        _state.update { it.copy(party = value) }
    }

    fun onReferenceNoChanged(value: String) {
        _state.update { it.copy(referenceNo = value) }
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

        _state.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

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
                        cashBoxManager.registerCashMovement(
                            modeOfPayment = sourceMode,
                            amount = amount,
                            isIncoming = false,
                            note = narration
                        )
                    }

                    PaymentEntryType.InternalTransfer -> {
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
                        notes = "",
                        successMessage = successText
                    )
                }
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
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: fallback
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

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }
}
