package com.erpnext.pos.views.billing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.usecases.FetchBillingProductsWithPriceUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchInventoryItemUseCase
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingViewModel(
    val customersUseCase: FetchCustomersUseCase,
    val itemsUseCase: FetchBillingProductsWithPriceUseCase,
    val contextProvider: CashBoxManager
) : BaseViewModel() {

    private val _state: MutableStateFlow<BillingState> =
        MutableStateFlow(BillingState.Loading)
    val state = _state.asStateFlow()

    var territory by mutableStateOf<String?>(null)

    var customers by mutableStateOf<List<CustomerBO>>(emptyList())
    var selectedCustomer by mutableStateOf<CustomerBO?>(null)
    var isLoadingCustomers by mutableStateOf(false)

    var products by mutableStateOf<List<ItemBO>>(emptyList())
    var isLoadingItems by mutableStateOf(false)

    var subtotal by mutableStateOf(0.0)
    var tax by mutableStateOf(0.0)
    var discount by mutableStateOf(0.0)

    val total: Double
        get() = subtotal + tax - discount

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        executeUseCase(
            action = {
                customersUseCase.invoke(null)
                    .collectLatest { c ->
                        viewModelScope.launch {
                            itemsUseCase.invoke(null)
                                .collectLatest { i ->
                                    _state.update {
                                        BillingState.Success(
                                            customers = c,
                                            productSearchResults = i,
                                        )
                                    }
                                }
                        }
                    }
                //itemsUseCase.invoke(null).collectLatest { products = it}
            },
            exceptionHandler = {
                _state.value = BillingState.Error(it.message ?: "Unknown error")
            }
        )
    }
}