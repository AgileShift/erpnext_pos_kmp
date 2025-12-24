package com.erpnext.pos.views.inventory

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.views.inventory.components.*
import dev.materii.pullrefresh.PullRefreshIndicator
import dev.materii.pullrefresh.pullRefresh
import dev.materii.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun InventoryScreen(
    state: InventoryState,
    actions: InventoryAction,
) {
    val listState = rememberLazyListState()
    val itemsLazy = (state as? InventoryState.Success)?.items?.collectAsLazyPagingItems()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state is InventoryState.Loading,
        onRefresh = { actions.onRefresh() }
    )

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Todos los grupos de artículos") }

    val fabVisible by remember {
        derivedStateOf {
            val count = itemsLazy?.itemCount ?: 0
            count > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    Scaffold(
        topBar = {
            InventoryTopBar(
                onRefresh = actions.onRefresh,
                isLoading = state is InventoryState.Loading
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = actions.onPrint,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Imprimir")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {

            /**
             * 1️⃣ Animación localizada — sin destruir SearchBar ni filtros
             * */
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(tween(250, easing = LinearOutSlowInEasing)) togetherWith
                            fadeOut(tween(200))
                },
                label = "InventoryContentTransition"
            ) { currentState ->

                when (currentState) {
                    is InventoryState.Loading -> {
                        // Mantiene el shimmer detrás si ya había datos cargados
                        if (itemsLazy?.itemCount.orZero() > 0) {
                            InventoryContent(
                                state = InventoryState.Success(
                                    items = flowOf(PagingData.empty()),
                                    categories = emptyList()
                                ),
                                itemsLazy = itemsLazy!!,
                                listState = listState,
                                actions = actions,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onQueryChanged = { query ->
                                    searchQuery = query
                                    actions.onSearchQueryChanged(query)
                                },
                                onCategorySelected = { category ->
                                    selectedCategory = category
                                    actions.onCategorySelected(category)
                                }
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .align(Alignment.Center)
                            ) {
                                FullScreenShimmerLoadingOverlay()
                            }
                        } else {
                            FullScreenShimmerLoading()
                        }
                    }

                    is InventoryState.Error -> {
                        FullScreenErrorMessage(currentState.message) { actions.onRefresh() }
                    }

                    InventoryState.Empty -> {
                        EmptyStateMessage(
                            "Inventario vacío",
                            Icons.Default.Inventory2
                        )
                    }

                    is InventoryState.Success -> {
                        InventoryContent(
                            state = currentState,
                            itemsLazy = currentState.items.collectAsLazyPagingItems(),
                            listState = listState,
                            actions = actions,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onQueryChanged = { query ->
                                searchQuery = query
                                actions.onSearchQueryChanged(query)
                            },
                            onCategorySelected = { category ->
                                selectedCategory = category
                                actions.onCategorySelected(category)
                            }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state is InventoryState.Loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/** 🧩 Overlay shimmer elegante (no cubre todo el Card ni el texto) */
@Composable
fun FullScreenShimmerLoadingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(5) { ShimmerProductPlaceholder(modifier = Modifier.fillMaxWidth()) }
        }
    }
}

private fun Int?.orZero() = this ?: 0

@Preview
@Composable
fun InventoryScreenPreview() {
    InventoryScreen(
        state = InventoryState.Success(
            items = flowOf(
                PagingData.from(
                    listOf(
                        ItemBO(
                            "Producto de prueba",
                            "Producto de prueba con una descripcion",
                            "Pollo Frito congelado",
                            "",
                            image = null,
                            currency = "NIO",
                            itemGroup = "POLLO",
                            brand = "TIPTOP",
                            price = 1000.0,
                            actualQty = 10.0,
                            discount = 0.0,
                            isService = false,
                            isStocked = true,
                            uom = "Unidad",
                            lastSyncedAt = null,
                        )
                    )
                )
            ),
            categories = listOf<CategoryBO>(CategoryBO("Carne"), CategoryBO("Embutidos"), CategoryBO("Pollo"))
        ),
        actions = InventoryAction(),
    )
}
