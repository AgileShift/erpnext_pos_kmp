package com.erpnext.pos.views.inventory.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.views.inventory.InventoryAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun <T : Any> rememberPreviewLazyPagingItems(items: List<T>): LazyPagingItems<T> {
    val flow: Flow<PagingData<T>> = remember(items) { flowOf(PagingData.from(items)) }
    return flow.collectAsLazyPagingItems()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InventoryList(
    items: LazyPagingItems<ItemBO>,
    listState: LazyListState,
    actions: InventoryAction,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    baseCurrency: String,
    exchangeRate: Double,
    searchQuery: String,
    selectedCategory: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val snapshotItems = items.itemSnapshotList.items
    val normalizedQuery = searchQuery.trim().lowercase()
    val isFiltering = normalizedQuery.isNotBlank() ||
            selectedCategory != "Todos los grupos de artículos"
    val filteredItems = remember(snapshotItems, normalizedQuery, selectedCategory) {
        snapshotItems.filter { item ->
            val matchesQuery = normalizedQuery.isBlank() || listOf(
                item.name,
                item.itemCode,
                item.description,
                item.barcode,
                item.brand.orEmpty()
            ).any { it.lowercase().contains(normalizedQuery) }
            val matchesCategory = selectedCategory == "Todos los grupos de artículos" ||
                    item.itemGroup.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    AnimatedContent(
        modifier = Modifier.fillMaxWidth(),
        targetState = items.loadState.refresh,
        label = "InventoryListAnimation"
    ) { state ->
        when (state) {
            is LoadState.Loading if items.itemCount == 0 -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(8) {
                        ShimmerProductPlaceholder(
                            Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        )
                    }
                }
            }

            is LoadState.Error if items.itemCount == 0 -> {
                ErrorItem(
                    message = strings.inventory.loadInventoryError,
                    onRetry = { items.retry() }
                )
            }

            else -> {
                if (isFiltering) {
                    if (filteredItems.isEmpty()) {
                        EmptyStateMessage(
                            strings.inventory.emptySearchMessage,
                            Icons.Default.Inventory2
                        )
                        return@AnimatedContent
                    }
                    if (isDesktop) {
                        val spacing = if (isWideLayout) 16.dp else 12.dp
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 360.dp),
                            modifier = modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(spacing),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                        ) {
                            itemsIndexed(filteredItems, key = { _, item ->
                                val keyBase = item.itemCode.ifBlank { item.name }
                                "$keyBase-$baseCurrency-${exchangeRate}"
                            }) { _, item ->
                                ProductCard(
                                    actions,
                                    item,
                                    isDesktop = isDesktop,
                                    baseCurrency = baseCurrency,
                                    exchangeRate = exchangeRate
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(filteredItems, key = { _, item ->
                                val keyBase = item.itemCode.ifBlank { item.name }
                                "$keyBase-$baseCurrency-${exchangeRate}"
                            }) { _, item ->
                                ProductCard(
                                    actions,
                                    item,
                                    isDesktop = isDesktop,
                                    baseCurrency = baseCurrency,
                                    exchangeRate = exchangeRate
                                )
                            }
                        }
                    }
                    return@AnimatedContent
                }
                if (isDesktop) {
                    val spacing = if (isWideLayout) 16.dp else 12.dp

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 360.dp),
                        modifier = modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        items(
                            count = items.itemCount,
                            key = { index ->
                                val item = items[index]
                                val keyBase = item?.itemCode ?: item?.name ?: "item-$index"
                                "$keyBase-$baseCurrency-${exchangeRate}"
                            }
                        ) { index ->
                            val item = items[index]
                            if (item != null) {
                                ProductCard(
                                    actions,
                                    item,
                                    isDesktop = isDesktop,
                                    baseCurrency = baseCurrency,
                                    exchangeRate = exchangeRate
                                )
                            } else {
                                ShimmerProductPlaceholder(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            count = items.itemCount,
                            key = { index ->
                                val item = items[index]
                                val keyBase = item?.itemCode ?: item?.name ?: "item-$index"
                                "$keyBase-$baseCurrency-${exchangeRate}"
                            }
                        ) { index ->
                            val item = items[index]
                            if (item != null) {
                                ProductCard(
                                    actions,
                                    item,
                                    isDesktop = isDesktop,
                                    baseCurrency = baseCurrency,
                                    exchangeRate = exchangeRate
                                )
                            } else {
                                // 🔹 Shimmer parcial discreto
                                ShimmerProductPlaceholder(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                )
                            }
                        }

                        when (items.loadState.append) {
                            is LoadState.Loading -> {
                                item { LoadingMoreItem() }
                            }

                            is LoadState.Error -> {
                                item {
                                    ErrorItem(
                                        strings.inventory.loadMoreError,
                                        onRetry = { items.retry() }
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingMoreItem() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(Modifier.size(24.dp))
    }
}

@Composable
fun ErrorItem(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Red),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = strings.inventory.retry
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = strings.inventory.retry)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
fun InventoryListPreview() {
    InventoryList(
        items = rememberPreviewLazyPagingItems(
            listOf(
                ItemBO(
                    name = "Palazzos a cuadros de paletones con faja incluida-ROJO VINO-S",
                    description = "Palazzos a cuadros de paletones con faja incluida-ROJO VINO-S",
                    itemCode = "P14823-ROJO VINO-S\n",
                    barcode = "",
                    image = "https://images.unsplash.com/photo-1708467374959-e5588da12e8f?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA==",
                    currency = "NIO",
                    itemGroup = "Palazzos",
                    brand = "Shein",
                    price = 18.00,
                    actualQty = 4.0,
                    discount = 0.0,
                    isService = false,
                    isStocked = true,
                    uom = "Unidad(es)",
                    lastSyncedAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        ),
        listState = LazyListState(),
        actions = InventoryAction(),
        isDesktop = true,
        modifier = Modifier,
        isWideLayout = true,
        baseCurrency = "USD",
        exchangeRate = 0.027,
        searchQuery = "",
        selectedCategory = "Todos los grupos de artículos"
    )
}
