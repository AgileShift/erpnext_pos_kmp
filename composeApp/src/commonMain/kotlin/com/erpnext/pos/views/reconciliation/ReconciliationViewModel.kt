package com.erpnext.pos.views.reconciliation

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.usecases.FetchClosingEntriesUseCase
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReconciliationViewModel(
    private val fetchClosingEntriesUseCase: FetchClosingEntriesUseCase,
    private val cashBoxManager: CashBoxManager
) : BaseViewModel() {
    private val _stateFlow = MutableStateFlow<ReconciliationState>(ReconciliationState.Loading)
    val stateFlow: StateFlow<ReconciliationState> = _stateFlow.asStateFlow()
    private val _closeState = MutableStateFlow(CloseCashboxState())
    val closeState: StateFlow<CloseCashboxState> = _closeState.asStateFlow()

    init {
        observeClosingEntries()
    }

    private fun observeClosingEntries() {
        viewModelScope.launch {
            fetchClosingEntriesUseCase(Unit)
                .catch { error ->
                    _stateFlow.update {
                        ReconciliationState.Error(
                            error.message ?: "Unable to load reconciliation data."
                        )
                    }
                }
                .collectLatest { entries ->
                    if (entries.isEmpty()) {
                        _stateFlow.update { ReconciliationState.Empty }
                    } else {
                        _stateFlow.update {
                            ReconciliationState.Success(entries.map { it.toUi() })
                        }
                    }
                }
        }
    }

    fun closeCashbox() {
        if (_closeState.value.isClosing) return
        _closeState.update { it.copy(isClosing = true, errorMessage = null) }
        executeUseCase(
            action = {
                cashBoxManager.closeCashBox()
                _closeState.update { it.copy(isClosing = false, isClosed = true) }
            },
            exceptionHandler = { error ->
                _closeState.update {
                    it.copy(
                        isClosing = false,
                        errorMessage = error.message ?: "Failed to close the cashbox."
                    )
                }
            }
        )
    }
}

private fun POSClosingEntryEntity.toUi(): ReconciliationSessionUi {
    return ReconciliationSessionUi(
        id = name,
        posProfile = posProfile,
        openingEntry = posOpeningEntry,
        periodStart = periodStartDate,
        periodEnd = periodEndDate,
        closingAmount = closingAmount,
        pendingSync = pendingSync
    )
}
