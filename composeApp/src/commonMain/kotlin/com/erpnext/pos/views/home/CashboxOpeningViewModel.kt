package com.erpnext.pos.views.home

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.PaymentModeWithAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CashboxOpeningProfileState(
    val profileId: String? = null,
    val company: String = "",
    val baseCurrency: String = "USD",
    val methods: List<ResolvedPaymentMethod> = emptyList(),
    val cashMethodsByCurrency: Map<String, List<ResolvedPaymentMethod>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CashboxOpeningViewModel(
    private val posProfileDao: POSProfileDao,
    private val paymentMethodLocalRepository: PosProfilePaymentMethodLocalRepository,
    private val cashBoxManager: CashBoxManager
) : BaseViewModel() {
    private val _state = MutableStateFlow(CashboxOpeningProfileState())
    val state: StateFlow<CashboxOpeningProfileState> = _state.asStateFlow()

    fun loadProfile(profileId: String?) {
        if (profileId.isNullOrBlank()) {
            _state.update { it.copy(profileId = null, methods = emptyList(), cashMethodsByCurrency = emptyMap()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val profile = posProfileDao.getPOSProfile(profileId)
                val methods = paymentMethodLocalRepository.getMethodsForProfile(profileId)
                val cashMethods = paymentMethodLocalRepository.getCashMethodsGroupedByCurrency(profileId)
                _state.update {
                    it.copy(
                        profileId = profile.profileName,
                        company = profile.company,
                        baseCurrency = profile.currency,
                        methods = methods,
                        cashMethodsByCurrency = cashMethods,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { error ->
                AppLogger.warn("CashboxOpeningViewModel loadProfile failed", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load profile data"
                    )
                }
            }
        }
    }

    suspend fun openCashbox(
        entry: POSProfileSimpleBO,
        amounts: List<PaymentModeWithAmount>
    ) {
        cashBoxManager.openCashBox(entry, amounts)
    }
}
