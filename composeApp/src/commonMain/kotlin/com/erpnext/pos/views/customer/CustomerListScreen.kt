package com.erpnext.pos.views.customer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.utils.toCurrencySymbol
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class CustomerQuickActionType {
    PendingInvoices,
    CreateQuotation,
    CreateSalesOrder,
    CreateDeliveryNote,
    CreateInvoice,
    RegisterPayment
}

private data class CustomerQuickAction(
    val type: CustomerQuickActionType,
    val label: String,
    val icon: ImageVector
)

private fun customerQuickActions(): List<CustomerQuickAction> = listOf(
    CustomerQuickAction(
        type = CustomerQuickActionType.PendingInvoices,
        label = "Ver facturas pendientes",
        icon = Icons.Filled.ReceiptLong
    ),
    CustomerQuickAction(
        type = CustomerQuickActionType.CreateQuotation,
        label = "Crear cotización",
        icon = Icons.Filled.Description
    ),
    CustomerQuickAction(
        type = CustomerQuickActionType.CreateSalesOrder,
        label = "Crear orden de venta",
        icon = Icons.Filled.PointOfSale
    ),
    CustomerQuickAction(
        type = CustomerQuickActionType.CreateDeliveryNote,
        label = "Crear nota de entrega",
        icon = Icons.Filled.LocalShipping
    ),
    CustomerQuickAction(
        type = CustomerQuickActionType.CreateInvoice,
        label = "Crear factura",
        icon = Icons.Filled.Receipt
    ),
    CustomerQuickAction(
        type = CustomerQuickActionType.RegisterPayment,
        label = "Registrar pago",
        icon = Icons.Filled.Payments
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    state: CustomerState,
    invoicesState: CustomerInvoicesState,
    paymentState: CustomerPaymentState,
    actions: CustomerAction
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    var searchQuery by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("Todos") }
    var quickActionsCustomer by remember { mutableStateOf<CustomerBO?>(null) }
    var outstandingCustomer by remember { mutableStateOf<CustomerBO?>(null) }

    val customers = remember(state) {
        if (state is CustomerState.Success) state.customers else emptyList()
    }
    val isDesktop = getPlatformName() == "Desktop"

    val filterElevation by animateDpAsState(
        targetValue = if (customers.isNotEmpty()) 4.dp else 0.dp,
        label = "filterElevation"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Clientes", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = actions.fetchAll) {
                        Icon(Icons.Filled.Refresh, "Actualizar Clientes")
                    }
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val isWideLayout = maxWidth >= 840.dp || isDesktop
            val contentPadding = if (isWideLayout) 24.dp else 16.dp

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Filtros y búsqueda
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = filterElevation,
                    shadowElevation = filterElevation
                ) {
                    CustomerFilters(
                        searchQuery = searchQuery,
                        selectedState = selectedState,
                        isWideLayout = isWideLayout,
                        onQueryChange = {
                            searchQuery = it
                            actions.onSearchQueryChanged(it)
                        },
                        onStateChange = {
                            selectedState = it ?: "Todos"
                            actions.onStateSelected(it)
                        },
                        modifier = Modifier.padding(horizontal = contentPadding, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenido principal según estado
                // Slot-based rendering: estructura fija con visibilidad dinámica
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {

                    // SLOT: LISTA DE CLIENTES
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state is CustomerState.Success &&
                                (state.customers.isNotEmpty()),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val filtered = customers.filter { customer ->
                            customer.customerName.contains(searchQuery, ignoreCase = true) ||
                                    (customer.mobileNo ?: "").contains(searchQuery)
                        }

                        if (filtered.isEmpty()) {
                            EmptyStateMessage(
                                message = if (searchQuery.isEmpty())
                                    "No hay clientes disponibles."
                                else
                                    "No se encontraron clientes que coincidan con tu búsqueda.",
                                icon = Icons.Filled.People
                            )
                        } else {
                            CustomerListContent(
                                customers = filtered,
                                actions = actions,
                                isWideLayout = isWideLayout,
                                isDesktop = isDesktop,
                                onOpenQuickActions = { quickActionsCustomer = it },
                                onQuickAction = { customer, actionType ->
                                    handleQuickAction(actions, customer, actionType)
                                }
                            )
                        }
                    }

                    // SLOT: LOADING
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state is CustomerState.Loading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CustomerShimmerList()
                    }

                    // SLOT: EMPTY
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state is CustomerState.Empty,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        EmptyStateMessage(
                            message = "No hay clientes disponibles.",
                            icon = Icons.Filled.People
                        )
                    }

                    // SLOT: ERROR
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state is CustomerState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FullScreenErrorMessage(
                            errorMessage = (state as CustomerState.Error).message,
                            onRetry = actions.fetchAll
                        )
                    }
                }

            }
        }
    }

    quickActionsCustomer?.let { customer ->
        CustomerQuickActionsSheet(
            customer = customer,
            onDismiss = { quickActionsCustomer = null },
            onActionSelected = { actionType ->
                quickActionsCustomer = null
                when (actionType) {
                    CustomerQuickActionType.PendingInvoices,
                    CustomerQuickActionType.RegisterPayment -> {
                        outstandingCustomer = customer
                        actions.loadOutstandingInvoices(customer)
                    }
                    else -> handleQuickAction(actions, customer, actionType)
                }
            }
        )
    }

    outstandingCustomer?.let { customer ->
        CustomerOutstandingInvoicesSheet(
            customer = customer,
            invoicesState = invoicesState,
            paymentState = paymentState,
            onDismiss = {
                outstandingCustomer = null
                actions.clearOutstandingInvoices()
            },
            onRegisterPayment = { invoiceId, mode, amount ->
                actions.registerPayment(customer.name, invoiceId, mode, amount)
            }
        )
    }
}

