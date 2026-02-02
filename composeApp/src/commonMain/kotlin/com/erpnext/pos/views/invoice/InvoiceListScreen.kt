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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.invoice.components.EmptyState
import com.erpnext.pos.views.invoice.components.ErrorState
import com.erpnext.pos.views.invoice.components.LoadingState
import org.koin.compose.koinInject

@Composable
fun InvoiceListScreen(action: InvoiceAction) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val lazyPagingItems = action.getInvoices().collectAsLazyPagingItems()
    val feedback by action.feedbackMessage.collectAsState("")
    var pendingDialog by remember { mutableStateOf<InvoiceCancelDialogState?>(null) }
    var dialogReason by remember { mutableStateOf("") }

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
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar por ID, cliente o teléfono...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                IconButton(onClick = { /* TODO: Show date picker */ }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Filtrar por fecha")
                }
            }

            HorizontalDivider()

            if (!feedback.isNullOrBlank()) {
                LaunchedEffect(feedback) {
                    action.onFeedbackCleared()
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = feedback!!,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val normalizedQuery = searchQuery.trim().lowercase()
            val filteredInvoices by remember(lazyPagingItems, normalizedQuery) {
                derivedStateOf {
                    if (normalizedQuery.isBlank()) {
                        lazyPagingItems.itemSnapshotList.items
                    } else {
                        lazyPagingItems.itemSnapshotList.items.filter { invoice ->
                            listOf(
                                invoice.invoiceId,
                                invoice.customer.orEmpty(),
                                invoice.customerPhone.orEmpty()
                            ).any { it.lowercase().contains(normalizedQuery) }
                        }
                    }
                }
            }

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
                if (filteredInvoices.isEmpty() && normalizedQuery.isNotBlank()) {
                    item { EmptyState(modifier = Modifier.fillParentMaxSize()) }
                } else {
                    items(
                        count = filteredInvoices.size,
                        key = { index -> filteredInvoices[index].invoiceId }
                    ) { index ->
                        val item = filteredInvoices[index]
                        InvoiceItem(
                            invoice = item,
                            onClick = { action.onItemClick(it.invoiceId) },
                            onCancelClick = { invoiceId, actionType ->
                                pendingDialog = InvoiceCancelDialogState(
                                    invoiceId = invoiceId,
                                    action = actionType
                                )
                                dialogReason = ""
                            }
                        )
                    }
                }

                // Muestra el indicador de carga para la paginación
                if (normalizedQuery.isBlank() && lazyPagingItems.loadState.append is LoadState.Loading) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        pendingDialog?.let { dialogState ->
            AlertDialog(
                onDismissRequest = {
                    pendingDialog = null
                    dialogReason = ""
                },
                title = {
                    Text(
                        text = if (dialogState.action == InvoiceCancellationAction.RETURN)
                            "Registrar retorno"
                        else
                            "Cancelar factura"
                    )
                },
                text = {
                    Column {
                        Text(
                            text = if (dialogState.action == InvoiceCancellationAction.RETURN)
                                "Se creará una nota de crédito vinculada a esta factura."
                            else
                                "La factura quedará cancelada y se excluye del cierre."
                        )
                        if (dialogState.action == InvoiceCancellationAction.RETURN) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = dialogReason,
                                onValueChange = { dialogReason = it },
                                label = { Text("Motivo (opcional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            action.onInvoiceCancelRequested(
                                dialogState.invoiceId,
                                dialogState.action,
                                dialogReason.takeIf { it.isNotBlank() }
                            )
                            pendingDialog = null
                            dialogReason = ""
                        }
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingDialog = null
                            dialogReason = ""
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
 fun InvoiceItem(
     invoice: SalesInvoiceBO,
     onClick: (SalesInvoiceBO) -> Unit,
     onCancelClick: (String, InvoiceCancellationAction) -> Unit
 ) {
    val cashboxManager: CashBoxManager = koinInject()
    val posCurrency = normalizeCurrency(cashboxManager.getContext()?.currency) ?: "USD"
    val baseCurrency = normalizeCurrency(invoice.partyAccountCurrency) ?: posCurrency
    val invoiceCurrency = normalizeCurrency(invoice.currency) ?: posCurrency
    val invoiceToBaseRate = invoice.conversionRate
    val baseTotal = invoice.baseGrandTotal ?: run {
        if (invoiceCurrency.equals(baseCurrency, ignoreCase = true)) invoice.total
        else if (invoiceToBaseRate != null && invoiceToBaseRate > 0.0)
            invoice.total * invoiceToBaseRate
        else invoice.total
    }
    val baseOutstanding = invoice.baseOutstandingAmount ?: invoice.outstandingAmount
    var rateBaseToPos by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(baseCurrency, posCurrency) {
        rateBaseToPos = if (baseCurrency.equals(posCurrency, ignoreCase = true)) {
            1.0
        } else {
            cashboxManager.resolveExchangeRateBetween(
                fromCurrency = baseCurrency,
                toCurrency = posCurrency,
                allowNetwork = false
            )
        }
    }
    val posTotal = rateBaseToPos?.takeIf { it > 0.0 }?.let { baseTotal * it } ?: baseTotal
    val posOutstanding =
        rateBaseToPos?.takeIf { it > 0.0 }?.let { baseOutstanding * it } ?: baseOutstanding
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Total: ${formatCurrency(posCurrency, posTotal)}",
                        fontWeight = FontWeight.Medium
                    )
                    if (!baseCurrency.equals(posCurrency, ignoreCase = true)) {
                        Text(
                            formatCurrency(baseCurrency, baseTotal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Pendiente: ${formatCurrency(posCurrency, posOutstanding)}",
                        fontWeight = FontWeight.Medium
                    )
                    if (!baseCurrency.equals(posCurrency, ignoreCase = true)) {
                        Text(
                            formatCurrency(baseCurrency, baseOutstanding),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                val normalizedStatus = invoice.status?.trim()?.lowercase()
                val hasPayments = invoice.paidAmount > 0.0 || invoice.payments.any { it.amount > 0.0 }
                val unpaidStatuses = setOf(
                    "draft",
                    "unpaid",
                    "overdue",
                    "overdue and discounted",
                    "unpaid and discounted"
                )
                val paidStatuses = setOf(
                    "paid",
                    "partly paid",
                    "partly paid and discounted"
                )
                val isDraftOrUnpaid = normalizedStatus in unpaidStatuses
                val isPaidOrPartly = normalizedStatus in paidStatuses
                val canCancel = (isDraftOrUnpaid || (invoice.outstandingAmount > 0.0 && invoice.paidAmount <= 0.0001)) &&
                    !hasPayments
                val canReturn = isPaidOrPartly || hasPayments
                if (canCancel || canReturn) {
                    val actionType =
                        if (canReturn) InvoiceCancellationAction.RETURN else InvoiceCancellationAction.CANCEL
                    TextButton(
                        onClick = {
                            onCancelClick(invoice.invoiceId, actionType)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = if (actionType == InvoiceCancellationAction.CANCEL)
                                "Cancelar"
                            else
                                "Registrar retorno"
                        )
                    }
                }
            }
        }
    }
}

private data class InvoiceCancelDialogState(
    val invoiceId: String,
    val action: InvoiceCancellationAction
)
