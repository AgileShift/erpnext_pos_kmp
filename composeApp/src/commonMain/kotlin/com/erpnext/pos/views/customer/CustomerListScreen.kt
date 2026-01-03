package com.erpnext.pos.views.customer

import AppTextField
import MoneyTextField
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
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

private data class CustomerCounts(
    val total: Int,
    val pending: Int
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
    var strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("Todos") }
    var quickActionsCustomer by remember { mutableStateOf<CustomerBO?>(null) }
    var outstandingCustomer by remember { mutableStateOf<CustomerBO?>(null) }

    val customers = remember(state) {
        if (state is CustomerState.Success) state.customers else emptyList()
    }
    var baseCounts by remember { mutableStateOf(CustomerCounts(0, 0)) }
    val isDesktop = getPlatformName() == "Desktop"

    val filterElevation by animateDpAsState(
        targetValue = if (customers.isNotEmpty()) 4.dp else 0.dp,
        label = "filterElevation"
    )

    LaunchedEffect(outstandingCustomer?.name) {
        outstandingCustomer?.let { customer ->
            actions.loadOutstandingInvoices(customer)
        }
    }
    LaunchedEffect(state, searchQuery, selectedState) {
        if (searchQuery.isEmpty() && selectedState == "Todos" && state is CustomerState.Success) {
            val pending = state.customers.count { (it.pendingInvoices ?: 0) > 0 }
            baseCounts = CustomerCounts(
                total = state.customers.size,
                pending = pending
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        strings.customer.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = actions.fetchAll) {
                        Icon(Icons.Filled.Refresh, strings.customer.refreshCustomers)
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

            Column(modifier = Modifier.fillMaxSize()) {
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
                        totalCount = baseCounts.total,
                        pendingCount = baseCounts.pending,
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
                                    strings.customer.emptyCustomers
                                else
                                    strings.customer.emptySearchCustomers,
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
                                    when (actionType) {
                                        CustomerQuickActionType.PendingInvoices,
                                        CustomerQuickActionType.RegisterPayment -> {
                                            outstandingCustomer = customer
                                        }

                                        else -> handleQuickAction(actions, customer, actionType)
                                    }
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
                            message = strings.customer.emptyCustomers,
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
    totalCount: Int,
    pendingCount: Int,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val allSelected = selectedState == "Todos"
                    FilterSummaryTile(
                        label = "Todos",
                        value = "$totalCount",
                        selected = allSelected,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onStateChange("Todos") }
                    )
                    states.forEach { state ->
                        val isSelected = selectedState == state
                        val color = if (state == "Pendientes") {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                        val value = if (state == "Pendientes") {
                            "$pendingCount"
                        } else {
                            "${(totalCount - pendingCount).coerceAtLeast(0)}"
                        }
                        FilterSummaryTile(
                            label = state,
                            value = value,
                            selected = isSelected,
                            color = color,
                            onClick = { onStateChange(state) }
                        )
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
                    FilterSummaryTile(
                        label = "Todos",
                        value = "$totalCount",
                        selected = isSelected,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onStateChange("Todos") }
                    )
                }
                items(states) { state ->
                    val isSelected = selectedState == state
                    val color = if (state == "Pendientes") {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                    val value = if (state == "Pendientes") {
                        "$pendingCount"
                    } else {
                        "${(totalCount - pendingCount).coerceAtLeast(0)}"
                    }
                    FilterSummaryTile(
                        label = state,
                        value = value,
                        selected = isSelected,
                        color = color,
                        onClick = { onStateChange(state) }
                    )
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
    TextField(
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
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.medium
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
    val strings = LocalAppStrings.current
    val isOverLimit = (customer.availableCredit ?: 0.0) < 0 || (customer.currentBalance ?: 0.0) > 0
    val pendingInvoices = customer.pendingInvoices ?: 0
    val availableCredit = customer.availableCredit ?: 0.0
    val currencySymbol = customer.currency.toCurrencySymbol()
    var isMenuExpanded by remember { mutableStateOf(false) }
    val quickActions = remember { customerQuickActions() }
    val avatarSize = if (isDesktop) 52.dp else 44.dp
    val pendingAmount = customer.totalPendingAmount ?: customer.currentBalance ?: 0.0
    val statusLabel = when {
        isOverLimit -> strings.customer.overdueLabel
        pendingInvoices > 0 || pendingAmount > 0.0 -> strings.customer.pendingLabel
        else -> strings.customer.activeLabel
    }
    val statusColor = when {
        isOverLimit -> MaterialTheme.colorScheme.error
        pendingInvoices > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val emphasis = pendingInvoices > 0 || isOverLimit
    val cardElevation = if (emphasis) 3.dp else 1.dp

    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .height(180.dp)
            .clip(cardShape)
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
        // elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = cardShape,
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverLimit) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Transparent, shape = cardShape)
                .padding(if (isDesktop) 14.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val context = LocalPlatformContext.current
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(55.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!customer.image.isNullOrEmpty()) {
                    AsyncImage(
                        modifier = Modifier.size(60.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        model = remember(customer.image) {
                            ImageRequest.Builder(context)
                                .data(customer.image?.ifBlank { "https://placehold.co/600x400" })
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = customer.name,
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = customer.customerName,
                        modifier = Modifier.size(avatarSize).clip(CircleShape)
                            .padding(12.dp),
                        tint = statusColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        customer.customerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusPill(
                        label = statusLabel,
                        isCritical = emphasis
                    )
                }

                IconButton(onClick = {
                    if (isDesktop) {
                        isMenuExpanded = true
                    } else {
                        onOpenQuickActions()
                    }
                }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = strings.customer.moreActions,
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

            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(55.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.widthIn(min = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricBlock(
                        label = strings.customer.pendingLabel,
                        value = "$currencySymbol$pendingAmount",
                        isCritical = emphasis
                    )
                    MetricBlock(
                        label = strings.customer.outstandingSummaryInvoicesLabel,
                        value = "$pendingInvoices",
                        isCritical = pendingInvoices > 0
                    )
                    MetricBlock(
                        label = strings.customer.availableLabel,
                        value = "$currencySymbol ${bd(availableCredit).moneyScale(2)}",
                        isCritical = availableCredit < 0
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if(customer.mobileNo?.isNotEmpty() == true) {
                        CustomerInfoChip(
                            icon = Icons.Filled.Phone,
                            text = customer.mobileNo ?: "Residencial Palmanova #117"
                        )
                    }
                    if (customer.address?.isNotEmpty() == true) {
                        CustomerInfoChip(
                            icon = Icons.Filled.Place,
                            text = customer.address
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
    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MetricBlock(label: String, value: String, isCritical: Boolean) {
    val background = if (isCritical) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.33f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.33f)
    }
    val textColor = if (isCritical) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
            Text(value, style = MaterialTheme.typography.titleSmall, color = textColor)
        }
    }
}

@Composable
private fun CustomerInfoChip(icon: ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerSummaryRow(
    customers: List<CustomerBO>,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val pendingCount = customers.count { (it.pendingInvoices ?: 0) > 0 }
    val overdueCount = customers.count {
        (it.availableCredit ?: 0.0) < 0 || (it.currentBalance ?: 0.0) > 0
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryTile(
            icon = Icons.Filled.People,
            label = strings.customer.title,
            value = "${customers.size}",
            color = MaterialTheme.colorScheme.primary
        )
        SummaryTile(
            icon = Icons.Filled.ReceiptLong,
            label = strings.customer.pendingLabel,
            value = "$pendingCount",
            color = MaterialTheme.colorScheme.tertiary
        )
        SummaryTile(
            icon = Icons.Filled.Warning,
            label = strings.customer.overdueLabel,
            value = "$overdueCount",
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun SummaryTile(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                Text(value, style = MaterialTheme.typography.titleMedium, color = color)
            }
        }
    }
}

@Composable
private fun FilterSummaryTile(
    label: String,
    value: String,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val background = if (selected) color.copy(alpha = 0.18f) else color.copy(alpha = 0.08f)
    Surface(
        color = background,
        shape = RoundedCornerShape(18.dp),
        // tonalElevation = 0.dp,
        border = if (selected) BorderStroke(1.dp, color.copy(alpha = 0.4f)) else null,
        modifier = Modifier.clickable { onClick() }.clip(RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 0.dp
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
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
    val strings = LocalAppStrings.current
    var selectedInvoice by remember { mutableStateOf<SalesInvoiceBO?>(null) }
    var amountRaw by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf(0.0) }
    val posBaseCurrency = paymentState.baseCurrency.ifBlank { "USD" }
    val invoiceBaseCurrency = selectedInvoice?.partyAccountCurrency
        ?.trim()
        ?.uppercase()
        ?.takeIf { it.isNotBlank() }
        ?: posBaseCurrency
    val allowedCodes = remember(paymentState.allowedCurrencies, posBaseCurrency) {
        val codes = paymentState.allowedCurrencies.map { it.code }.filter { it.isNotBlank() }
        val normalizedBase = posBaseCurrency.trim().uppercase()
        val supported = codes.filter { code ->
            code.equals(normalizedBase, ignoreCase = true) || (code.equals(
                "USD",
                ignoreCase = true
            ) && !normalizedBase.equals("USD", true))
        }
        val fallback = if (supported.isNotEmpty()) supported else listOf(normalizedBase)
        if (fallback.any { it.equals(normalizedBase, ignoreCase = true) }) {
            fallback.distinct()
        } else {
            (fallback + normalizedBase).distinct()
        }
    }
    var selectedCurrency by remember(allowedCodes, posBaseCurrency) {
        mutableStateOf(allowedCodes.firstOrNull { it.equals(posBaseCurrency, ignoreCase = true) }
            ?: allowedCodes.firstOrNull() ?: posBaseCurrency)
    }
    val paymentModes = remember(paymentState.paymentModes) {
        paymentState.paymentModes.map { it.modeOfPayment }.distinct()
    }
    val defaultMode = paymentModes.firstOrNull().orEmpty()
    var paymentMode by remember(paymentModes, defaultMode) { mutableStateOf(defaultMode) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }

    val exchangeRate = remember(selectedCurrency, posBaseCurrency, paymentState.exchangeRate) {
        val normalizedBase = posBaseCurrency.trim().uppercase()
        val normalizedSelected = selectedCurrency.trim().uppercase()
        when {
            normalizedSelected == normalizedBase -> 1.0
            invoicesState is CustomerInvoicesState.Success ->
                invoicesState.exchangeRateByCurrency[normalizedSelected]

            else -> null
        }
    }
    val conversionError = exchangeRate == null
    val baseAmount = exchangeRate?.let { rate ->
        if (rate <= 0.0) 0.0 else amountValue / rate
    } ?: 0.0
    val outstandingBase = selectedInvoice?.outstandingAmount ?: 0.0
    val outstandingInSelectedCurrency = exchangeRate?.let { rate ->
        outstandingBase * rate
    }
    val changeDue = outstandingInSelectedCurrency?.let { amountValue - it } ?: 0.0
    val isSubmitEnabled = !paymentState.isSubmitting &&
            selectedInvoice?.invoiceId?.isNotBlank() == true &&
            paymentMode.isNotBlank() &&
            amountValue > 0.0 &&
            !conversionError

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
                CustomerInvoicesState.Idle -> {
                    Text(
                        text = strings.customer.selectCustomerToViewInvoices,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                CustomerInvoicesState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CustomerInvoicesState.Error -> {
                    Text(
                        text = invoicesState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is CustomerInvoicesState.Success -> {
                    if (invoicesState.invoices.isEmpty()) {
                        Text(
                            text = "No outstanding invoices for this customer.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(invoicesState.invoices, key = { it.invoiceId }) { invoice ->
                                val isSelected = invoice.invoiceId == selectedInvoice?.invoiceId
                                val invoiceBaseCurrency = invoice.partyAccountCurrency
                                    ?.trim()
                                    ?.uppercase()
                                    ?.takeIf { it.isNotBlank() }
                                    ?: posBaseCurrency
                                val invoiceCurrency = invoice.currency?.trim()?.uppercase()
                                    ?.takeIf { it.isNotBlank() }
                                    ?: invoiceBaseCurrency
                                val baseSymbol = invoiceBaseCurrency.toCurrencySymbol().ifBlank {
                                    invoiceBaseCurrency
                                }
                                val invoiceSymbol = invoiceCurrency.toCurrencySymbol().ifBlank {
                                    invoiceCurrency
                                }
                                val convertedOutstanding = if (invoiceCurrency.equals(
                                        invoiceBaseCurrency,
                                        ignoreCase = true
                                    )
                                ) {
                                    invoice.outstandingAmount
                                } else {
                                    invoicesState.exchangeRateByCurrency[invoiceCurrency]
                                        ?.takeIf { it > 0.0 }
                                        ?.let { rate -> invoice.outstandingAmount * rate }
                                }
                                val outstandingLabel = if (convertedOutstanding != null) {
                                    "$invoiceSymbol ${bd(convertedOutstanding).moneyScale(2)}"
                                } else {
                                    "$baseSymbol ${bd(invoice.outstandingAmount).moneyScale(2)}"
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedInvoice = invoice
                                                val amountToUse =
                                                    convertedOutstanding
                                                        ?: invoice.outstandingAmount
                                                amountRaw = amountToUse.toString()
                                                val preferredCurrency =
                                                    if (convertedOutstanding != null) {
                                                        invoiceCurrency
                                                    } else {
                                                        invoiceBaseCurrency
                                                    }
                                                if (allowedCodes.any {
                                                        it.equals(
                                                            preferredCurrency,
                                                            ignoreCase = true
                                                        )
                                                    }
                                                ) {
                                                    selectedCurrency = preferredCurrency
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = invoice.invoiceId,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${strings.customer.postedLabel}: ${invoice.postingDate}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedInvoice = invoice
                                                    val amountToUse =
                                                        convertedOutstanding
                                                            ?: invoice.outstandingAmount
                                                    amountRaw = amountToUse.toString()
                                                    val preferredCurrency =
                                                        if (convertedOutstanding != null) {
                                                            invoiceCurrency
                                                        } else {
                                                            invoiceBaseCurrency
                                                        }
                                                    if (allowedCodes.any {
                                                            it.equals(
                                                                preferredCurrency,
                                                                ignoreCase = true
                                                            )
                                                        }
                                                    ) {
                                                        selectedCurrency = preferredCurrency
                                                    }
                                                }
                                            )
                                        }
                                        Text(
                                            text = "${strings.customer.outstandingLabel}: $outstandingLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (!invoiceCurrency.equals(
                                                invoiceBaseCurrency,
                                                ignoreCase = true
                                            )
                                        ) {
                                            val baseLabel =
                                                "$baseSymbol ${
                                                    bd(invoice.outstandingAmount).moneyScale(
                                                        2
                                                    )
                                                }"
                                            val helperText =
                                                if (convertedOutstanding != null) {
                                                    "${strings.customer.baseCurrency}: $baseLabel"
                                                } else {
                                                    "Exchange rate unavailable. Base: $baseLabel"
                                                }
                                            Text(
                                                text = helperText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = strings.customer.registerPaymentTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(strings.customer.paymentModeLabel, style = MaterialTheme.typography.bodyMedium)
            ExposedDropdownMenuBox(
                expanded = modeExpanded,
                onExpandedChange = { modeExpanded = it }
            ) {
                AppTextField(
                    value = paymentMode,
                    onValueChange = {},
                    label = "Select payment mode",
                    placeholder = "Select payment mode",
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = modeExpanded,
                    onDismissRequest = { modeExpanded = false }
                ) {
                    paymentModes.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = {
                                paymentMode = mode
                                modeExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Payment currency", style = MaterialTheme.typography.bodyMedium)
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it }
            ) {
                AppTextField(
                    value = selectedCurrency,
                    onValueChange = {},
                    label = "Select currency",
                    placeholder = "Select currency",
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ) {
                    allowedCodes.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                selectedCurrency = currency
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

            MoneyTextField(
                currencyCode = selectedCurrency,
                rawValue = amountRaw,
                onRawValueChange = { amountRaw = it },
                label = strings.customer.amountLabel,
                onAmountChanged = { amountValue = it },
                supportingText = {
                    if (conversionError) {
                        Text(
                            text = "Exchange rate unavailable for $selectedCurrency to $invoiceBaseCurrency.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (!selectedCurrency.equals(invoiceBaseCurrency, ignoreCase = true)) {
                        val symbol =
                            invoiceBaseCurrency.toCurrencySymbol().ifBlank { invoiceBaseCurrency }
                        Text("POS base: $symbol$baseAmount")
                    }
                }
            )

            if (changeDue > 0.0) {
                val currencySymbol =
                    selectedCurrency.toCurrencySymbol().ifBlank { selectedCurrency }
                Text(
                    text = "Change due: $currencySymbol ${bd(changeDue).moneyScale(2)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

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
                    val invoiceId = selectedInvoice?.invoiceId?.trim().orEmpty()
                    val amount = minOf(baseAmount, outstandingBase)
                    onRegisterPayment(invoiceId, paymentMode, amount)
                },
                enabled = isSubmitEnabled
            ) {
                Text(
                    if (paymentState.isSubmitting)
                        strings.customer.processing
                    else strings.customer.registerPaymentButton
                )
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
                        image = "https://images.unsplash.com/photo-1708467374959-e5588da12e8f?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA==",
                        customerType = "Individual",
                        currentBalance = 13450.0,
                        pendingInvoices = 2,
                        availableCredit = 0.0,
                        address = "Residencial Palmanova #117",
                    )
                ), 10, 5
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