@Composable
private fun CustomerFilters(
    searchQuery: String,
    selectedState: String,
    isWideLayout: Boolean,
    states: List<String> = listOf("Pendientes", "Sin Pendientes"),
    onQueryChange: (String) -> Unit,
    onStateChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        val isSelected = selectedState == "Todos"
                        FilterChipItem("Todos", isSelected) { onStateChange("Todos") }
                    }
                    items(states) { state ->
                        val isSelected = selectedState == state
                        FilterChipItem(state, isSelected) { onStateChange(state) }
                    }
                }

                SearchTextField(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onQueryChange,
                    placeholderText = "Buscar cliente por nombre o teléfono...",
                    modifier = Modifier.weight(1.2f)
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isSelected = selectedState == "Todos"
                    FilterChipItem("Todos", isSelected) { onStateChange("Todos") }
                }
                items(states) { state ->
                    val isSelected = selectedState == state
                    FilterChipItem(state, isSelected) { onStateChange(state) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            SearchTextField(
                searchQuery = searchQuery,
                onSearchQueryChange = onQueryChange,
                placeholderText = "Buscar cliente por nombre o teléfono..."
            )
        }
    }
}

@Composable
fun SearchTextField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Buscar...",
    onSearchAction: (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value =
            searchQuery,
        onValueChange = { query -> onSearchQueryChange(query) },
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        placeholder = {
            Text(
                placeholderText,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Icono de búsqueda",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onSearchQueryChange("")
                        keyboardController?.show()
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Borrar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = if (onSearchAction != null) ImeAction.Search else ImeAction.Done),
        keyboardActions = KeyboardActions(onSearch = {
            if (onSearchAction != null) {
                onSearchAction()
                keyboardController?.hide()
            } else {
                keyboardController?.hide()
            }
        }, onDone = { keyboardController?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (selected) Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun CustomerListContent(
    customers: List<CustomerBO>,
    actions: CustomerAction,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    onOpenQuickActions: (CustomerBO) -> Unit,
    onQuickAction: (CustomerBO, CustomerQuickActionType) -> Unit
) {
    val spacing = if (isWideLayout) 16.dp else 12.dp
    if (isWideLayout) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 360.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(customers, key = { it.name }) { customer ->
                CustomerItem(
                    customer = customer,
                    isDesktop = isDesktop,
                    onClick = { actions.toDetails(customer.name) },
                    onOpenQuickActions = { onOpenQuickActions(customer) },
                    onQuickAction = { actionType -> onQuickAction(customer, actionType) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(customers, key = { it.name }) { customer ->
                CustomerItem(
                    customer = customer,
                    isDesktop = isDesktop,
                    onClick = { actions.toDetails(customer.name) },
                    onOpenQuickActions = { onOpenQuickActions(customer) },
                    onQuickAction = { actionType -> onQuickAction(customer, actionType) }
                )
            }
        }
    }
}

@Composable
fun CustomerItem(
    customer: CustomerBO,
    isDesktop: Boolean,
    onClick: () -> Unit,
    onOpenQuickActions: () -> Unit,
    onQuickAction: (CustomerQuickActionType) -> Unit
) {
    val isOverLimit = (customer.availableCredit ?: 0.0) < 0 || (customer.currentBalance ?: 0.0) > 0
    var isMenuExpanded by remember { mutableStateOf(false) }
    val quickActions = remember { customerQuickActions() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(customer.name, isDesktop) {
                if (!isDesktop) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(totalDrag) > 64) {
                                onOpenQuickActions()
                            }
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        }
                    )
                }
            }
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverLimit) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circular
            Card(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverLimit) MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.2f
                    ) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = customer.customerName,
                    modifier = Modifier.size(48.dp).padding(12.dp),
                    tint = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contenido expandible
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    customer.customerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if ((customer.pendingInvoices ?: 0) > 0) {
                    StatusPill(
                        label = "Overdue",
                        isCritical = true
                    )
                }
                Text(
                    customer.mobileNo ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    customer.territory ?: "N/A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Columna derecha para balance/pendientes
            val currencySymbol = customer.currency.toCurrencySymbol()
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$currencySymbol${customer.currentBalance ?: 0.0}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isOverLimit) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    "Pending: ${customer.pendingInvoices}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverLimit) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    "Available: $currencySymbol${customer.availableCredit ?: 0.0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Limit: $currencySymbol${customer.creditLimit ?: 0.0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                IconButton(onClick = {
                    if (isDesktop) {
                        isMenuExpanded = true
                    } else {
                        onOpenQuickActions()
                    }
                }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Más acciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = isDesktop && isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    quickActions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.label) },
                            leadingIcon = {
                                Icon(action.icon, contentDescription = null)
                            },
                            onClick = {
                                isMenuExpanded = false
                                onQuickAction(action.type)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, isCritical: Boolean) {
    val background = if (isCritical) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    }
    val textColor = if (isCritical) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondary
    }
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@Composable
fun CustomerShimmerList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .shimmerBackground(RoundedCornerShape(16.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerQuickActionsSheet(
    customer: CustomerBO,
    onDismiss: () -> Unit,
    onActionSelected: (CustomerQuickActionType) -> Unit
) {
    val quickActions = remember { customerQuickActions() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = customer.customerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            CustomerOutstandingSummary(customer)

            Divider()

            quickActions.forEach { action ->
                ListItem(
                    headlineContent = { Text(action.label) },
                    leadingContent = { Icon(action.icon, contentDescription = null) },
                    modifier = Modifier.clickable { onActionSelected(action.type) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerOutstandingInvoicesSheet(
    customer: CustomerBO,
    invoicesState: CustomerInvoicesState,
    paymentState: CustomerPaymentState,
    onDismiss: () -> Unit,
    onRegisterPayment: (invoiceId: String, modeOfPayment: String, amount: Double) -> Unit
) {
    var selectedInvoiceId by remember { mutableStateOf<String?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Outstanding invoices - ${customer.customerName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            when (invoicesState) {
                CustomerInvoicesState.Idle -> Text(
                    text = "Select a customer to view outstanding invoices.",
                    style = MaterialTheme.typography.bodyMedium
                )
                CustomerInvoicesState.Loading -> CircularProgressIndicator()
                is CustomerInvoicesState.Error -> Text(
                    text = invoicesState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                is CustomerInvoicesState.Success -> {
                    if (invoicesState.invoices.isEmpty()) {
                        Text(
                            text = "No outstanding invoices for this customer.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        invoicesState.invoices.forEach { invoice ->
                            val isSelected = invoice.invoiceId == selectedInvoiceId
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedInvoiceId = invoice.invoiceId
                                            paymentAmount = invoice.outstandingAmount.toString()
                                        }
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = invoice.invoiceId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Posted: ${invoice.postingDate}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Outstanding: ${invoice.currency?.toCurrencySymbol().orEmpty()}${invoice.outstandingAmount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            Text(
                text = "Register payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = selectedInvoiceId ?: "",
                onValueChange = { selectedInvoiceId = it },
                label = { Text("Invoice ID") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paymentMode,
                onValueChange = { paymentMode = it },
                label = { Text("Mode of payment") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paymentAmount,
                onValueChange = { paymentAmount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            paymentState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            paymentState.successMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    val invoiceId = selectedInvoiceId?.trim().orEmpty()
                    val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                    onRegisterPayment(invoiceId, paymentMode, amount)
                },
                enabled = !paymentState.isSubmitting
            ) {
                Text(if (paymentState.isSubmitting) "Processing..." else "Register payment")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CustomerOutstandingSummary(customer: CustomerBO) {
    val currencySymbol = customer.currency.toCurrencySymbol()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Resumen de pendientes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Facturas pendientes")
            Text("${customer.pendingInvoices}")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Monto pendiente")
            Text("$currencySymbol${customer.totalPendingAmount ?: customer.currentBalance}")
        }
    }
}

private fun handleQuickAction(
    actions: CustomerAction,
    customer: CustomerBO,
    actionType: CustomerQuickActionType
) {
    when (actionType) {
        CustomerQuickActionType.PendingInvoices -> actions.onViewPendingInvoices(customer)
        CustomerQuickActionType.CreateQuotation -> actions.onCreateQuotation(customer)
        CustomerQuickActionType.CreateSalesOrder -> actions.onCreateSalesOrder(customer)
        CustomerQuickActionType.CreateDeliveryNote -> actions.onCreateDeliveryNote(customer)
        CustomerQuickActionType.CreateInvoice -> actions.onCreateInvoice(customer)
        CustomerQuickActionType.RegisterPayment -> actions.onRegisterPayment(customer)
    }
}

@Composable
private fun FullScreenErrorMessage(
    errorMessage: String, onRetry: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Error,
                "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                errorMessage,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = onRetry, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(
    message: String, icon: ImageVector, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                message,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun Modifier.shimmerBackground(
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    baseAlpha: Float = 0.6f,
    highlightAlpha: Float = 0.2f,
    gradientWidth: Float = 400f,
    durationMillis: Int = 1200
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = gradientWidth,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "shimmerTranslateAnim"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseAlpha),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = highlightAlpha),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseAlpha)
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - gradientWidth, translateAnim - gradientWidth),
            end = Offset(translateAnim, translateAnim)
        ),
        shape = shape
    )
}

@Preview
@Composable
fun CustomerListScreenPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Success(
                customers = listOf(
                    CustomerBO(
                        name = "1",
                        customerName = "Ricardo García",
                        territory = "Managua",
                        mobileNo = "+505 8888 0505",
                        customerType = "Individual",
                        currentBalance = 13450.0,
                        pendingInvoices = 2,
                        availableCredit = 0.0
                    )
                )
            ),
            invoicesState = CustomerInvoicesState.Idle,
            paymentState = CustomerPaymentState(),
            actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerListScreenLoadingPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Loading,
            invoicesState = CustomerInvoicesState.Idle,
            paymentState = CustomerPaymentState(),
            actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerListScreenErrorPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Error("Error al cargar clientes"),
            invoicesState = CustomerInvoicesState.Idle,
            paymentState = CustomerPaymentState(),
            actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerListScreenEmptyPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Empty,
            invoicesState = CustomerInvoicesState.Idle,
            paymentState = CustomerPaymentState(),
            actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerItemPreview() {
    MaterialTheme {
        CustomerItem(
            customer = CustomerBO(
                name = "1",
                customerName = "Ricardo García",
                territory = "Managua",
                mobileNo = "+505 8888 0505",
                customerType = "Individual",
                currentBalance = 13450.0,
                pendingInvoices = 2,
                availableCredit = 0.0
            ),
            isDesktop = false,
            onClick = {},
            onOpenQuickActions = {},
            onQuickAction = {}
        )
    }
}

@Preview
@Composable
fun CustomerItemOverLimitPreview() {
    MaterialTheme {
        CustomerItem(
            customer = CustomerBO(
                name = "2",
                customerName = "Sofía Ramírez",
                territory = "León",
                mobileNo = "+505 7777 0404",
                customerType = "Company",
                currentBalance = 0.0,
                pendingInvoices = 0,
                availableCredit = -500.0  // Sobre límite para rojo
            ),
            isDesktop = false,
            onClick = {},
            onOpenQuickActions = {},
            onQuickAction = {}
        )
    }
}
