package com.erpnext.pos.views.inventory

import androidx.paging.PagingData
import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.ItemBO
import kotlinx.coroutines.flow.Flow

sealed class InventoryState {
    object Loading : InventoryState()
    object Empty : InventoryState()
data class Success(
        val items: Flow<PagingData<ItemBO>>,
        val categories: List<CategoryBO>? = emptyList(),
        val baseCurrency: String = "USD",
        val exchangeRate: Double = 1.0
    ) : InventoryState()

    data class Error(val message: String) : InventoryState()
}

data class InventoryAction(
    val onCategorySelected: (String) -> Unit = {},
    val onSearchQueryChanged: (String) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onPrint: () -> Unit = {},
    val onItemClick: (ItemBO) -> Unit = {},
    val isCashboxOpen: () -> Unit = {},
    val onClearSearch: () -> Unit = {},
    val getDetails: (String) -> Unit = { },
)
