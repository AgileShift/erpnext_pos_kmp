@file:OptIn(FlowPreview::class)

package com.erpnext.pos.views.inventory

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.base.Resource
import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.usecases.FetchCategoriesUseCase
import com.erpnext.pos.domain.usecases.FetchInventoryItemUseCase
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val fetchCategoryUseCase: FetchCategoriesUseCase,
    private val fetchInventoryItemUseCase: FetchInventoryItemUseCase,
    private val context: CashBoxManager,
) : BaseViewModel() {

    private val _stateFlow = MutableStateFlow<InventoryState>(InventoryState.Loading)
    val stateFlow: StateFlow<InventoryState> = _stateFlow.asStateFlow()

    private val searchFilter = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow("Todos los grupos de artículos")

    // Exponemos categorías separadas por si la UI la necesita en otro binding
    private val _categoriesFlow = MutableStateFlow<List<CategoryBO>?>(emptyList())
    val categoriesFlow: StateFlow<List<CategoryBO>?> = _categoriesFlow.asStateFlow()

    init {
        loadCategoriesOnce()
        loadItemsReactive()
    }

    /**
     * Carga categorías usando executeUseCase (como tenías antes).
     * Se asegura que la carga pesada quede en IO dentro del use case/repositorio.
     */
    private fun loadCategoriesOnce() {
        viewModelScope.launch {
            executeUseCase(
                action = {
                    // Suponemos que fetchCategoryUseCase devuelve Flow<Resource<List<String>>>
                    fetchCategoryUseCase.invoke(null)
                        .flowOn(Dispatchers.IO) // fuerza upstream en IO
                        .collect { resource ->
                            when (resource) {
                                is Resource.Success -> {
                                    val list = resource.data ?: emptyList()
                                    _categoriesFlow.value = list
                                }
                                is Resource.Error -> {
                                    _categoriesFlow.value = emptyList()
                                    onError(resource.message)
                                }
                                else -> { /* Loading u otros */ }
                            }
                        }
                },
                exceptionHandler = { e ->
                    _categoriesFlow.value = emptyList()
                    onError(e.message)
                },
                showLoading = false
            )
        }
    }

    /**
     * Flujo reactivo de items + filtros. No bloquea main thread:
     * - upstream de items ejecutado en IO (flowOn)
     * - transformaciones ligeras en Main son seguras
     */
    private fun loadItemsReactive() {
        viewModelScope.launch {
            context.contextFlow.collectLatest { current ->
                if (current == null || !current.isCashBoxOpen) {
                    _stateFlow.value = InventoryState.Empty
                    return@collectLatest
                }
                val baseCurrency = current.currency
                val exchangeRate = current.exchangeRate

                val itemsFlow: Flow<PagingData<ItemBO>> = combine(
                    searchFilter.debounce(300),
                    categoryFilter.debounce(300)
                ) { query, category -> query to category }
                    .debounce(200)
                    .flatMapLatest { (query, category) ->
                        fetchInventoryItemUseCase.invoke(query.takeIf { it.isNotBlank() })
                            .flowOn(Dispatchers.IO)
                            .map { pagingData ->
                                pagingData.filter { item ->
                                    val matchesQuery = query.isBlank() ||
                                            item.name.contains(query, ignoreCase = true) ||
                                            item.itemCode.contains(query, ignoreCase = true)

                                    val matchesCategory = category.isBlank() ||
                                            category == "Todos los grupos de artículos" ||
                                            item.itemGroup.equals(category, ignoreCase = true)

                                    matchesQuery && matchesCategory
                                }
                            }
                    }
                    .cachedIn(viewModelScope)

                combine(itemsFlow, _categoriesFlow) { items, categories ->
                    InventoryState.Success(
                        flowOf(items),
                        categories,
                        baseCurrency = baseCurrency,
                        exchangeRate = exchangeRate
                    )
                }
                    .onStart {
                        _stateFlow.value = InventoryState.Loading
                    }
                    .catch { e ->
                        _stateFlow.value = InventoryState.Error(e.message ?: "Error al cargar inventario")
                        onError(e.message)
                    }
                    .collectLatest { state ->
                        _stateFlow.value = state
                    }
            }
        }
    }

    // Conservadas: interfaz pública que ya usabas
    fun onSearchQueryChanged(query: String) {
        searchFilter.value = query
    }

    fun onCategorySelected(category: String) {
        categoryFilter.value = category
    }

    fun refresh() {
        // Forzamos re-emisión sin alterar valor final (minimiza blink)
        val q = searchFilter.value
        val c = categoryFilter.value

        // Emisiones rápidas para forzar flatMapLatest en loadItemsReactive
        searchFilter.value = "$q "
        searchFilter.value = q

        // Reaplicar categoría (esto también dispara combine)
        categoryFilter.value = c
    }

    private fun onError(message: String?) {
        println("InventoryViewModel onError = $message")
    }

}
