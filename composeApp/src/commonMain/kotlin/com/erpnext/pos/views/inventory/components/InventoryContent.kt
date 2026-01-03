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
    searchQuery: String,
    selectedCategory: String,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isDesktop = getPlatformName() == "Desktop"
    Column(Modifier.fillMaxSize()) {
        InventoryFilters(
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            categories = state.categories?.map { it.name!! },
            onQueryChange = onQueryChanged,
            onCategoryChange = onCategorySelected,
            onSearchQueryChanged = { query -> {} },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )

        when {
            itemsLazy == null -> {
                // Aún no hay datos inicializados
                FullScreenShimmerLoading()
            }

            itemsLazy.loadState.refresh is LoadState.Loading -> {
                // Cargando por primera vez (antes de que haya data)
                FullScreenShimmerLoading()
            }

            itemsLazy.loadState.refresh is LoadState.Error -> {
                val error = (itemsLazy.loadState.refresh as LoadState.Error).error
                EmptyStateMessage("${strings.inventory.errorPrefix}: ${error.message}", Icons.Default.Error)
            }

            itemsLazy.itemCount == 0 -> {
                // Ya cargó, pero no hay resultados
                EmptyStateMessage(strings.inventory.emptySearchMessage, Icons.Default.Inventory2)
            }

            else -> {
                // Hay datos, muestra la lista
                InventoryList(
                    items = itemsLazy,
                    listState = listState,
                    actions = actions,
                    isDesktop = isDesktop
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
