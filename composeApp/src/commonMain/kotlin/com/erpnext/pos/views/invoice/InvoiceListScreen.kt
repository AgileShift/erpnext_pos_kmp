package com.erpnext.pos.views.invoice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.views.invoice.components.EmptyState
import com.erpnext.pos.views.invoice.components.ErrorState
import com.erpnext.pos.views.invoice.components.LoadingState

@Composable
fun InvoiceListScreen(action: InvoiceAction) {
    var searchQuery by remember { mutableStateOf("") }
    val lazyPagingItems = action.getInvoices().collectAsLazyPagingItems()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { action.goToBilling() }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva factura")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        action.onSearchQueryChanged(it)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar por ID, cliente o teléfono...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                action.onSearchQueryChanged("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true
                )
                IconButton(onClick = { /* TODO: Show date picker */ }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Filtrar por fecha")
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Muestra los estados de carga, error y vacío de la carga inicial
                when (val loadState = lazyPagingItems.loadState.refresh) {
                    is LoadState.Loading -> {
                        item { LoadingState(modifier = Modifier.fillParentMaxSize()) }
                    }

                    is LoadState.Error -> {
                        val error = loadState.error
                        item {
                            ErrorState(
                                modifier = Modifier.fillParentMaxSize(),
                                message = "Error al cargar facturas: ${error.message}",
                                onRetry = { lazyPagingItems.retry() }
                            )
                        }
                    }

                    is LoadState.NotLoading -> {
                        if (lazyPagingItems.itemCount == 0) {
                            item { EmptyState(modifier = Modifier.fillParentMaxSize()) }
                        }
                    }
                }

                // Muestra la lista de items
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.invoiceId }
                ) { index ->
                    val item = lazyPagingItems[index]
                    if (item != null) {
                        InvoiceItem(item, onClick = { action.onItemClick(it.invoiceId) })
                    }
                }

                // Muestra el indicador de carga para la paginación
                if (lazyPagingItems.loadState.append is LoadState.Loading) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceItem(invoice: SalesInvoiceBO, onClick: (SalesInvoiceBO) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp) // Añadido padding vertical
            .clickable { onClick(invoice) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(invoice.invoiceId, fontWeight = FontWeight.Bold)
                Text(invoice.status ?: "Draft", color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(8.dp))
            Text(invoice.customer ?: "Cliente no especificado")
            Spacer(Modifier.height(4.dp))
            Text(invoice.postingDate, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Total: ${invoice.total}", fontWeight = FontWeight.Medium)
            }
        }
    }
}
