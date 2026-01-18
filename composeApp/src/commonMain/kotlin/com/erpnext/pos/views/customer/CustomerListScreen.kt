package com.erpnext.pos.views.customer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.erpnext.pos.domain.models.CustomerCounts
import com.erpnext.pos.domain.models.CustomerQuickActionType
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.utils.QuickActions.customerQuickActions
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.resolveRateBetweenFromBaseRates
import com.erpnext.pos.utils.resolvePaymentCurrencyForMode
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.views.billing.AppTextField
import com.erpnext.pos.views.billing.MoneyTextField
import org.koin.compose.koinInject

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
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("Todos") }
    var quickActionsCustomer by remember { mutableStateOf<CustomerBO?>(null) }
    var outstandingCustomer by remember { mutableStateOf<CustomerBO?>(null) }

    val snackbar: SnackbarController = koinInject()
    val cashboxManager: CashBoxManager = koinInject()
    val posContext = cashboxManager.getContext()
    val customers = if (state is CustomerState.Success) {
        state.customers.sortedWith(
            compareByDescending<CustomerBO> {
                val pendingAmount = it.totalPendingAmount ?: it.currentBalance ?: 0.0
                (it.pendingInvoices ?: 0) > 0 || pendingAmount > 0.0
            }.thenByDescending { it.totalPendingAmount ?: it.currentBalance ?: 0.0 }
                .thenBy { it.customerName }
        )
    } else emptyList()
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

    LaunchedEffect(paymentState.successMessage) {
        paymentState.successMessage?.takeIf { it.isNotBlank() }?.let { message ->
            snackbar.show(message, SnackbarType.Success, position = SnackbarPosition.Top)
            actions.clearPaymentMessages()
        }
    }

    LaunchedEffect(paymentState.errorMessage) {
        paymentState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            snackbar.show(message, SnackbarType.Error, position = SnackbarPosition.Top)
            actions.clearPaymentMessages()
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    when (state) {
                        is CustomerState.Loading -> {
                            CustomerShimmerList()
                        }

                        is CustomerState.Empty -> {
                            EmptyStateMessage(
                                message = strings.customer.emptyCustomers,
                                icon = Icons.Filled.People
                            )
                        }

                        is CustomerState.Error -> {
                            FullScreenErrorMessage(
                                errorMessage = state.message,
                                onRetry = actions.fetchAll
                            )
                        }

                        is CustomerState.Success -> {
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
                                val posCurrency = normalizeCurrency(posContext?.currency) ?: "USD"
                                val partyCurrency = normalizeCurrency(paymentState.partyAccountCurrency)

                                CustomerListContent(
                                    customers = filtered,
                                    posCurrency = posCurrency,
                                    partyAccountCurrency = partyCurrency,
                                    posExchangeRate = posContext?.exchangeRate
                                        ?: paymentState.exchangeRate,
                                    cashboxManager = cashboxManager,
                                    actions = actions,
                                    isWideLayout = isWideLayout,
                                    isDesktop = isDesktop,
                                    onOpenQuickActions = { quickActionsCustomer = it },
                                    onQuickAction = { customer, actionType ->
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
                    }
                }
            }
        }
    }

    quickActionsCustomer?.let { customer ->
        CustomerQuickActionsSheet(
            customer = customer,
            invoicesState = invoicesState,
            paymentState = paymentState,
            onDismiss = {  },
            onActionSelected = { actionType ->
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
            onRegisterPayment = { invoiceId, mode, enteredAmount, enteredCurrency, referenceNumber ->
                actions.registerPayment(
                    customer.name,
                    invoiceId,
                    mode,
                    enteredAmount,
                    enteredCurrency,
                    referenceNumber
                )
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
    posCurrency: String,
    partyAccountCurrency: String,
    posExchangeRate: Double,
    cashboxManager: CashBoxManager,
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
                    posCurrency = posCurrency,
                    partyAccountCurrency = partyAccountCurrency,
                    posExchangeRate = posExchangeRate,
                    isDesktop = isDesktop,
                    onClick = { actions.toDetails(customer.name) },
                    onOpenQuickActions = { onOpenQuickActions(customer) },
                    onQuickAction = { actionType -> onQuickAction(customer, actionType) },
                    cashboxManager = cashboxManager
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
                    posCurrency = posCurrency,
                    partyAccountCurrency = partyAccountCurrency,
                    posExchangeRate = posExchangeRate,
                    isDesktop = isDesktop,
                    onClick = { actions.toDetails(customer.name) },
                    onOpenQuickActions = { onOpenQuickActions(customer) },
                    onQuickAction = { actionType -> onQuickAction(customer, actionType) },
                    cashboxManager = cashboxManager
                )
            }
        }
    }
}

