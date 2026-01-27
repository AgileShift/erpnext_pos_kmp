@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import com.erpnext.pos.domain.usecases.CreateCustomerInput
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.localization.LocalAppStrings
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
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.AppTextField
import com.erpnext.pos.views.billing.MoneyTextField
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    state: CustomerState,
    invoicesState: CustomerInvoicesState,
    paymentState: CustomerPaymentState,
    historyState: CustomerInvoiceHistoryState,
    historyMessage: String?,
    historyBusy: Boolean,
    customerMessage: String?,
    dialogDataState: CustomerDialogDataState,
    actions: CustomerAction
) {
    val strings = LocalAppStrings.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedState by rememberSaveable { mutableStateOf("Todos") }
    var quickActionsCustomer by remember { mutableStateOf<CustomerBO?>(null) }
    var outstandingCustomer by remember { mutableStateOf<CustomerBO?>(null) }
    var historyCustomer by remember { mutableStateOf<CustomerBO?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerBO?>(null) }
    var rightPanelTab by rememberSaveable { mutableStateOf(CustomerPanelTab.Details) }
    var allowSheets by remember { mutableStateOf(true) }
    var showNewCustomerDialog by rememberSaveable { mutableStateOf(false) }

    val snackbar: SnackbarController = koinInject()
    val cashboxManager: CashBoxManager = koinInject()
    val posContext = cashboxManager.getContext()
    val posCurrency = normalizeCurrency(posContext?.currency) ?: "USD"
    val partyCurrency = normalizeCurrency(paymentState.partyAccountCurrency)
    val supportedCurrencies = remember(
        posContext?.allowedCurrencies,
        posContext?.currency,
        paymentState.partyAccountCurrency
    ) {
        val fromModes = posContext?.allowedCurrencies
            ?.mapNotNull { it.code }
            .orEmpty()
        val merged = (fromModes + listOfNotNull(
            posContext?.currency,
            paymentState.partyAccountCurrency
        )).mapNotNull { normalizeCurrency(it) }
            .distinct()
        merged.ifEmpty { listOf(posCurrency) }
    }
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

    LaunchedEffect(historyCustomer?.name) {
        historyCustomer?.let { actions.onViewInvoiceHistory(it) }
    }

    LaunchedEffect(historyMessage) {
        historyMessage?.takeIf { it.isNotBlank() }?.let { message ->
            snackbar.show(message, SnackbarType.Success, position = SnackbarPosition.Top)
            actions.clearInvoiceHistoryMessages()
        }
    }

    LaunchedEffect(customerMessage) {
        customerMessage?.takeIf { it.isNotBlank() }?.let { message ->
            snackbar.show(message, SnackbarType.Success, position = SnackbarPosition.Top)
            actions.clearCustomerMessages()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewCustomerDialog = true }
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo cliente")
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val isWideLayout = maxWidth >= 840.dp || isDesktop
            val contentPadding = if (isWideLayout) 24.dp else 16.dp
            SideEffect {
                allowSheets = !isWideLayout
            }

            LaunchedEffect(isWideLayout, selectedCustomer?.name, rightPanelTab) {
                if (!isWideLayout) return@LaunchedEffect
                val customer = selectedCustomer ?: return@LaunchedEffect
                when (rightPanelTab) {
                    CustomerPanelTab.Pending -> actions.loadOutstandingInvoices(customer)
                    CustomerPanelTab.History -> actions.onViewInvoiceHistory(customer)
                    else -> Unit
                }
            }

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
                        },
                        onStateChange = {
                            selectedState = it ?: "Todos"
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
                                val matchesQuery =
                                    customer.customerName.contains(searchQuery, ignoreCase = true) ||
                                        (customer.mobileNo ?: "").contains(searchQuery)
                                val matchesState = when (selectedState) {
                                    "Pendientes" -> (customer.pendingInvoices ?: 0) > 0 ||
                                        (customer.totalPendingAmount ?: 0.0) > 0.0
                                    "Sin Pendientes" -> (customer.pendingInvoices ?: 0) == 0 &&
                                        (customer.totalPendingAmount ?: 0.0) <= 0.0
                                    "Todos", null -> true
                                    else -> customer.state.equals(selectedState, ignoreCase = true)
                                }
                                matchesQuery && matchesState
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
                                LaunchedEffect(filtered, isWideLayout) {
                                    if (isWideLayout && selectedCustomer !in filtered) {
                                        selectedCustomer = filtered.firstOrNull()
                                    }
                                }
                                if (isWideLayout) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(0.65f)) {
                                            CustomerListContent(
                                                customers = filtered,
                                                posCurrency = posCurrency,
                                                partyAccountCurrency = partyCurrency,
                                                posExchangeRate = posContext?.exchangeRate
                                                    ?: paymentState.exchangeRate,
                                                cashboxManager = cashboxManager,
                                                supportedCurrencies = supportedCurrencies,
                                                isWideLayout = false,
                                                isDesktop = isDesktop,
                                                onOpenQuickActions = {
                                                    selectedCustomer = it
                                                    rightPanelTab = CustomerPanelTab.Details
                                                },
                                                onSelect = {
                                                    selectedCustomer = it
                                                },
                                                onQuickAction = { customer, actionType ->
                                                    when (actionType) {
                                                        CustomerQuickActionType.PendingInvoices,
                                                        CustomerQuickActionType.RegisterPayment -> {
                                                            selectedCustomer = customer
                                                            rightPanelTab = CustomerPanelTab.Pending
                                                        }

                                                        CustomerQuickActionType.InvoiceHistory -> {
                                                            selectedCustomer = customer
                                                            rightPanelTab = CustomerPanelTab.History
                                                        }

                                                        else -> handleQuickAction(
                                                            actions,
                                                            customer,
                                                            actionType
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1.35f),
                                            shape = RoundedCornerShape(18.dp),
                                            tonalElevation = 1.dp,
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            CustomerRightPanel(
                                                customer = selectedCustomer,
                                                rightPanelTab = rightPanelTab,
                                                onTabChange = { rightPanelTab = it },
                                                paymentState = paymentState,
                                                invoicesState = invoicesState,
                                                historyState = historyState,
                                                historyMessage = historyMessage,
                                                historyBusy = historyBusy,
                                                supportedCurrencies = supportedCurrencies,
                                                cashboxManager = cashboxManager,
                                                posBaseCurrency = posCurrency,
                                                onRegisterPayment = { invoiceId, mode, enteredAmount, enteredCurrency, referenceNumber ->
                                                    actions.registerPayment(
                                                        selectedCustomer?.name.orEmpty(),
                                                        invoiceId,
                                                        mode,
                                                        enteredAmount,
                                                        enteredCurrency,
                                                        referenceNumber
                                                    )
                                                },
                                                onInvoiceHistoryAction = { invoiceId, action, refundMode, refundReference, applyRefund ->
                                                    actions.onInvoiceHistoryAction(
                                                        invoiceId,
                                                        action,
                                                        null,
                                                        refundMode,
                                                        refundReference,
                                                        applyRefund
                                                    )
                                                },
                                                loadLocalInvoice = actions.loadInvoiceLocal,
                                                onSubmitPartialReturn = actions.onInvoicePartialReturn,
                                                onOpenPending = {
                                                    rightPanelTab = CustomerPanelTab.Pending
                                                },
                                                onOpenHistory = {
                                                    rightPanelTab = CustomerPanelTab.History
                                                },
                                                onOpenRegisterPayment = {
                                                    rightPanelTab = CustomerPanelTab.Pending
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    CustomerListContent(
                                        customers = filtered,
                                        posCurrency = posCurrency,
                                        partyAccountCurrency = partyCurrency,
                                        posExchangeRate = posContext?.exchangeRate
                                            ?: paymentState.exchangeRate,
                                        cashboxManager = cashboxManager,
                                        supportedCurrencies = supportedCurrencies,
                                        isWideLayout = isWideLayout,
                                        isDesktop = isDesktop,
                                        onOpenQuickActions = { quickActionsCustomer = it },
                                        onSelect = {},
                                        onQuickAction = { customer, actionType ->
                                            when (actionType) {
                                                CustomerQuickActionType.PendingInvoices,
                                                CustomerQuickActionType.RegisterPayment -> {
                                                    outstandingCustomer = customer
                                                }

                                                CustomerQuickActionType.InvoiceHistory -> {
                                                    historyCustomer = customer
                                                }

                                                else -> handleQuickAction(
                                                    actions,
                                                    customer,
                                                    actionType
                                                )
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
    }

    if (allowSheets) {
        quickActionsCustomer?.let { customer ->
        CustomerQuickActionsSheet(
            customer = customer,
            invoicesState = invoicesState,
            paymentState = paymentState,
            supportedCurrencies = supportedCurrencies,
            cashboxManager = cashboxManager,
            onDismiss = { },
            onActionSelected = { actionType ->
                when (actionType) {
                    CustomerQuickActionType.PendingInvoices,
                    CustomerQuickActionType.RegisterPayment -> {
                        outstandingCustomer = customer
                    }

                    CustomerQuickActionType.InvoiceHistory -> {
                        historyCustomer = customer
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

        historyCustomer?.let { customer ->
            CustomerInvoiceHistorySheet(
                customer = customer,
                historyState = historyState,
                historyMessage = historyMessage,
                historyBusy = historyBusy,
                paymentState = paymentState,
                posBaseCurrency = posCurrency,
                supportedCurrencies = supportedCurrencies,
                cashboxManager = cashboxManager,
                onAction = { invoiceId, action, refundMode, refundReference, applyRefund ->
                    actions.onInvoiceHistoryAction(
                        invoiceId,
                        action,
                        null,
                        refundMode,
                        refundReference,
                        applyRefund
                    )
                },
                onDismiss = {
                    historyCustomer = null
                    actions.clearInvoiceHistory()
                },
                loadLocalInvoice = actions.loadInvoiceLocal,
                onSubmitPartialReturn = actions.onInvoicePartialReturn
            )
        }
    }

    if (showNewCustomerDialog) {
        NewCustomerDialog(
            onDismiss = { showNewCustomerDialog = false },
            onSubmit = { input -> actions.onCreateCustomer(input) },
            customerGroups = dialogDataState.customerGroups,
            territories = dialogDataState.territories,
            paymentTermsOptions = dialogDataState.paymentTerms,
            companies = dialogDataState.companies
        )
    }
}

private enum class CustomerPanelTab(val label: String) {
    Details("Resumen"),
    Pending("Pendientes"),
    History("Historial")
}

private enum class CustomerDialogTab(val label: String) {
    Personal("Principal"),
    Contact("Contacto"),
    Tax("Impuestos"),
    Accounting("Contabilidad")
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
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { query -> onSearchQueryChange(query) },
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                placeholderText,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onSearchQueryChange("")
                        keyboardController?.show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpiar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = if (onSearchAction != null) ImeAction.Search else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearchAction?.invoke()
                keyboardController?.hide()
            },
            onDone = { keyboardController?.hide() }
        ),
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
}

@Composable
private fun CustomerListContent(
    customers: List<CustomerBO>,
    posCurrency: String,
    partyAccountCurrency: String,
    posExchangeRate: Double,
    cashboxManager: CashBoxManager,
    supportedCurrencies: List<String>,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    onOpenQuickActions: (CustomerBO) -> Unit,
    onSelect: (CustomerBO) -> Unit,
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
                    supportedCurrencies = supportedCurrencies,
                    isDesktop = isDesktop,
                    onSelect = onSelect,
                    //onClick = { actions.toDetails(customer.name) },
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
                    supportedCurrencies = supportedCurrencies,
                    isDesktop = isDesktop,
                    onSelect = onSelect,
                    //onClick = { actions.toDetails(customer.name) },
                    onOpenQuickActions = { onOpenQuickActions(customer) },
                    onQuickAction = { actionType -> onQuickAction(customer, actionType) },
                    cashboxManager = cashboxManager
                )
            }
        }
    }
}

@Composable
private fun CustomerDetailPanel(
    customer: CustomerBO,
    paymentState: CustomerPaymentState,
    invoicesState: CustomerInvoicesState,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
    onOpenPending: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRegisterPayment: () -> Unit
) {
    val posCurrency = normalizeCurrency(paymentState.baseCurrency) ?: "USD"
    val baseCurrency = normalizeCurrency(paymentState.partyAccountCurrency) ?: posCurrency
    val pendingAmount = bd(customer.totalPendingAmount ?: customer.currentBalance ?: 0.0)
        .moneyScale(2).toDouble(2)
    val displayCurrencies = remember(supportedCurrencies, posCurrency, baseCurrency) {
        (supportedCurrencies + listOf(posCurrency, baseCurrency))
            .mapNotNull { normalizeCurrency(it) }
            .distinct()
    }
    var pendingByCurrency by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    LaunchedEffect(displayCurrencies, baseCurrency, pendingAmount) {
        val resolved = mutableMapOf<String, Double>()
        displayCurrencies.forEach { currency ->
            val rate = if (baseCurrency.equals(currency, ignoreCase = true)) {
                1.0
            } else {
                cashboxManager.resolveExchangeRateBetween(
                    fromCurrency = baseCurrency,
                    toCurrency = currency,
                    allowNetwork = false
                )
            }
            if (rate != null && rate > 0.0) {
                resolved[currency] = pendingAmount * rate
            }
        }
        pendingByCurrency = resolved
    }
    val primaryCurrency = when {
        pendingByCurrency.containsKey(posCurrency) -> posCurrency
        pendingByCurrency.containsKey(baseCurrency) -> baseCurrency
        else -> displayCurrencies.firstOrNull() ?: "USD"
    }
    val primarySymbol = primaryCurrency.toCurrencySymbol().ifBlank { primaryCurrency }
    val primaryAmount = pendingByCurrency[primaryCurrency] ?: pendingAmount
    val secondaryValue = pendingByCurrency
        .filterKeys { !it.equals(primaryCurrency, ignoreCase = true) }
        .map { (currency, amount) ->
            val symbol = currency.toCurrencySymbol().ifBlank { currency }
            "$symbol ${formatAmount(amount)}"
        }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ) {
                Text(
                    text = customer.customerName.take(1).uppercase(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    customer.customerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    customer.mobileNo ?: "Sin teléfono",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                label = if ((customer.pendingInvoices ?: 0) > 0) "Pendientes" else "Activo",
                isCritical = (customer.pendingInvoices ?: 0) > 0
            )
        }

        MetricBlock(
            label = "Pendiente",
            value = "$primarySymbol ${formatAmount(primaryAmount)}",
            secondaryValue = secondaryValue,
            isCritical = (customer.pendingInvoices ?: 0) > 0
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onOpenPending, modifier = Modifier.weight(1f)) {
                Text("Pendientes")
            }
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                Text("Historial")
            }
            OutlinedButton(onClick = onOpenRegisterPayment, modifier = Modifier.weight(1f)) {
                Text("Registrar pago")
            }
        }

        if (invoicesState is CustomerInvoicesState.Success && invoicesState.invoices.isNotEmpty()) {
            CustomerOutstandingSummary(
                customer = customer,
                invoices = invoicesState.invoices,
                posBaseCurrency = posCurrency,
                supportedCurrencies = supportedCurrencies,
                cashboxManager = cashboxManager
            )
        }
    }
}

@Composable
private fun CustomerRightPanel(
    customer: CustomerBO?,
    rightPanelTab: CustomerPanelTab,
    onTabChange: (CustomerPanelTab) -> Unit,
    paymentState: CustomerPaymentState,
    invoicesState: CustomerInvoicesState,
    historyState: CustomerInvoiceHistoryState,
    historyMessage: String?,
    historyBusy: Boolean,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
    posBaseCurrency: String,
    onRegisterPayment: (
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        referenceNumber: String
    ) -> Unit,
    onInvoiceHistoryAction: (
        invoiceId: String,
        action: InvoiceCancellationAction,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean
    ) -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments?,
    onSubmitPartialReturn: (
        invoiceId: String,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean,
        itemsToReturnByCode: Map<String, Double>
    ) -> Unit,
    onOpenPending: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRegisterPayment: () -> Unit
) {
    val tabs = remember {
        listOf(CustomerPanelTab.Details, CustomerPanelTab.Pending, CustomerPanelTab.History)
    }
    val selectedIndex = tabs.indexOf(rightPanelTab).coerceAtLeast(0)
    Column(modifier = Modifier.fillMaxSize()) {
        CustomerPanelHeader(customer = customer)

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEach { tab ->
                val enabled = customer != null
                Tab(
                    selected = tab == rightPanelTab,
                    onClick = {
                        if (enabled) onTabChange(tab)
                    },
                    enabled = enabled,
                    text = { Text(tab.label) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (rightPanelTab) {
                CustomerPanelTab.Details -> {
                    if (customer != null) {
                        CustomerDetailPanel(
                            customer = customer,
                            paymentState = paymentState,
                            invoicesState = invoicesState,
                            supportedCurrencies = supportedCurrencies,
                            cashboxManager = cashboxManager,
                            onOpenPending = onOpenPending,
                            onOpenHistory = onOpenHistory,
                            onOpenRegisterPayment = onOpenRegisterPayment
                        )
                    } else {
                        EmptyStateMessage(
                            message = "Selecciona un cliente",
                            icon = Icons.Filled.People,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                CustomerPanelTab.Pending -> {
                    if (customer != null) {
                        CustomerOutstandingInvoicesContent(
                            customer = customer,
                            invoicesState = invoicesState,
                            paymentState = paymentState,
                            onRegisterPayment = onRegisterPayment,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        )
                    } else {
                        EmptyStateMessage(
                            message = "Selecciona un cliente",
                            icon = Icons.Filled.ReceiptLong,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                CustomerPanelTab.History -> {
                    if (customer != null) {
                        CustomerInvoiceHistoryContent(
                            customer = customer,
                            historyState = historyState,
                            historyMessage = historyMessage,
                            historyBusy = historyBusy,
                            paymentState = paymentState,
                            posBaseCurrency = posBaseCurrency,
                            supportedCurrencies = supportedCurrencies,
                            cashboxManager = cashboxManager,
                            onAction = onInvoiceHistoryAction,
                            loadLocalInvoice = loadLocalInvoice,
                            onSubmitPartialReturn = onSubmitPartialReturn,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        )
                    } else {
                        EmptyStateMessage(
                            message = "Selecciona un cliente",
                            icon = Icons.Filled.History,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerPanelHeader(
    customer: CustomerBO?
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = customer?.customerName?.take(1)?.uppercase() ?: "?",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = customer?.customerName ?: "Clientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = customer?.mobileNo ?: "Selecciona un cliente para ver detalle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun NewCustomerDialog(
    onDismiss: () -> Unit,
    onSubmit: (CreateCustomerInput) -> Unit,
    customerGroups: List<com.erpnext.pos.domain.models.CustomerGroupBO>,
    territories: List<com.erpnext.pos.domain.models.TerritoryBO>,
    paymentTermsOptions: List<com.erpnext.pos.domain.models.PaymentTermBO>,
    companies: List<com.erpnext.pos.domain.models.CompanyBO>
) {
    var name by rememberSaveable { mutableStateOf("") }
    var customerType by rememberSaveable { mutableStateOf("Individual") }
    var customerGroup by rememberSaveable { mutableStateOf("") }
    var territory by rememberSaveable { mutableStateOf("") }
    var taxId by rememberSaveable { mutableStateOf("") }
    var taxCategory by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var addressLine by rememberSaveable { mutableStateOf("") }
    var addressLine2 by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var creditLimit by rememberSaveable { mutableStateOf("") }
    var selectedPaymentTerm by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var isInternalCustomer by rememberSaveable { mutableStateOf(false) }
    var internalCompany by rememberSaveable { mutableStateOf("") }

    var typeExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    var territoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var companyExpanded by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(CustomerDialogTab.Personal) }
    val isValid = name.isNotBlank() &&
        (!isInternalCustomer || internalCompany.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Nuevo cliente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    CustomerDialogTab.values().forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab.ordinal == index,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .padding(end = 4.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        CustomerDialogTab.Personal -> {
                            AppTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "Nombre del cliente",
                                placeholder = "Cliente S.A.",
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ExposedDropdownMenuBox(
                                expanded = typeExpanded,
                                onExpandedChange = { typeExpanded = it }
                            ) {
                                AppTextField(
                                    value = customerType,
                                    onValueChange = {},
                                    label = "Tipo de cliente",
                                    placeholder = "Seleccionar",
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = typeExpanded,
                                    onDismissRequest = { typeExpanded = false }
                                ) {
                                    listOf("Individual", "Empresa").forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                customerType = option
                                                typeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (customerGroups.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = groupExpanded,
                                    onExpandedChange = { groupExpanded = it }
                                ) {
                                    AppTextField(
                                        value = customerGroup,
                                        onValueChange = {},
                                        label = "Grupo de cliente",
                                        placeholder = "Seleccionar",
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = groupExpanded,
                                        onDismissRequest = { groupExpanded = false }
                                    ) {
                                        customerGroups.forEach { option ->
                                            val label = option.displayName?.takeIf { it.isNotBlank() }
                                                ?: option.name
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    customerGroup = option.name
                                                    groupExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                AppTextField(
                                    value = customerGroup,
                                    onValueChange = { customerGroup = it },
                                    label = "Grupo de cliente",
                                    placeholder = "Retail",
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (territories.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = territoryExpanded,
                                    onExpandedChange = { territoryExpanded = it }
                                ) {
                                    AppTextField(
                                        value = territory,
                                        onValueChange = {},
                                        label = "Territorio",
                                        placeholder = "Seleccionar",
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = territoryExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = territoryExpanded,
                                        onDismissRequest = { territoryExpanded = false }
                                    ) {
                                        territories.forEach { option ->
                                            val label = option.displayName?.takeIf { it.isNotBlank() }
                                                ?: option.name
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    territory = option.name
                                                    territoryExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                AppTextField(
                                    value = territory,
                                    onValueChange = { territory = it },
                                    label = "Territorio",
                                    placeholder = "Managua",
                                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isInternalCustomer,
                                    onCheckedChange = { isInternalCustomer = it }
                                )
                                Text("Cliente interno (intercompany)")
                            }
                            if (isInternalCustomer) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Selecciona la compañía a la que pertenece este cliente interno.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isInternalCustomer) {
                                if (companies.isNotEmpty()) {
                                    ExposedDropdownMenuBox(
                                        expanded = companyExpanded,
                                        onExpandedChange = { companyExpanded = it }
                                    ) {
                                        AppTextField(
                                            value = internalCompany,
                                            onValueChange = {},
                                            label = "Compañía",
                                            placeholder = "Seleccionar",
                                            modifier = Modifier
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth(),
                                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded)
                                            }
                                        )
                                        ExposedDropdownMenu(
                                            expanded = companyExpanded,
                                            onDismissRequest = { companyExpanded = false }
                                        ) {
                                            companies.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.company) },
                                                    onClick = {
                                                        internalCompany = option.company
                                                        companyExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    AppTextField(
                                        value = internalCompany,
                                        onValueChange = { internalCompany = it },
                                        label = "Compañía",
                                        placeholder = "Escribe la compañía",
                                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            AppTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = "Notas",
                                placeholder = "Observaciones internas",
                                leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        CustomerDialogTab.Contact -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AppTextField(
                                    value = mobile,
                                    onValueChange = { mobile = it },
                                    label = "Móvil",
                                    placeholder = "+505 8888 8888",
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                                AppTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = "Teléfono",
                                    placeholder = "2222 2222",
                                    leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            AppTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Correo",
                                placeholder = "cliente@correo.com",
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = addressLine,
                                onValueChange = { addressLine = it },
                                label = "Dirección línea 1",
                                placeholder = "Calle principal",
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = addressLine2,
                                onValueChange = { addressLine2 = it },
                                label = "Dirección línea 2",
                                placeholder = "Referencias, barrio",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AppTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = "Ciudad",
                                    placeholder = "Managua",
                                    modifier = Modifier.weight(1f)
                                )
                                AppTextField(
                                    value = state,
                                    onValueChange = { state = it },
                                    label = "Departamento",
                                    placeholder = "Managua",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            AppTextField(
                                value = country,
                                onValueChange = { country = it },
                                label = "País",
                                placeholder = "Nicaragua",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        CustomerDialogTab.Tax -> {
                            AppTextField(
                                value = taxId,
                                onValueChange = { taxId = it },
                                label = "RUC / NIT",
                                placeholder = "J0310000000001",
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = taxCategory,
                                onValueChange = { taxCategory = it },
                                label = "Categoría de impuesto",
                                placeholder = "IVA General",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        CustomerDialogTab.Accounting -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AppTextField(
                                    value = creditLimit,
                                    onValueChange = { creditLimit = it },
                                    label = "Límite de crédito",
                                    placeholder = "0.00",
                                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (paymentTermsOptions.isNotEmpty()) {
                                    ExposedDropdownMenuBox(
                                        expanded = paymentExpanded,
                                        onExpandedChange = { paymentExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AppTextField(
                                            value = selectedPaymentTerm,
                                            onValueChange = {},
                                            label = "Términos de pago",
                                            placeholder = "Seleccionar",
                                            modifier = Modifier
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth(),
                                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded)
                                            }
                                        )
                                        ExposedDropdownMenu(
                                            expanded = paymentExpanded,
                                            onDismissRequest = { paymentExpanded = false }
                                        ) {
                                            paymentTermsOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.name) },
                                                    onClick = {
                                                        selectedPaymentTerm = option.name
                                                        paymentExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    AppTextField(
                                        value = selectedPaymentTerm,
                                        onValueChange = { selectedPaymentTerm = it },
                                        label = "Términos de pago",
                                        placeholder = "Contado / 30 días",
                                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            onSubmit(
                                CreateCustomerInput(
                                    customerName = name.trim(),
                                    customerType = customerType,
                                    customerGroup = customerGroup.trim().ifBlank { null },
                                    territory = territory.trim().ifBlank { null },
                                    isInternalCustomer = isInternalCustomer,
                                    internalCompany = internalCompany.trim().ifBlank { null },
                                    taxId = taxId.trim().ifBlank { null },
                                    taxCategory = taxCategory.trim().ifBlank { null },
                                    email = email.trim().ifBlank { null },
                                    mobileNo = mobile.trim().ifBlank { null },
                                    phone = phone.trim().ifBlank { null },
                                    addressLine1 = addressLine.trim().ifBlank { null },
                                    addressLine2 = addressLine2.trim().ifBlank { null },
                                    city = city.trim().ifBlank { null },
                                    state = state.trim().ifBlank { null },
                                    country = country.trim().ifBlank { null },
                                    creditLimit = creditLimit.toDoubleOrNull(),
                                    paymentTerms = selectedPaymentTerm.trim().ifBlank { null },
                                    notes = notes.trim().ifBlank { null }
                                )
                            )
                            onDismiss()
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                }
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
    supportedCurrencies: List<String>,
    isDesktop: Boolean,
    onSelect: (CustomerBO) -> Unit,
    //onClick: () -> Unit,
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
    val displayCurrencies = remember(baseCurrency, posCurr, supportedCurrencies) {
        (supportedCurrencies + listOfNotNull(baseCurrency, posCurr))
            .mapNotNull { normalizeCurrency(it) }
            .distinct()
    }
    var pendingByCurrency by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    LaunchedEffect(baseCurrency, displayCurrencies, pendingAmount) {
        val resolved = mutableMapOf<String, Double>()
        displayCurrencies.forEach { currency ->
            val rate = if (baseCurrency.equals(currency, ignoreCase = true)) {
                1.0
            } else {
                cashboxManager.resolveExchangeRateBetween(
                    fromCurrency = baseCurrency,
                    toCurrency = currency,
                    allowNetwork = false
                ) ?: run {
                    when {
                        baseCurrency.equals("USD", true) -> posExchangeRate.takeIf { it > 0.0 }
                        currency.equals("USD", true) && posExchangeRate > 0.0 -> 1 / posExchangeRate
                        else -> null
                    }
                }
            }
            if (rate != null && rate > 0.0) {
                resolved[currency] = pendingAmount * rate
            }
        }
        pendingByCurrency = resolved
    }
    val primaryCurrency = when {
        !posCurr.isNullOrBlank() && pendingByCurrency.containsKey(posCurr) -> posCurr
        !baseCurrency.isNullOrBlank() && pendingByCurrency.containsKey(baseCurrency) -> baseCurrency
        else -> displayCurrencies.firstOrNull() ?: "USD"
    }
    val primaryAmount = pendingByCurrency[primaryCurrency] ?: pendingAmount
    val secondaryLabels = pendingByCurrency
        .filterKeys { !it.equals(primaryCurrency, ignoreCase = true) }
        .map { (currency, amount) ->
            val symbol = currency.toCurrencySymbol().ifBlank { currency }
            "$symbol ${formatAmount(amount)}"
        }
    val secondaryValue = secondaryLabels.takeIf { it.isNotEmpty() }?.joinToString(" · ")
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
    val cardShape = RoundedCornerShape(22.dp)
    val cardBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(cardShape)
            .clickable { onSelect(customer) }
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
            },
        //.clickable { onClick() },
        // elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = cardShape,
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush, cardShape)
                .border(1.dp, Color.Transparent, shape = cardShape)
                .padding(if (isDesktop) 12.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val context = LocalPlatformContext.current
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(48.dp),
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
                        fontSize = 15.sp,
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

            val pendingPrimarySymbol =
                primaryCurrency.toCurrencySymbol().ifBlank { primaryCurrency }
            Text(
                text = "$pendingPrimarySymbol ${formatAmount(primaryAmount)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (emphasis) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Facturas pendientes: $pendingInvoices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (customer.mobileNo?.isNotEmpty() == true) {
                Text(
                    text = customer.mobileNo ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
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
                supportedCurrencies = supportedCurrencies,
                cashboxManager = cashboxManager
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CustomerOutstandingInvoicesContent(
            customer = customer,
            invoicesState = invoicesState,
            paymentState = paymentState,
            onRegisterPayment = onRegisterPayment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CustomerOutstandingInvoicesContent(
    customer: CustomerBO,
    invoicesState: CustomerInvoicesState,
    paymentState: CustomerPaymentState,
    onRegisterPayment: (
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        referenceNumber: String
    ) -> Unit,
    modifier: Modifier = Modifier
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

    Column(
        modifier = modifier,
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
                            val baseLabel =
                                "$baseSymbol ${formatAmount(invoice.outstandingAmount)}"

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
                                                    val amountToUse =
                                                        rateBaseToSelected?.let { rate ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerInvoiceHistorySheet(
    customer: CustomerBO,
    historyState: CustomerInvoiceHistoryState,
    historyMessage: String?,
    historyBusy: Boolean,
    paymentState: CustomerPaymentState,
    posBaseCurrency: String,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
    onAction: (String, InvoiceCancellationAction, String?, String?, Boolean) -> Unit,
    onDismiss: () -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments? = { null },
    onSubmitPartialReturn: (
        invoiceId: String,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean,
        itemsToReturnByCode: Map<String, Double>
    ) -> Unit = { _, _, _, _, _, _ -> }
) {
    val scope = rememberCoroutineScope()

    var returnInvoiceId by remember { mutableStateOf<String?>(null) }
    var returnInvoiceLocal by remember { mutableStateOf<SalesInvoiceWithItemsAndPayments?>(null) }
    var returnLoading by remember { mutableStateOf(false) }
    var returnError by remember { mutableStateOf<String?>(null) }
    var showReturnDialog by remember { mutableStateOf(false) }

    var refundMode by remember { mutableStateOf<String?>(null) }
    var refundReference by remember { mutableStateOf("") }
    var qtyByItemCode by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var returnDestination by remember { mutableStateOf(ReturnDestination.RETURN) }

    var showFullReturnDialog by remember { mutableStateOf(false) }
    var fullReturnInvoiceId by remember { mutableStateOf<String?>(null) }
    var fullRefundMode by remember { mutableStateOf<String?>(null) }
    var fullRefundReference by remember { mutableStateOf("") }
    var fullReturnDestination by remember { mutableStateOf(ReturnDestination.RETURN) }

    val refundOptions = remember(paymentState.paymentModes) {
        paymentState.paymentModes.mapNotNull { it.modeOfPayment.ifBlank { null } }.distinct()
    }

    fun canConfirmReturn(): Boolean = qtyByItemCode.values.any { it > 0.0 }

    fun openPartialReturn(invoiceId: String) {
        showReturnDialog = true
        returnInvoiceId = invoiceId
        returnInvoiceLocal = null
        returnError = null
        refundMode = null
        refundReference = ""
        qtyByItemCode = emptyMap()
        returnDestination = ReturnDestination.RETURN

        scope.launch {
            returnLoading = true
            try {
                val local = loadLocalInvoice(invoiceId)
                if (local == null) {
                    returnError = "No se encontró la factura localmente."
                } else {
                    returnInvoiceLocal = local
                    qtyByItemCode = local.items.associate { it.itemCode to 0.0 }
                    refundMode = refundOptions.firstOrNull()
                }
            } catch (e: Exception) {
                returnError = e.message ?: "No se pudo cargar la factura."
            } finally {
                returnLoading = false
            }
        }
    }

    fun openFullReturn(invoiceId: String) {
        showFullReturnDialog = true
        fullReturnInvoiceId = invoiceId
        fullRefundMode = refundOptions.firstOrNull()
        fullRefundReference = ""
        fullReturnDestination = ReturnDestination.RETURN
    }

    fun closeFullReturnDialog() {
        showFullReturnDialog = false
        fullReturnInvoiceId = null
        fullRefundMode = null
        fullRefundReference = ""
        fullReturnDestination = ReturnDestination.RETURN
    }

    fun closeReturnDialog() {
        showReturnDialog = false
        returnInvoiceId = null
        returnInvoiceLocal = null
        returnError = null
        refundMode = null
        refundReference = ""
        qtyByItemCode = emptyMap()
    }

    if (showReturnDialog && returnInvoiceId != null) {
        val refundEnabled = returnDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(refundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && refundMode.isNullOrBlank()
        val missingReference = needsReference && refundReference.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)

        AlertDialog(
            onDismissRequest = {
                if (!historyBusy) {
                    closeReturnDialog()
                }
            },
            title = { Text("Retorno parcial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Factura: ${returnInvoiceId!!}")

                    if (returnLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "Cargando detalle local...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (!returnError.isNullOrBlank()) {
                        Text(
                            returnError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    var refundModeExpanded by remember { mutableStateOf(false) }

                    Text(
                        text = "Destino del monto devuelto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReturnDestination.entries.forEach { destination ->
                            FilterChip(
                                selected = returnDestination == destination,
                                onClick = { returnDestination = destination },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    if (refundEnabled && refundOptions.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = refundModeExpanded,
                            onExpandedChange = { refundModeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = refundMode ?: "",
                                onValueChange = { },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                label = { Text("Modo de reembolso (opcional)") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = refundModeExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Sell, contentDescription = null)
                                },
                                readOnly = true,
                                singleLine = true,
                                enabled = !historyBusy,
                                supportingText = {
                                    Text("Vacío = solo nota de crédito.")
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = refundModeExpanded,
                                onDismissRequest = { refundModeExpanded = false }
                            ) {
                                refundOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            refundMode = option
                                            refundModeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (refundEnabled && refundOptions.isEmpty()) {
                        Text(
                            "No hay modos de pago disponibles para reembolsos.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (refundEnabled && needsReference) {
                        OutlinedTextField(
                            value = refundReference,
                            onValueChange = { refundReference = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Referencia (requerida)") },
                            singleLine = true,
                            enabled = !historyBusy
                        )
                    }

                    if (refundEnabled) {
                        Text(
                            "El retorno genera una nota de crédito aplicable contra la factura original.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        "Selecciona cantidades a devolver:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    returnInvoiceLocal?.let { local ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(local.items, key = { it.itemCode }) { item ->
                                val soldQty = item.qty
                                val current = qtyByItemCode[item.itemCode] ?: 0.0
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            .copy(alpha = 0.45f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = item.itemName ?: item.itemCode,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Vendidos: ${
                                                    formatDoubleToString(
                                                        soldQty,
                                                        2
                                                    )
                                                }",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val next =
                                                        (current - 1.0).coerceAtLeast(0.0)
                                                    qtyByItemCode =
                                                        qtyByItemCode.toMutableMap().apply {
                                                            put(item.itemCode, next)
                                                        }
                                                },
                                                enabled = !historyBusy && current > 0.0
                                            ) {
                                                Icon(Icons.Default.Remove, null)
                                            }
                                            Text(
                                                text = formatDoubleToString(current, 2),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            IconButton(
                                                onClick = {
                                                    val next =
                                                        (current + 1.0).coerceAtMost(soldQty)
                                                    qtyByItemCode =
                                                        qtyByItemCode.toMutableMap().apply {
                                                            put(item.itemCode, next)
                                                        }
                                                },
                                                enabled = !historyBusy && current < soldQty
                                            ) {
                                                Icon(Icons.Default.Add, null)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!canConfirmReturn()) {
                            Text(
                                "Debes seleccionar al menos 1 artículo.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (refundEnabled && missingRefundMode) {
                            Text(
                                "Selecciona un modo de reembolso.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (refundEnabled && missingReference) {
                            Text(
                                "La referencia es requerida para este modo de reembolso.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                }
            },
            confirmButton = {
                Button(
                    enabled = !historyBusy && !returnLoading && returnInvoiceLocal != null &&
                            canConfirmReturn() && canConfirmRefund,
                    onClick = {
                        val invoiceId = returnInvoiceId ?: return@Button
                        onSubmitPartialReturn(
                            invoiceId,
                            null,
                            refundMode?.takeIf { it.isNotBlank() },
                            refundReference.takeIf { it.isNotBlank() },
                            returnDestination == ReturnDestination.RETURN,
                            qtyByItemCode.filterValues { it > 0.0 }
                        )
                        closeReturnDialog()
                    }
                ) { Text("Confirmar retorno") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !historyBusy,
                    onClick = { closeReturnDialog() }
                ) { Text("Cerrar") }
            }
        )
    }

    if (showFullReturnDialog && fullReturnInvoiceId != null) {
        val refundEnabled = fullReturnDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(fullRefundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && fullRefundMode.isNullOrBlank()
        val missingReference = needsReference && fullRefundReference.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)

        AlertDialog(
            onDismissRequest = {
                if (!historyBusy) {
                    closeFullReturnDialog()
                }
            },
            title = { Text("Retorno total") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Factura: ${fullReturnInvoiceId!!}")
                    Text(
                        text = "Destino del monto devuelto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReturnDestination.entries.forEach { destination ->
                            FilterChip(
                                selected = fullReturnDestination == destination,
                                onClick = { fullReturnDestination = destination },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    var refundModeExpanded by remember { mutableStateOf(false) }
                    if (refundEnabled && refundOptions.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = refundModeExpanded,
                            onExpandedChange = { refundModeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = fullRefundMode ?: "",
                                onValueChange = { },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                label = { Text("Modo de reembolso (opcional)") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = refundModeExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Sell, contentDescription = null)
                                },
                                readOnly = true,
                                singleLine = true,
                                enabled = !historyBusy,
                                supportingText = {
                                    Text("Vacío = solo nota de crédito.")
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = refundModeExpanded,
                                onDismissRequest = { refundModeExpanded = false }
                            ) {
                                refundOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            fullRefundMode = option
                                            refundModeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (refundEnabled && refundOptions.isEmpty()) {
                        Text(
                            "No hay modos de pago disponibles para reembolsos.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (refundEnabled && needsReference) {
                        OutlinedTextField(
                            value = fullRefundReference,
                            onValueChange = { fullRefundReference = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Referencia (requerida)") },
                            singleLine = true,
                            enabled = !historyBusy
                        )
                    }

                    if (refundEnabled) {
                        Text(
                            "El retorno genera una nota de crédito aplicable contra la factura original.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (refundEnabled && missingRefundMode) {
                        Text(
                            "Selecciona un modo de reembolso.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (refundEnabled && missingReference) {
                        Text(
                            "La referencia es requerida para este modo de reembolso.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !historyBusy && canConfirmRefund,
                    onClick = {
                        val invoiceId = fullReturnInvoiceId ?: return@Button
                        onAction(
                            invoiceId,
                            InvoiceCancellationAction.RETURN,
                            fullRefundMode?.takeIf { it.isNotBlank() },
                            fullRefundReference.takeIf { it.isNotBlank() },
                            fullReturnDestination == ReturnDestination.RETURN
                        )
                        closeFullReturnDialog()
                    }
                ) { Text("Confirmar retorno") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !historyBusy,
                    onClick = { closeFullReturnDialog() }
                ) { Text("Cerrar") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CustomerInvoiceHistoryContent(
            customer = customer,
            historyState = historyState,
            historyMessage = historyMessage,
            historyBusy = historyBusy,
            paymentState = paymentState,
            posBaseCurrency = posBaseCurrency,
            supportedCurrencies = supportedCurrencies,
            cashboxManager = cashboxManager,
            onAction = onAction,
            loadLocalInvoice = loadLocalInvoice,
            onSubmitPartialReturn = onSubmitPartialReturn,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CustomerInvoiceHistoryContent(
    customer: CustomerBO,
    historyState: CustomerInvoiceHistoryState,
    historyMessage: String?,
    historyBusy: Boolean,
    paymentState: CustomerPaymentState,
    posBaseCurrency: String,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
    onAction: (String, InvoiceCancellationAction, String?, String?, Boolean) -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments? = { null },
    onSubmitPartialReturn: (
        invoiceId: String,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean,
        itemsToReturnByCode: Map<String, Double>
    ) -> Unit = { _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier,
    showDialogs: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var selectedRangeDays by remember { mutableStateOf(30) }

    var returnInvoiceId by remember { mutableStateOf<String?>(null) }
    var returnInvoiceLocal by remember { mutableStateOf<SalesInvoiceWithItemsAndPayments?>(null) }
    var returnLoading by remember { mutableStateOf(false) }
    var returnError by remember { mutableStateOf<String?>(null) }
    var showReturnDialog by remember { mutableStateOf(false) }

    var refundMode by remember { mutableStateOf<String?>(null) }
    var refundReference by remember { mutableStateOf("") }
    var qtyByItemCode by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var returnDestination by remember { mutableStateOf(ReturnDestination.RETURN) }

    var showFullReturnDialog by remember { mutableStateOf(false) }
    var fullReturnInvoiceId by remember { mutableStateOf<String?>(null) }
    var fullRefundMode by remember { mutableStateOf<String?>(null) }
    var fullRefundReference by remember { mutableStateOf("") }
    var fullReturnDestination by remember { mutableStateOf(ReturnDestination.RETURN) }

    val refundOptions = remember(paymentState.paymentModes) {
        paymentState.paymentModes.mapNotNull { it.modeOfPayment.ifBlank { null } }.distinct()
    }

    fun canConfirmReturn(): Boolean = qtyByItemCode.values.any { it > 0.0 }

    fun openPartialReturn(invoiceId: String) {
        showReturnDialog = true
        returnInvoiceId = invoiceId
        returnInvoiceLocal = null
        returnError = null
        refundMode = null
        refundReference = ""
        qtyByItemCode = emptyMap()
        returnDestination = ReturnDestination.RETURN

        scope.launch {
            returnLoading = true
            try {
                val local = loadLocalInvoice(invoiceId)
                if (local == null) {
                    returnError = "No se encontró la factura localmente."
                } else {
                    returnInvoiceLocal = local
                    qtyByItemCode = local.items.associate { it.itemCode to 0.0 }
                    refundMode = refundOptions.firstOrNull()
                }
            } catch (e: Exception) {
                returnError = e.message ?: "No se pudo cargar la factura."
            } finally {
                returnLoading = false
            }
        }
    }

    fun openFullReturn(invoiceId: String) {
        showFullReturnDialog = true
        fullReturnInvoiceId = invoiceId
        fullRefundMode = refundOptions.firstOrNull()
        fullRefundReference = ""
        fullReturnDestination = ReturnDestination.RETURN
    }

    fun closeFullReturnDialog() {
        showFullReturnDialog = false
        fullReturnInvoiceId = null
        fullRefundMode = null
        fullRefundReference = ""
        fullReturnDestination = ReturnDestination.RETURN
    }

    fun closeReturnDialog() {
        showReturnDialog = false
        returnInvoiceId = null
        returnInvoiceLocal = null
        returnError = null
        refundMode = null
        refundReference = ""
        qtyByItemCode = emptyMap()
    }

    if (showDialogs && showReturnDialog && returnInvoiceId != null) {
        val refundEnabled = returnDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(refundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && refundMode.isNullOrBlank()
        val missingReference = needsReference && refundReference.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)

        AlertDialog(
            onDismissRequest = {
                if (!historyBusy) {
                    closeReturnDialog()
                }
            },
            title = { Text("Retorno parcial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Factura: ${returnInvoiceId!!}")

                    if (returnLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "Cargando detalle local...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (!returnError.isNullOrBlank()) {
                        Text(
                            returnError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    var refundModeExpanded by remember { mutableStateOf(false) }

                    Text(
                        text = "Destino del monto devuelto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReturnDestination.entries.forEach { destination ->
                            FilterChip(
                                selected = returnDestination == destination,
                                onClick = { returnDestination = destination },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    if (refundEnabled && refundOptions.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = refundModeExpanded,
                            onExpandedChange = { refundModeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = refundMode ?: "",
                                onValueChange = { },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                label = { Text("Modo de reembolso (opcional)") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = refundModeExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Sell, contentDescription = null)
                                },
                                readOnly = true,
                                singleLine = true,
                                enabled = !historyBusy,
                                supportingText = {
                                    Text("Vacío = solo nota de crédito.")
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = refundModeExpanded,
                                onDismissRequest = { refundModeExpanded = false }
                            ) {
                                refundOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            refundMode = option
                                            refundModeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (refundEnabled && refundOptions.isEmpty()) {
                        Text(
                            "No hay modos de pago disponibles para reembolsos.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (refundEnabled && needsReference) {
                        OutlinedTextField(
                            value = refundReference,
                            onValueChange = { refundReference = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Referencia (requerida)") },
                            singleLine = true,
                            enabled = !historyBusy
                        )
                    }

                    if (refundEnabled) {
                        Text(
                            "El retorno genera una nota de crédito aplicable contra la factura original.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        "Selecciona cantidades a devolver:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    returnInvoiceLocal?.let { local ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(local.items, key = { it.itemCode }) { item ->
                                val soldQty = item.qty
                                val current = qtyByItemCode[item.itemCode] ?: 0.0
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            .copy(alpha = 0.45f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = item.itemName ?: item.itemCode,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Vendidos: ${
                                                    formatDoubleToString(
                                                        soldQty,
                                                        2
                                                    )
                                                }",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val next =
                                                        (current - 1.0).coerceAtLeast(0.0)
                                                    qtyByItemCode =
                                                        qtyByItemCode.toMutableMap().apply {
                                                            put(item.itemCode, next)
                                                        }
                                                },
                                                enabled = !historyBusy && current > 0.0
                                            ) {
                                                Icon(Icons.Default.Remove, null)
                                            }
                                            Text(
                                                text = formatDoubleToString(current, 2),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            IconButton(
                                                onClick = {
                                                    val next =
                                                        (current + 1.0).coerceAtMost(soldQty)
                                                    qtyByItemCode =
                                                        qtyByItemCode.toMutableMap().apply {
                                                            put(item.itemCode, next)
                                                        }
                                                },
                                                enabled = !historyBusy && current < soldQty
                                            ) {
                                                Icon(Icons.Default.Add, null)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!canConfirmReturn()) {
                            Text(
                                "Selecciona al menos un item.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!historyBusy && returnInvoiceId != null) {
                            onSubmitPartialReturn(
                                returnInvoiceId!!,
                                null,
                                if (refundEnabled) refundMode else null,
                                if (refundEnabled) refundReference else null,
                                refundEnabled,
                                qtyByItemCode
                            )
                            closeReturnDialog()
                        }
                    },
                    enabled = !historyBusy && canConfirmReturn() && canConfirmRefund
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!historyBusy) {
                            closeReturnDialog()
                        }
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDialogs && showFullReturnDialog && fullReturnInvoiceId != null) {
        val refundEnabled = fullReturnDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(fullRefundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && fullRefundMode.isNullOrBlank()
        val missingReference = needsReference && fullRefundReference.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)

        AlertDialog(
            onDismissRequest = {
                if (!historyBusy) {
                    closeFullReturnDialog()
                }
            },
            title = { Text("Retorno total") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Factura: ${fullReturnInvoiceId!!}")

                    Text(
                        text = "Destino del monto devuelto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReturnDestination.entries.forEach { destination ->
                            FilterChip(
                                selected = fullReturnDestination == destination,
                                onClick = { fullReturnDestination = destination },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    if (refundEnabled && refundOptions.isNotEmpty()) {
                        var fullRefundModeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = fullRefundModeExpanded,
                            onExpandedChange = { fullRefundModeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = fullRefundMode ?: "",
                                onValueChange = { },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                label = { Text("Modo de reembolso (opcional)") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = fullRefundModeExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Sell, contentDescription = null)
                                },
                                readOnly = true,
                                singleLine = true,
                                enabled = !historyBusy,
                                supportingText = {
                                    Text("Vacío = solo nota de crédito.")
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = fullRefundModeExpanded,
                                onDismissRequest = { fullRefundModeExpanded = false }
                            ) {
                                refundOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            fullRefundMode = option
                                            fullRefundModeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (refundEnabled && refundOptions.isEmpty()) {
                        Text(
                            "No hay modos de pago disponibles para reembolsos.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (refundEnabled && needsReference) {
                        OutlinedTextField(
                            value = fullRefundReference,
                            onValueChange = { fullRefundReference = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Referencia (requerida)") },
                            singleLine = true,
                            enabled = !historyBusy
                        )
                    }

                    if (refundEnabled) {
                        Text(
                            "El retorno genera una nota de crédito aplicable contra la factura original.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !historyBusy && canConfirmRefund,
                    onClick = {
                        val invoiceId = fullReturnInvoiceId ?: return@Button
                        onAction(
                            invoiceId,
                            InvoiceCancellationAction.RETURN,
                            fullRefundMode?.takeIf { it.isNotBlank() },
                            fullRefundReference.takeIf { it.isNotBlank() },
                            fullReturnDestination == ReturnDestination.RETURN
                        )
                        closeFullReturnDialog()
                    }
                ) { Text("Confirmar retorno") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !historyBusy,
                    onClick = { closeFullReturnDialog() }
                ) { Text("Cerrar") }
            }
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ) {
                Text(
                    text = customer.customerName.take(1).uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.customerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                val subtitle = when (historyState) {
                    is CustomerInvoiceHistoryState.Success -> {
                        val count = historyState.invoices.count {
                            isWithinDays(it.postingDate, selectedRangeDays)
                        }
                        "$count facturas en ${selectedRangeDays} días"
                    }

                    CustomerInvoiceHistoryState.Loading -> "Cargando historial..."
                    is CustomerInvoiceHistoryState.Error -> "Historial no disponible"
                    else -> "Historial de facturas"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryRangeChip(
                label = "7 días",
                selected = selectedRangeDays == 7,
                onClick = { selectedRangeDays = 7 }
            )
            HistoryRangeChip(
                label = "30 días",
                selected = selectedRangeDays == 30,
                onClick = { selectedRangeDays = 30 }
            )
            HistoryRangeChip(
                label = "90 días",
                selected = selectedRangeDays == 90,
                onClick = { selectedRangeDays = 90 }
            )
        }
        if (historyState is CustomerInvoiceHistoryState.Success) {
            val invoices = historyState.invoices.filter {
                isWithinDays(it.postingDate, selectedRangeDays)
            }
            val pendingCount = invoices.count {
                val status = it.status?.trim()?.lowercase()
                status == "unpaid" || status == "overdue" || status == "partly paid"
            }
            val paidCount = invoices.count {
                it.status?.trim()?.lowercase() == "paid"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoryStatChip(
                    label = "Total",
                    value = invoices.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                HistoryStatChip(
                    label = "Pendientes",
                    value = pendingCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                HistoryStatChip(
                    label = "Pagadas",
                    value = paidCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (!historyMessage.isNullOrBlank()) {
            Text(
                text = historyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        when (historyState) {
            CustomerInvoiceHistoryState.Idle -> {
                Text("Abre la vista para cargar las facturas de los últimos 90 días.")
            }

            CustomerInvoiceHistoryState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Cargando historial...")
                }
            }

            is CustomerInvoiceHistoryState.Error -> {
                Text(
                    historyState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is CustomerInvoiceHistoryState.Success -> {
                val invoices = historyState.invoices.filter {
                    isWithinDays(it.postingDate, selectedRangeDays)
                }
                if (invoices.isEmpty()) {
                    Text("No se encontraron facturas en los últimos $selectedRangeDays días.")
                } else {
                    InvoiceHistorySummary(
                        invoices = invoices,
                        posBaseCurrency = posBaseCurrency,
                        supportedCurrencies = supportedCurrencies,
                        cashboxManager = cashboxManager
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items = invoices, key = { it.invoiceId }) { invoice ->
                            InvoiceHistoryRow(
                                invoice = invoice,
                                isBusy = historyBusy,
                                posBaseCurrency = posBaseCurrency,
                                supportedCurrencies = supportedCurrencies,
                                cashboxManager = cashboxManager,
                                onCancel = { invoiceId ->
                                    onAction(
                                        invoiceId,
                                        InvoiceCancellationAction.CANCEL,
                                        null,
                                        null,
                                        false
                                    )
                                },
                                onReturnTotal = { invoiceId ->
                                    openFullReturn(invoiceId)
                                },
                                onPartialReturn = { invoiceId ->
                                    openPartialReturn(invoiceId)
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HistoryStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HistoryRangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun InvoiceHistorySummary(
    invoices: List<SalesInvoiceBO>,
    posBaseCurrency: String,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager
) {
    val base = normalizeCurrency(posBaseCurrency) ?: "USD"
    val displayCurrencies = remember(supportedCurrencies, base) {
        (supportedCurrencies + base).mapNotNull { normalizeCurrency(it) }.distinct()
    }
    var totalsByCurrency by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    LaunchedEffect(invoices, displayCurrencies, base) {
        val totals = mutableMapOf<String, Double>()
        val rateCache = mutableMapOf<Pair<String, String>, Double?>()
        suspend fun rate(
            from: String,
            to: String,
            invoiceRate: Double?,
            invoiceCurrency: String,
            receivableCurrency: String
        ): Double? {
            if (from.equals(to, true)) return 1.0
            if (invoiceRate != null && invoiceRate > 0.0) {
                if (from.equals(receivableCurrency, true) && to.equals(invoiceCurrency, true)) {
                    return 1 / invoiceRate
                }
                if (from.equals(invoiceCurrency, true) && to.equals(receivableCurrency, true)) {
                    return invoiceRate
                }
            }
            val key = from.uppercase() to to.uppercase()
            if (rateCache.containsKey(key)) return rateCache[key]
            val resolved = cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
            rateCache[key] = resolved
            return resolved
        }
        invoices.forEach { invoice ->
            val receivableCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                ?: normalizeCurrency(invoice.currency)
                ?: base
            val invoiceCurrency = normalizeCurrency(invoice.currency) ?: receivableCurrency
            val invoiceRate = invoice.conversionRate ?: invoice.customExchangeRate
            displayCurrencies.forEach { target ->
                val r = rate(
                    from = receivableCurrency,
                    to = target,
                    invoiceRate = invoiceRate,
                    invoiceCurrency = invoiceCurrency,
                    receivableCurrency = receivableCurrency
                )
                if (r != null && r > 0.0) {
                    totals[target] = (totals[target] ?: 0.0) + invoice.outstandingAmount * r
                }
            }
        }
        totalsByCurrency = totals
    }

    if (totalsByCurrency.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            totalsByCurrency.forEach { (currency, amount) ->
                val symbol = currency.toCurrencySymbol().ifBlank { currency }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(currency, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$symbol ${formatAmount(amount)}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceHistoryRow(
    invoice: SalesInvoiceBO,
    isBusy: Boolean,
    posBaseCurrency: String,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
    onCancel: (String) -> Unit,
    onReturnTotal: (String) -> Unit,
    onPartialReturn: (String) -> Unit = {}
) {
    val invoiceCurrency = normalizeCurrency(invoice.currency)
        ?: normalizeCurrency(posBaseCurrency)
        ?: "USD"
    val receivableCurrency = normalizeCurrency(invoice.partyAccountCurrency) ?: invoiceCurrency
    val displayCurrencies = remember(supportedCurrencies, invoiceCurrency, receivableCurrency) {
        (supportedCurrencies + listOf(invoiceCurrency, receivableCurrency))
            .mapNotNull { normalizeCurrency(it) }
            .distinct()
    }
    var totalByCurrency by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var outstandingByCurrency by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    LaunchedEffect(displayCurrencies, invoiceCurrency, receivableCurrency, invoice.total, invoice.outstandingAmount) {
        val totals = mutableMapOf<String, Double>()
        val outs = mutableMapOf<String, Double>()
        val rateCache = mutableMapOf<Pair<String, String>, Double?>()
        suspend fun rate(from: String, to: String): Double? {
            if (from.equals(to, true)) return 1.0
            val conversionRate = invoice.conversionRate ?: invoice.customExchangeRate
            if (conversionRate != null && conversionRate > 0.0) {
                if (from.equals(receivableCurrency, true) && to.equals(invoiceCurrency, true)) {
                    return 1 / conversionRate
                }
                if (from.equals(invoiceCurrency, true) && to.equals(receivableCurrency, true)) {
                    return conversionRate
                }
            }
            val key = from.uppercase() to to.uppercase()
            if (rateCache.containsKey(key)) return rateCache[key]
            val resolved = cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
            rateCache[key] = resolved
            return resolved
        }
        displayCurrencies.forEach { target ->
            val rTotal = rate(invoiceCurrency, target)
            val rOut = rate(receivableCurrency, target)
            if (rTotal != null && rTotal > 0.0) {
                totals[target] = invoice.total * rTotal
            }
            if (rOut != null && rOut > 0.0) {
                outs[target] = invoice.outstandingAmount * rOut
            }
        }
        totalByCurrency = totals
        outstandingByCurrency = outs
    }
    val formattedTotal = formatDoubleToString(invoice.total, 2)
    val formattedOutstanding = formatDoubleToString(invoice.outstandingAmount, 2)
    val statusLabel = invoice.status ?: "Sin estado"
    val normalizedStatus = invoice.status?.trim()?.lowercase()
    val hasPayments = invoice.paidAmount > 0.0 || invoice.payments.any { it.amount > 0.0 }
    val isDraftOrUnpaid = normalizedStatus == "draft" || normalizedStatus == "unpaid"
    val isPaidOrPartly = normalizedStatus == "paid" || normalizedStatus == "partly paid"
    val allowCancel = isDraftOrUnpaid && !hasPayments
    val allowReturn = isPaidOrPartly || hasPayments
    val (statusBg, statusText) = when (normalizedStatus) {
        "paid" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "partly paid" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        "overdue" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        "unpaid", "draft" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "cancelled" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.invoiceId, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = invoice.postingDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (invoice.isPos == true) "POS" else "Crédito",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusText
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val totalSymbol = invoiceCurrency.toCurrencySymbol().ifBlank { invoiceCurrency }
                    Text(
                        "$totalSymbol $formattedTotal",
                        style = MaterialTheme.typography.titleSmall
                    )
                    val totalSecondary = totalByCurrency
                        .filterKeys { !it.equals(invoiceCurrency, ignoreCase = true) }
                        .map { (cur, amount) ->
                            val symbol = cur.toCurrencySymbol().ifBlank { cur }
                            "$symbol ${formatAmount(amount)}"
                        }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(" · ")
                    if (!totalSecondary.isNullOrBlank()) {
                        Text(
                            totalSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val outSymbol =
                        receivableCurrency.toCurrencySymbol().ifBlank { receivableCurrency }
                    Text(
                        "$outSymbol $formattedOutstanding",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (invoice.outstandingAmount > 0.0)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                    val outstandingSecondary = outstandingByCurrency
                        .filterKeys { !it.equals(receivableCurrency, ignoreCase = true) }
                        .map { (cur, amount) ->
                            val symbol = cur.toCurrencySymbol().ifBlank { cur }
                            "$symbol ${formatAmount(amount)}"
                        }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(" · ")
                    if (!outstandingSecondary.isNullOrBlank()) {
                        Text(
                            outstandingSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (allowCancel) {
                    FilledTonalButton(
                        onClick = { onCancel(invoice.invoiceId) },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Cancelar")
                        }
                    }
                }
                if (allowReturn) {
                    OutlinedButton(
                        onClick = { onReturnTotal(invoice.invoiceId) },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retorno total")
                    }
                }
                if (allowReturn) {
                    OutlinedButton(
                        onClick = { onPartialReturn(invoice.invoiceId) },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retorno parcial")
                    }
                }
            }
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

private enum class ReturnDestination(val label: String) {
    RETURN("Reembolso"),
    CREDIT("Crédito a favor")
}

@Composable
private fun CustomerOutstandingSummary(
    customer: CustomerBO,
    invoices: List<SalesInvoiceBO>,
    posBaseCurrency: String,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager
) {
    val strings = LocalAppStrings.current
    val posCurrency = normalizeCurrency(posBaseCurrency) ?: "USD"
    val displayCurrencies = remember(supportedCurrencies, posCurrency) {
        (supportedCurrencies + posCurrency).mapNotNull { normalizeCurrency(it) }.distinct()
    }
    var totalsByCurrency by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    LaunchedEffect(invoices, displayCurrencies, posCurrency) {
        val totals = mutableMapOf<String, Double>()
        val rateCache = mutableMapOf<Pair<String, String>, Double?>()
        suspend fun rate(from: String, to: String): Double? {
            if (from.equals(to, true)) return 1.0
            val key = from.uppercase() to to.uppercase()
            if (rateCache.containsKey(key)) return rateCache[key]
            val resolved = cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
            rateCache[key] = resolved
            return resolved
        }
        if (invoices.isNotEmpty()) {
            invoices.forEach { invoice ->
                val invoiceCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                    ?: normalizeCurrency(invoice.currency)
                    ?: posCurrency
                displayCurrencies.forEach { target ->
                    val r = rate(invoiceCurrency, target)
                    if (r != null && r > 0.0) {
                        totals[target] = (totals[target] ?: 0.0) + invoice.outstandingAmount * r
                    }
                }
            }
        } else {
            val baseAmount = customer.totalPendingAmount
                ?: customer.currentBalance ?: 0.0
            displayCurrencies.forEach { target ->
                val r = rate(posCurrency, target)
                if (r != null && r > 0.0) {
                    totals[target] = baseAmount * r
                }
            }
        }
        totalsByCurrency = totals
    }

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
        if (totalsByCurrency.isEmpty()) {
            Text(strings.customer.outstandingSummaryAmountLabel)
        } else {
            totalsByCurrency.forEach { (currency, amount) ->
                val symbol = currency.toCurrencySymbol().ifBlank { currency }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(currency)
                    Text("$symbol ${formatAmount(amount)}")
                }
            }
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
        else -> {}
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

private fun isWithinDays(postingDate: String?, days: Int): Boolean {
    val invoiceDate = parsePostingDate(postingDate) ?: return false
    val threshold = currentLocalDate().minus(DatePeriod(days = days))
    return invoiceDate >= threshold
}

private fun parsePostingDate(value: String?): LocalDate? {
    val raw = value?.substringBefore('T')?.substringBefore(' ')?.trim()
    if (raw.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(raw) }.getOrNull()
}

private fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun formatAmount(value: Double): String = formatDoubleToString(bd(value).toDouble(0), 2)
