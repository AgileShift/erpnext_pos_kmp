package com.erpnext.pos.views.inventory.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.views.inventory.InventoryAction
import com.erpnext.pos.views.inventory.InventoryState

@Composable
fun InventoryContent(
    state: InventoryState.Success,
    itemsLazy: LazyPagingItems<ItemBO>?,
    listState: LazyListState,
    actions: InventoryAction,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    searchQuery: String,
    selectedCategory: String,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(Modifier.fillMaxSize()) {
        InventoryFilters(
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            categories = state.categories?.map { it.name!! },
            onQueryChange = onQueryChanged,
            onCategoryChange = onCategorySelected,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )

        when {
            itemsLazy == null -> {
                FullScreenShimmerLoading()
            }

            itemsLazy.loadState.refresh is LoadState.Loading -> {
                FullScreenShimmerLoading()
            }

            itemsLazy.loadState.refresh is LoadState.Error -> {
                val error = (itemsLazy.loadState.refresh as LoadState.Error).error
                EmptyStateMessage("${strings.inventory.errorPrefix}: ${error.message}", Icons.Default.Error)
            }

            itemsLazy.itemCount == 0 -> {
                EmptyStateMessage(strings.inventory.emptySearchMessage, Icons.Default.Inventory2)
            }

            else -> {
                InventoryList(
                    items = itemsLazy,
                    listState = listState,
                    actions = actions,
                    isDesktop = isDesktop,
                    isWideLayout = isWideLayout,
                    baseCurrency = state.baseCurrency,
                    exchangeRate = state.exchangeRate,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory
                )
            }
        }

        /*// Inventory list (single collector point)
        if (itemsLazy != null) {
            InventoryList(
                items = itemsLazy,
                listState = listState,
                actions = actions
            )
        } else {
            FullScreenShimmerLoading()
        }*/
    }
}