@Composable
fun CustomerItem(
    customer: CustomerBO,
    posCurrency: String,
    partyAccountCurrency: String,
    posExchangeRate: Double,
    cashboxManager: CashBoxManager,
    isDesktop: Boolean,
    onClick: () -> Unit,
    onOpenQuickActions: () -> Unit,
    onQuickAction: (CustomerQuickActionType) -> Unit
) {
    val strings = LocalAppStrings.current
    val isOverLimit = (customer.availableCredit ?: 0.0) < 0 || (customer.currentBalance ?: 0.0) > 0
    val pendingInvoices = customer.pendingInvoices ?: 0
    val baseCurrency = normalizeCurrency(partyAccountCurrency)
    var isMenuExpanded by remember { mutableStateOf(false) }
    val quickActions = remember { customerQuickActions() }
    val avatarSize = if (isDesktop) 52.dp else 44.dp
    val pendingAmountRaw =
        bd(customer.totalPendingAmount ?: 0.0).moneyScale(2)
    val pendingAmount = pendingAmountRaw.toDouble(2)
    val posCurr = normalizeCurrency(posCurrency)
    var rateToPos by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(baseCurrency, posCurr, pendingAmount) {
        rateToPos = if (baseCurrency.equals(posCurr, ignoreCase = true)) {
            1.0
        } else {
            cashboxManager.resolveExchangeRateBetween(
                fromCurrency = baseCurrency,
                toCurrency = posCurr
            ) ?: run {
                when {
                    baseCurrency.equals("USD", true) -> posExchangeRate.takeIf { it > 0.0 }
                    posCurr.equals("USD", true) && posExchangeRate > 0.0 -> 1 / posExchangeRate
                    else -> null
                }
            }
        }
    }
    val posAmount = rateToPos?.let { rate -> pendingAmount * rate }
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
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .height(180.dp)
            .clip(cardShape)
            .pointerInput(customer.name, isDesktop) {
                if (!isDesktop) {
                    val totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (kotlin.math.abs(totalDrag) > 64) {
                                onOpenQuickActions()
                            }
                        },
                        onHorizontalDrag = { _, _ ->
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
                                .data(customer.image.ifBlank { "https://placehold.co/600x400" })
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
                    val pendingPrimaryAmount = posAmount ?: pendingAmount
                    val pendingPrimaryCurrency = if (posAmount != null) posCurr else baseCurrency
                    val pendingPrimarySymbol =
                        pendingPrimaryCurrency.toCurrencySymbol().ifBlank { pendingPrimaryCurrency }

                    MetricBlock(
                        label = strings.customer.pendingLabel,
                        value = "$pendingPrimarySymbol ${formatAmount(pendingPrimaryAmount)}",
                        isCritical = emphasis
                    )
                    MetricBlock(
                        label = strings.customer.outstandingSummaryInvoicesLabel,
                        value = "$pendingInvoices",
                        isCritical = pendingInvoices > 0
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
                    if (customer.mobileNo?.isNotEmpty() == true) {
                        CustomerInfoChip(
                            icon = Icons.Filled.Phone,
                            text = customer.mobileNo
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
fun MetricBlock(
    label: String,
    value: String,
    secondaryValue: String? = null,
    isCritical: Boolean
) {
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
            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    secondaryValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
private fun FilterSummaryTile(
    label: String,
    value: String,
    selected: Boolean,
    color: Color,
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
    invoicesState: CustomerInvoicesState,
    paymentState: CustomerPaymentState,
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
            CustomerOutstandingSummary(
                customer = customer,
                invoices = if (invoicesState is CustomerInvoicesState.Success) {
                    invoicesState.invoices
                } else emptyList(),
                posBaseCurrency = paymentState.baseCurrency,
                baseRates = if (invoicesState is CustomerInvoicesState.Success) {
                    invoicesState.exchangeRateByCurrency
                } else emptyMap()
            )

            HorizontalDivider()

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
    onRegisterPayment: (
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        referenceNumber: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedInvoice by remember { mutableStateOf<SalesInvoiceBO?>(null) }
    var amountRaw by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf(0.0) }
    val posBaseCurrency = normalizeCurrency(paymentState.baseCurrency)
    val receivableCurrency = normalizeCurrency(selectedInvoice?.partyAccountCurrency)
        ?: posBaseCurrency
    val invoiceBaseCurrency = normalizeCurrency(selectedInvoice?.currency)
        ?: posBaseCurrency
    val invoiceToReceivableRate = when {
        invoiceBaseCurrency.equals(receivableCurrency, ignoreCase = true) -> 1.0
        selectedInvoice?.conversionRate != null && (selectedInvoice?.conversionRate ?: 0.0) > 0.0 ->
            selectedInvoice?.conversionRate
        selectedInvoice?.customExchangeRate != null &&
            (selectedInvoice?.customExchangeRate ?: 0.0) > 0.0 ->
            selectedInvoice?.customExchangeRate
        else -> null
    }
    val paymentModes = paymentState.paymentModes
    val modeOptions = remember(paymentModes) { paymentModes.map { it.modeOfPayment }.distinct() }
    val defaultMode = paymentModes.firstOrNull()?.modeOfPayment.orEmpty()
    var selectedMode by remember(modeOptions, defaultMode) { mutableStateOf(defaultMode) }
    val selectedModeOption = paymentModes.firstOrNull { it.modeOfPayment == selectedMode }
    val requiresReference = remember(selectedModeOption) {
        requiresReference(selectedModeOption)
    }
    var referenceInput by remember { mutableStateOf("") }

    LaunchedEffect(selectedMode) {
        referenceInput = ""
    }

    var selectedCurrency by remember { mutableStateOf(invoiceBaseCurrency) }
    LaunchedEffect(selectedMode, invoiceBaseCurrency) {
        selectedCurrency = resolvePaymentCurrencyForMode(
            modeOfPayment = selectedMode,
            invoiceCurrency = invoiceBaseCurrency,
            paymentModeCurrencyByMode = paymentState.paymentModeCurrencyByMode,
            paymentModeDetails = paymentState.modeTypes ?: mapOf()
        )
    }

    var modeExpanded by remember { mutableStateOf(false) }

    // Exchange rate de invoiceBaseCurrency -> paymentCurrency usando baseRates
    val exchangeRate =
        remember(selectedCurrency, invoiceBaseCurrency, posBaseCurrency) {
            val from = normalizeCurrency(invoiceBaseCurrency)
            val to = normalizeCurrency(selectedCurrency)
            when {
                from == to -> 1.0
                invoicesState is CustomerInvoicesState.Success -> {
                    resolveRateBetweenFromBaseRates(
                        fromCurrency = from,
                        toCurrency = to,
                        baseCurrency = posBaseCurrency,
                        baseRates = invoicesState.exchangeRateByCurrency
                    )
                }

                else -> null
            }
        }

    val conversionError = exchangeRate == null

    // Base amount = monto pagado convertido a moneda base de la factura
    val baseAmount = exchangeRate?.let { rate ->
        if (rate <= 0.0) 0.0 else amountValue / rate
    } ?: 0.0

    val outstandingReceivable = selectedInvoice?.outstandingAmount ?: 0.0
    val outstandingBase = if (!invoiceBaseCurrency.equals(receivableCurrency, ignoreCase = true)) {
        val rate = invoiceToReceivableRate?.takeIf { it > 0.0 }
        if (rate != null) outstandingReceivable / rate else outstandingReceivable
    } else {
        outstandingReceivable
    }
    val outstandingInSelectedCurrency = exchangeRate?.let { rate ->
        outstandingBase * rate
    }
    val rateBaseToPos = if (posBaseCurrency.equals(invoiceBaseCurrency, ignoreCase = true)) {
        1.0
    } else {
        resolveRateBetweenFromBaseRates(
            fromCurrency = invoiceBaseCurrency,
            toCurrency = posBaseCurrency,
            baseCurrency = posBaseCurrency,
            baseRates = when (invoicesState) {
                is CustomerInvoicesState.Success -> invoicesState.exchangeRateByCurrency
                else -> emptyMap()
            }
        )
    }
    val receivableSymbol = receivableCurrency.toCurrencySymbol().ifBlank { receivableCurrency }
    val baseOutstandingLabel = "$receivableSymbol ${formatAmount(outstandingReceivable)}"
    val changeDue = outstandingInSelectedCurrency?.let { amountValue - it } ?: 0.0
    val isSubmitEnabled = !paymentState.isSubmitting &&
            selectedInvoice?.invoiceId?.isNotBlank() == true &&
            selectedMode.isNotBlank() &&
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
                text = "${strings.customer.outstandingInvoicesTitle} - ${customer.customerName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            // ====== LISTADO DE FACTURAS ======
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
                            text = strings.customer.emptyOsInvoices,
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
                                val invoiceBaseCurrency =
                                    normalizeCurrency(invoice.partyAccountCurrency)
                                        ?: posBaseCurrency
                                val baseSymbol = invoiceBaseCurrency.toCurrencySymbol().ifBlank {
                                    invoiceBaseCurrency
                                }
                                val rateBaseToPos = if (posBaseCurrency.equals(
                                        invoiceBaseCurrency,
                                        ignoreCase = true
                                    )
                                ) {
                                    1.0
                                } else {
                                    resolveRateBetweenFromBaseRates(
                                        fromCurrency = invoiceBaseCurrency,
                                        toCurrency = posBaseCurrency,
                                        baseCurrency = posBaseCurrency,
                                        baseRates = invoicesState.exchangeRateByCurrency
                                    )
                                }
                                val posSymbol = posBaseCurrency.toCurrencySymbol()
                                    .ifBlank { posBaseCurrency }
                                val posLabel = rateBaseToPos?.let { rate ->
                                    "$posSymbol ${formatAmount(invoice.outstandingAmount * rate)}"
                                } ?: "$posSymbol --"
                                val baseLabel = "$baseSymbol ${formatAmount(invoice.outstandingAmount)}"

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
                                                val rateBaseToSelected =
                                                    resolveRateBetweenFromBaseRates(
                                                        fromCurrency = invoiceBaseCurrency,
                                                        toCurrency = selectedCurrency,
                                                        baseCurrency = posBaseCurrency,
                                                        baseRates = invoicesState.exchangeRateByCurrency
                                                    )
                                                val amountToUse = rateBaseToSelected?.let { rate ->
                                                    invoice.outstandingAmount * rate
                                                } ?: invoice.outstandingAmount
                                                amountRaw = amountToUse.toString()
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                when (invoice.syncStatus) {
                                                    "Pending" -> {
                                                        AssistChip(
                                                            onClick = {},
                                                            label = { Text("Pendiente sync") }
                                                        )
                                                    }

                                                    "Failed" -> {
                                                        AssistChip(
                                                            onClick = {},
                                                            label = { Text("Sync falló") },
                                                            colors = AssistChipDefaults.assistChipColors(
                                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                                                            )
                                                        )
                                                    }
                                                }
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedInvoice = invoice
                                                        val rateBaseToSelected =
                                                            resolveRateBetweenFromBaseRates(
                                                                fromCurrency = invoiceBaseCurrency,
                                                                toCurrency = selectedCurrency,
                                                                baseCurrency = posBaseCurrency,
                                                                baseRates = invoicesState.exchangeRateByCurrency
                                                            )
                                                        val amountToUse = rateBaseToSelected?.let { rate ->
                                                            invoice.outstandingAmount * rate
                                                        } ?: invoice.outstandingAmount
                                                        amountRaw = amountToUse.toString()
                                                    }
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${strings.customer.outstandingLabel}: $posLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (!invoiceBaseCurrency.equals(
                                                posBaseCurrency,
                                                ignoreCase = true
                                            )
                                        ) {
                                            Text(
                                                text = "${strings.customer.baseCurrency}: $baseLabel",
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

            // ====== REGISTRAR PAGO ======
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
                    value = selectedMode,
                    onValueChange = {},
                    label = strings.customer.selectPaymentMode,
                    placeholder = strings.customer.selectPaymentMode,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
                            text = { Text(mode.name) },
                            onClick = {
                                selectedMode = mode.name
                                selectedCurrency = resolvePaymentCurrencyForMode(
                                    modeOfPayment = mode.modeOfPayment,
                                    invoiceCurrency = invoiceBaseCurrency,
                                    paymentModeCurrencyByMode = paymentState.paymentModeCurrencyByMode,
                                    paymentModeDetails = paymentState.modeTypes ?: mapOf()
                                )
                                modeExpanded = false
                            }
                        )
                    }
                }
            }

            if (requiresReference) {
                AppTextField(
                    value = referenceInput,
                    onValueChange = { referenceInput = it },
                    label = "Número de referencia",
                    placeholder = "#11231",
                    leadingIcon = {
                        Icon(
                            Icons.Default.ConfirmationNumber,
                            contentDescription = null
                        )
                    },
                    supportingText = {
                        if (referenceInput.isBlank()) {
                            Text("Requerido para pagos con ${selectedMode}.")
                        }
                    },
                    isError = referenceInput.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
            }

            /*Text("Payment currency", style = MaterialTheme.typography.bodyMedium)
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
            }*/

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
                        Text("Base: $symbol ${formatAmount(baseAmount)}")
                    }
                }
            )
            val posSymbol = posBaseCurrency.toCurrencySymbol().ifBlank { posBaseCurrency }
            val posOutstandingLabel = rateBaseToPos?.let { rate ->
                "$posSymbol ${formatAmount(outstandingBase * rate)}"
            } ?: "$posSymbol --"
            Text(
                text = "${strings.customer.outstandingLabel}: $posOutstandingLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!receivableCurrency.equals(posBaseCurrency, ignoreCase = true)) {
                Text(
                    text = "${strings.customer.baseCurrency}: $baseOutstandingLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (changeDue > 0.0) {
                val currencySymbol =
                    selectedCurrency.toCurrencySymbol().ifBlank { selectedCurrency }
                Text(
                    text = "Change due: $currencySymbol ${formatAmount(changeDue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    val invoiceId = selectedInvoice?.invoiceId?.trim().orEmpty()
                    onRegisterPayment(
                        invoiceId,
                        selectedMode,
                        amountValue,
                        selectedCurrency,
                        referenceInput
                    )
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

fun requiresReference(option: POSPaymentModeOption?): Boolean {
    val type = option?.type?.trim().orEmpty()
    return type.equals("Bank", ignoreCase = true) || type.equals(
        "Card", ignoreCase = true
    ) || option?.modeOfPayment?.contains(
        "bank", ignoreCase = true
    ) == true || option?.modeOfPayment?.contains("card", ignoreCase = true) == true
}

@Composable
private fun CustomerOutstandingSummary(
    customer: CustomerBO,
    invoices: List<SalesInvoiceBO>,
    posBaseCurrency: String,
    baseRates: Map<String, Double>
) {
    val strings = LocalAppStrings.current
    val posCurrency = normalizeCurrency(posBaseCurrency)
    val partyTotals = if (invoices.isNotEmpty()) {
        invoices.groupBy { invoice ->
            normalizeCurrency(invoice.partyAccountCurrency)
                ?: normalizeCurrency(invoice.currency)
                ?: posCurrency
        }.mapValues { (_, list) ->
            list.sumOf { it.outstandingAmount }
        }
    } else {
        val fallbackCurrency = normalizeCurrency(customer.currency)
            .takeIf { it.isNotBlank() } ?: posCurrency
        val fallbackAmount = customer.totalPendingAmount
            ?: customer.currentBalance ?: 0.0
        mapOf(fallbackCurrency to fallbackAmount)
    }

    val customerCurrency = partyTotals.keys.firstOrNull() ?: posCurrency
    val customerAmount = partyTotals.getOrElse(customerCurrency) { 0.0 }
    val totalInPos = if (invoices.isNotEmpty()) {
        invoices.sumOf { invoice ->
            val invoiceCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                ?: normalizeCurrency(invoice.currency) ?: posCurrency
            val rateToPos = resolveRateBetweenFromBaseRates(
                fromCurrency = invoiceCurrency,
                toCurrency = posCurrency,
                baseCurrency = posCurrency,
                baseRates = baseRates
            )
            if (rateToPos != null) invoice.outstandingAmount * rateToPos
            else invoice.outstandingAmount
        }
    } else {
        customerAmount
    }

    val posSymbol = posCurrency.toCurrencySymbol().ifBlank { posCurrency }
    val posLabel = "$posSymbol ${formatAmount(totalInPos)}"

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
            text = strings.customer.outstandingSummaryTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(strings.customer.outstandingSummaryInvoicesLabel)
            Text("${customer.pendingInvoices ?: 0}")
        }
        partyTotals.forEach { (currency, amount) ->
            val symbol = currency.toCurrencySymbol().ifBlank { currency }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val label = if (currency.equals(customerCurrency, ignoreCase = true)) {
                    strings.customer.outstandingSummaryAmountLabel
                } else {
                    currency
                }
                Text(label)
                Text("$symbol ${formatAmount(amount)}")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(strings.customer.baseCurrency)
            Text(posLabel)
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

private fun formatAmount(value: Double): String = formatDoubleToString(bd(value).toDouble(0), 2)
