package com.erpnext.pos.views.inventory.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.paging.compose.LazyPagingItems
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.views.inventory.InventoryAction

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InventoryList(
    items: LazyPagingItems<ItemBO>,
    listState: LazyListState,
    actions: InventoryAction,
    isDesktop: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = items.loadState.refresh,
        label = "InventoryListAnimation"
    ) { state ->
        when {
            state is LoadState.Loading && items.itemCount == 0 -> {
                // 🔹 Shimmer elegante parcial (no cubre toda la card)
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

            state is LoadState.Error && items.itemCount == 0 -> {
                ErrorItem(
                    message = "Error al cargar inventario",
                    onRetry = { items.retry() }
                )
            }

            else -> {
                if (isDesktop) {
                    LazyVerticalGrid(
                        modifier = modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            count = items.itemCount,
                            key = { index ->
                                val item = items[index]
                                item?.itemCode ?: item?.name ?: "item-$index"
                            }
                        ) { index ->
                            val item = items[index]
                            if (item != null) {
                                ProductCard(actions, item, isDesktop = true)
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
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            count = items.itemCount,
                            key = { index ->
                                val item = items[index]
                                // 🔸 Usa claves estables y consistentes
                                item?.itemCode ?: item?.name ?: "item-$index"
                            }
                        ) { index ->
                            val item = items[index]
                            if (item != null) {
                                ProductCard(actions, item, isDesktop = false)
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
                                        "Error al cargar más items",
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
                contentDescription = "Reintentar"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Reintentar")
        }
    }
}
