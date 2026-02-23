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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.CustomerCounts
import com.erpnext.pos.domain.models.CustomerQuickActionType
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.ReturnDestinationPolicy
import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.CreateCustomerInput
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.CurrencyService
import com.erpnext.pos.utils.QuickActions.customerQuickActions
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.resolveCompanyToTargetAmount
import com.erpnext.pos.utils.resolveInvoiceDisplayAmounts
import com.erpnext.pos.utils.resolvePaymentCurrencyForMode
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.AppTextField
import com.erpnext.pos.views.billing.MoneyTextField
import kotlinx.coroutines.flow.Flow
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
    customersPagingFlow: Flow<PagingData<CustomerBO>>,
    outstandingInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    historyInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    invoicesState: CustomerInvoicesState,
    paymentState: CustomerPaymentState,
    historyState: CustomerInvoiceHistoryState,
    historyMessage: String?,
    returnInfoMessage: String?,
    historyBusy: Boolean,
    customerMessage: String?,
    dialogDataState: CustomerDialogDataState,
    returnPolicy: ReturnPolicySettings,
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
    val posCurrency = normalizeCurrency(posContext?.currency)
    val companyCurrency = normalizeCurrency(paymentState.baseCurrency)
    val supportedCurrencies = remember(
        posContext?.allowedCurrencies, posContext?.currency, paymentState.baseCurrency
    ) {
        val fromModes = posContext?.allowedCurrencies?.map { it.code }.orEmpty()
        val merged = (fromModes + listOfNotNull(
            posContext?.currency, paymentState.baseCurrency
        )).map { normalizeCurrency(it) }.distinct()
        merged.ifEmpty { listOf(posCurrency) }
    }
    val customersPagingItems = customersPagingFlow.collectAsLazyPagingItems()
    val outstandingInvoicesPagingItems = outstandingInvoicesPagingFlow.collectAsLazyPagingItems()
    val historyInvoicesPagingItems = historyInvoicesPagingFlow.collectAsLazyPagingItems()
    var baseCounts by remember { mutableStateOf(CustomerCounts(0, 0)) }
    val isDesktop = getPlatformName() == "Desktop"
    val customerListState = rememberLazyListState()
    val showBackToTop by remember {
        derivedStateOf { customerListState.firstVisibleItemIndex > 0 }
    }

    val hasCustomersLoaded =
        customersPagingItems.itemCount > 0 || customersPagingItems.loadState.refresh is LoadState.Loading
    val filterElevation by animateDpAsState(
        targetValue = if (hasCustomersLoaded) 4.dp else 0.dp, label = "filterElevation"
    )


    LaunchedEffect(outstandingCustomer?.name) {
        outstandingCustomer?.let { customer ->
            actions.loadOutstandingInvoices(customer)
        }
    }
    LaunchedEffect(state, searchQuery, selectedState) {
        if (searchQuery.isEmpty() && selectedState == "Todos" && state is CustomerState.Success) {
            baseCounts = CustomerCounts(
                total = state.totalCount,
                pending = state.pendingCount
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
                onClick = { showNewCustomerDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo cliente")
            }
        }) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier.padding(paddingValues).fillMaxSize()
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
                            actions.onSearchQueryChanged(it)
                        },
                        onStateChange = {
                            selectedState = it ?: "Todos"
                            actions.onStateSelected(it ?: "Todos")
                        },
                        modifier = Modifier.padding(horizontal = contentPadding, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenido principal según estado
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding)
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
                                errorMessage = state.message, onRetry = actions.fetchAll
                            )
                        }

                        is CustomerState.Success -> {
                            val refreshState = customersPagingItems.loadState.refresh
                            val appendState = customersPagingItems.loadState.append
                            val isLoading = refreshState is LoadState.Loading
                            val isEmpty = customersPagingItems.itemCount == 0 && !isLoading
                            val hasError = refreshState is LoadState.Error || appendState is LoadState.Error

                            if (hasError) {
                                val errorMessage = (refreshState as? LoadState.Error)?.error?.message
                                    ?: (appendState as? LoadState.Error)?.error?.message
                                    ?: "Error al cargar clientes"
                                FullScreenErrorMessage(
                                    errorMessage = errorMessage,
                                    onRetry = {
                                        customersPagingItems.refresh()
                                        actions.fetchAll()
                                    }
                                )
                            } else if (isLoading && customersPagingItems.itemCount == 0) {
                                CustomerShimmerList()
                            } else if (isEmpty) {
                                EmptyStateMessage(
                                    message = if (searchQuery.isEmpty()) strings.customer.emptyCustomers
                                    else strings.customer.emptySearchCustomers,
                                    icon = Icons.Filled.People
                                )
                            } else {
                                LaunchedEffect(customersPagingItems.itemCount, isWideLayout) {
                                    if (isWideLayout) {
                                        val loaded = customersPagingItems.itemSnapshotList.items
                                        if (selectedCustomer == null || loaded.none { it.name == selectedCustomer?.name }) {
                                            selectedCustomer = loaded.firstOrNull()
                                        }
                                    }
                                }
                                if (isWideLayout) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(0.65f)) {
                                            CustomerListPane(
                                                customers = customersPagingItems,
                                                posCurrency = posCurrency,
                                                companyCurrency = companyCurrency,
                                                cashboxManager = cashboxManager,
                                                listState = customerListState,
                                                showBackToTop = showBackToTop,
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
                                                        CustomerQuickActionType.PendingInvoices, CustomerQuickActionType.RegisterPayment -> {
                                                            selectedCustomer = customer
                                                            rightPanelTab = CustomerPanelTab.Pending
                                                        }

                                                        CustomerQuickActionType.InvoiceHistory -> {
                                                            selectedCustomer = customer
                                                            rightPanelTab = CustomerPanelTab.History
                                                        }

                                                        else -> handleQuickAction(
                                                            actions, customer, actionType
                                                        )
                                                    }
                                                })
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
                                                outstandingInvoicesPagingItems = outstandingInvoicesPagingItems,
                                                historyState = historyState,
                                                historyInvoicesPagingItems = historyInvoicesPagingItems,
                                                historyMessage = historyMessage,
                                                returnInfoMessage = returnInfoMessage,
                                                historyBusy = historyBusy,
                                                supportedCurrencies = supportedCurrencies,
                                                cashboxManager = cashboxManager,
                                                posBaseCurrency = companyCurrency,
                                                returnPolicy = returnPolicy,
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
                                                onDownloadInvoicePdf = actions.onDownloadInvoicePdf,
                                                onInvoiceHistoryAction = { invoiceId, action, reason, refundMode, refundReference, applyRefund ->
                                                    actions.onInvoiceHistoryAction(
                                                        invoiceId,
                                                        action,
                                                        reason,
                                                        refundMode,
                                                        refundReference,
                                                        applyRefund
                                                    )
                                                },
                                                loadLocalInvoice = actions.loadInvoiceLocal,
                                                onSubmitPartialReturn = actions.onInvoicePartialReturn
                                            )
                                        }
                                    }
                                } else {
                                    CustomerListPane(
                                        customers = customersPagingItems,
                                        posCurrency = posCurrency,
                                        companyCurrency = companyCurrency,
                                        cashboxManager = cashboxManager,
                                        listState = customerListState,
                                        showBackToTop = showBackToTop,
                                        isWideLayout = isWideLayout,
                                        isDesktop = isDesktop,
                                        onOpenQuickActions = { quickActionsCustomer = it },
                                        onSelect = {},
                                        onQuickAction = { customer, actionType ->
                                            when (actionType) {
                                                CustomerQuickActionType.PendingInvoices, CustomerQuickActionType.RegisterPayment -> {
                                                    outstandingCustomer = customer
                                                }

                                                CustomerQuickActionType.InvoiceHistory -> {
                                                    historyCustomer = customer
                                                }

                                                else -> handleQuickAction(
                                                    actions, customer, actionType
                                                )
                                            }
                                        })
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
                cashboxManager = cashboxManager,
                onDismiss = { },
                onActionSelected = { actionType ->
                    when (actionType) {
                        CustomerQuickActionType.PendingInvoices, CustomerQuickActionType.RegisterPayment -> {
                            outstandingCustomer = customer
                        }

                        CustomerQuickActionType.InvoiceHistory -> {
                            historyCustomer = customer
                        }

                        else -> handleQuickAction(actions, customer, actionType)
                    }
                })
        }

        outstandingCustomer?.let { customer ->
            CustomerOutstandingInvoicesSheet(
                customer = customer,
                invoicesState = invoicesState,
                outstandingInvoicesPagingItems = outstandingInvoicesPagingItems,
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
                },
                onDownloadInvoicePdf = actions.onDownloadInvoicePdf
            )
        }

        historyCustomer?.let { customer ->
            CustomerInvoiceHistorySheet(
                customer = customer,
                historyState = historyState,
                historyInvoicesPagingItems = historyInvoicesPagingItems,
                historyMessage = historyMessage,
                historyBusy = historyBusy,
                paymentState = paymentState,
                posBaseCurrency = companyCurrency,
                cashboxManager = cashboxManager,
                returnPolicy = returnPolicy,
                onAction = { invoiceId, action, reason, refundMode, refundReference, applyRefund ->
                    actions.onInvoiceHistoryAction(
                        invoiceId, action, reason, refundMode, refundReference, applyRefund
                    )
                },
                onDownloadInvoicePdf = actions.onDownloadInvoicePdf,
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

@Composable
private fun CustomerListPane(
    customers: androidx.paging.compose.LazyPagingItems<CustomerBO>,
    posCurrency: String,
    companyCurrency: String,
    cashboxManager: CashBoxManager,
    listState: LazyListState,
    showBackToTop: Boolean,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    onOpenQuickActions: (CustomerBO) -> Unit,
    onSelect: (CustomerBO) -> Unit,
    onQuickAction: (CustomerBO, CustomerQuickActionType) -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        CustomerListContent(
            customers = customers,
            posCurrency = posCurrency,
            companyCurrency = companyCurrency,
            cashboxManager = cashboxManager,
            isWideLayout = isWideLayout,
            isDesktop = isDesktop,
            listState = listState,
            onOpenQuickActions = onOpenQuickActions,
            onSelect = onSelect,
            onQuickAction = onQuickAction
        )
        if (showBackToTop) {
            FilledTonalButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 52.dp, bottom = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Back to top"
                )
            }
        }
    }
}

private enum class CustomerPanelTab(val label: String) {
    Details("Resumen"), Pending("Pendientes"), History("Historial")
}

private enum class CustomerDialogTab(val label: String) {
    Personal("Principal"), Contact("Contacto"), Tax("Impuestos"), Accounting("Contabilidad")
}

private enum class NicaraguanTaxRegime(val label: String, val hint: String) {
    Simplified("Régimen simplificado", "0013012120003D"),
    General("Régimen general", "J0310000000001")
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
                        onClick = { onStateChange("Todos") })
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
                            onClick = { onStateChange(state) })
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
                        onClick = { onStateChange("Todos") })
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
                        onClick = { onStateChange(state) })
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
                    }) {
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
        keyboardActions = KeyboardActions(onSearch = {
            onSearchAction?.invoke()
            keyboardController?.hide()
        }, onDone = { keyboardController?.hide() }),
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
    customers: androidx.paging.compose.LazyPagingItems<CustomerBO>,
    posCurrency: String,
    companyCurrency: String,
    cashboxManager: CashBoxManager,
    listState: LazyListState,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    onOpenQuickActions: (CustomerBO) -> Unit,
    onSelect: (CustomerBO) -> Unit,
    onQuickAction: (CustomerBO, CustomerQuickActionType) -> Unit
) {
    val spacing = if (isWideLayout) 16.dp else 12.dp
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items(
            count = customers.itemCount,
            key = { index -> customers[index]?.name ?: "customer_list_$index" }
        ) { index ->
            val customer = customers[index] ?: return@items
            CustomerItem(
                customer = customer,
                posCurrency = posCurrency,
                companyCurrency = companyCurrency,
                isDesktop = isDesktop,
                onSelect = onSelect,
                onOpenQuickActions = { onOpenQuickActions(customer) },
                onQuickAction = { actionType -> onQuickAction(customer, actionType) },
                cashboxManager = cashboxManager
            )
        }
        if (customers.loadState.append is LoadState.Loading) {
            item(key = "customers_append_loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        val appendError = customers.loadState.append as? LoadState.Error
        if (appendError != null) {
            item(key = "customers_append_error") {
                OutlinedButton(
                    onClick = { customers.retry() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reintentar carga")
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailPanel(
    customer: CustomerBO,
    paymentState: CustomerPaymentState,
    invoicesState: CustomerInvoicesState,
    historyState: CustomerInvoiceHistoryState,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager
) {
    val companyCurrency = normalizeCurrency(paymentState.baseCurrency)
    val pendingAmount =
        bd(customer.totalPendingAmount ?: customer.currentBalance ?: 0.0).moneyScale(2).toDouble(2)
    val displayCurrencies = remember(supportedCurrencies, companyCurrency) {
        CurrencyService.resolveDisplayCurrencies(
            supported = supportedCurrencies,
            invoiceCurrency = null,
            receivableCurrency = companyCurrency,
            posCurrency = companyCurrency
        )
    }

    LaunchedEffect(displayCurrencies, companyCurrency, pendingAmount) {
        val resolved = mutableMapOf<String, Double>()
        displayCurrencies.forEach { currency ->
            val converted = CurrencyService.convertFromReceivable(
                amount = pendingAmount,
                receivableCurrency = companyCurrency,
                targetCurrency = currency,
                invoiceCurrency = null,
                conversionRate = null,
                customExchangeRate = null,
                rateResolver = { from, to ->
                    cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
                })
            if (converted != null) {
                resolved[currency] = converted
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        /*Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape
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
            value = formatCurrency(primaryCurrency, primaryAmount),
            secondaryValue = secondaryValue,
            isCritical = (customer.pendingInvoices ?: 0) > 0
        )*/

        val historyInvoices =
            (historyState as? CustomerInvoiceHistoryState.Success)?.invoices.orEmpty()
        val recentInvoices = historyInvoices.filter { isWithinDays(it.postingDate, 90) }
        val totalSpentBase = recentInvoices.sumOf {
            val invoiceCurrency = normalizeCurrency(it.currency)
            toBaseAmount(it.total, invoiceCurrency, companyCurrency, it.conversionRate)
        }
        val avgTicket = if (recentInvoices.isNotEmpty()) {
            totalSpentBase / recentInvoices.size
        } else 0.0
        val lastPurchase = recentInvoices.maxByOrNull {
            parsePostingDate(it.postingDate) ?: LocalDate(1970, 1, 1)
        }?.postingDate
        val creditAvailable = customer.availableCredit

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            /*Text(
                "Resumen inteligente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )*/
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SummaryStatChip(
                        label = "Compras 90d", value = recentInvoices.size.toString()
                    )
                }
                item {
                    SummaryStatChip(
                        label = "Total 90d", value = if (recentInvoices.isNotEmpty()) {
                            formatCurrency(companyCurrency, totalSpentBase)
                        } else {
                            "—"
                        }
                    )
                }
                item {
                    SummaryStatChip(
                        label = "Ticket prom.", value = if (recentInvoices.isNotEmpty()) {
                            formatCurrency(companyCurrency, avgTicket)
                        } else {
                            "—"
                        }
                    )
                }
                item {
                    SummaryStatChip(
                        label = "Última compra", value = lastPurchase ?: "—"
                    )
                }
                item {
                    SummaryStatChip(
                        label = "Crédito disp.",
                        value = creditAvailable?.let { formatCurrency(companyCurrency, it) } ?: "—")
                }
            }
        }

        if ((customer.totalPendingAmount ?: customer.currentBalance ?: 0.0) > 0.0) {
            CustomerOutstandingSummary(
                customer = customer,
                invoices = emptyList(),
                posBaseCurrency = companyCurrency,
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
    outstandingInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
    historyState: CustomerInvoiceHistoryState,
    historyInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
    historyMessage: String?,
    returnInfoMessage: String?,
    historyBusy: Boolean,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
    posBaseCurrency: String,
    returnPolicy: ReturnPolicySettings,
    onRegisterPayment: (
        invoiceId: String, modeOfPayment: String, enteredAmount: Double, enteredCurrency: String, referenceNumber: String
    ) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
    onInvoiceHistoryAction: (
        invoiceId: String,
        action: InvoiceCancellationAction,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean
    ) -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments?,
    onSubmitPartialReturn: (
        invoiceId: String, reason: String?, refundModeOfPayment: String?, refundReferenceNo: String?, applyRefund: Boolean, itemsToReturnByCode: Map<String, Double>
    ) -> Unit
) {
    val tabs = remember {
        listOf(CustomerPanelTab.Details, CustomerPanelTab.Pending, CustomerPanelTab.History)
    }
    val selectedIndex = tabs.indexOf(rightPanelTab).coerceAtLeast(0)
    Column(modifier = Modifier.fillMaxSize()) {
        CustomerPanelHeader(
            customer = customer,
            invoicesState = invoicesState,
            cashboxManager = cashboxManager,
            baseCurrency = paymentState.baseCurrency
        )

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEach { tab ->
                val enabled = customer != null
                Tab(selected = tab == rightPanelTab, onClick = {
                    if (enabled) onTabChange(tab)
                }, enabled = enabled, text = { Text(tab.label) })
            }
        }
        if (rightPanelTab == CustomerPanelTab.Details && !returnInfoMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = returnInfoMessage,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            historyState = historyState,
                            supportedCurrencies = supportedCurrencies,
                            cashboxManager = cashboxManager
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
                            outstandingInvoicesPagingItems = outstandingInvoicesPagingItems,
                            paymentState = paymentState,
                            onRegisterPayment = onRegisterPayment,
                            onDownloadInvoicePdf = onDownloadInvoicePdf,
                            modifier = Modifier.fillMaxSize().padding(20.dp)
                        )
                    } else {
                        EmptyStateMessage(
                            message = "Selecciona un cliente",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                CustomerPanelTab.History -> {
                    if (customer != null) {
                        CustomerInvoiceHistoryContent(
                            customer = customer,
                            historyState = historyState,
                            historyInvoicesPagingItems = historyInvoicesPagingItems,
                            historyMessage = historyMessage,
                            historyBusy = historyBusy,
                            paymentState = paymentState,
                            posBaseCurrency = posBaseCurrency,
                            cashboxManager = cashboxManager,
                            returnPolicy = returnPolicy,
                            onAction = onInvoiceHistoryAction,
                            onDownloadInvoicePdf = onDownloadInvoicePdf,
                            loadLocalInvoice = loadLocalInvoice,
                            onSubmitPartialReturn = onSubmitPartialReturn,
                            modifier = Modifier.fillMaxSize().padding(20.dp)
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
@Suppress("UNUSED_PARAMETER")
private fun CustomerPanelHeader(
    customer: CustomerBO?,
    invoicesState: CustomerInvoicesState,
    cashboxManager: CashBoxManager,
    baseCurrency: String
) {
    val companyCurrency = normalizeCurrency(baseCurrency)
    val pendingCount = customer?.pendingInvoices ?: 0
    val invoiceCurrency = companyCurrency
    val pendingCompanyAmount = customer?.totalPendingAmount ?: customer?.currentBalance ?: 0.0

    val pendingCompany =
        bd(customer?.totalPendingAmount ?: customer?.currentBalance ?: 0.0).toDouble(2)
    var pendingPos by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(pendingCompany, companyCurrency, invoiceCurrency) {
        pendingPos = if (invoiceCurrency.equals(companyCurrency, ignoreCase = true)) {
            pendingCompany
        } else {
            resolveCompanyToTargetAmount(
                amountCompany = pendingCompany,
                companyCurrency = companyCurrency,
                targetCurrency = invoiceCurrency,
                rateResolver = { from, to ->
                    cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
                }
            )
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant
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

            if (customer != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderChip(
                        label = "Pendientes",
                        value = pendingCount.toString(),
                        isCritical = pendingCount > 0
                    )
                    HeaderChip(
                        label = invoiceCurrency,
                        value = formatCurrency(invoiceCurrency, bd(pendingPos ?: 0.0).toDouble(0)),
                        isCritical = pendingCount > 0
                    )
                    HeaderChip(
                        label = companyCurrency,
                        value = formatCurrency(companyCurrency, pendingCompanyAmount),
                        isCritical = pendingCount > 0
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
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
    var rucRegionExpanded by remember { mutableStateOf(false) }
    var phoneRegionExpanded by remember { mutableStateOf(false) }
    var niTaxRegimeExpanded by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(CustomerDialogTab.Personal) }
    var niTaxRegime by rememberSaveable { mutableStateOf(NicaraguanTaxRegime.General.name) }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }
    val regionOptions = remember { customerRegionOptions() }
    val defaultRegionCode = remember(companies) {
        resolveRegionCodeFromCountry(companies.firstOrNull()?.country)
    }
    var rucRegionCode by rememberSaveable { mutableStateOf(defaultRegionCode) }
    var phoneRegionCode by rememberSaveable { mutableStateOf(defaultRegionCode) }
    val selectedRucRegion = remember(rucRegionCode, regionOptions) {
        regionOptions.firstOrNull { it.code == rucRegionCode } ?: regionOptions.first()
    }
    val selectedPhoneRegion = remember(phoneRegionCode, regionOptions) {
        regionOptions.firstOrNull { it.code == phoneRegionCode } ?: regionOptions.first()
    }
    val selectedNicaraguanTaxRegime = remember(niTaxRegime) {
        NicaraguanTaxRegime.entries.firstOrNull { it.name == niTaxRegime }
            ?: NicaraguanTaxRegime.General
    }
    val taxIdentifierHint = remember(rucRegionCode, selectedRucRegion, selectedNicaraguanTaxRegime) {
        if (rucRegionCode == "NI") {
            selectedNicaraguanTaxRegime.hint
        } else {
            selectedRucRegion.taxIdHint
        }
    }
    val creditCurrency = remember(companies, internalCompany, isInternalCustomer) {
        val selectedCompany = if (isInternalCustomer && internalCompany.isNotBlank()) {
            companies.firstOrNull { it.company.equals(internalCompany, ignoreCase = true) }
        } else {
            companies.firstOrNull()
        }
        normalizeCurrency(selectedCompany?.defaultCurrency)
    }
    val emailTrimmed = email.trim()
    val emailInvalid = emailTrimmed.isNotBlank() && !isValidEmailAddress(emailTrimmed)
    val creditInvalid = creditLimit.isNotBlank() && creditLimit.toDoubleOrNull() == null
    val nameInvalid = submitAttempted && name.isBlank()
    val internalCompanyInvalid = submitAttempted && isInternalCustomer && internalCompany.isBlank()
    val isValid = name.isNotBlank() &&
        (!isInternalCustomer || internalCompany.isNotBlank()) &&
        !emailInvalid &&
        !creditInvalid
    val requiredCompleted = buildList {
        add(name.isNotBlank())
        add(!isInternalCustomer || internalCompany.isNotBlank())
    }.count { it }
    val tabBodyMinHeight = 380.dp
    val tabBodyMaxHeight = 560.dp
    val tabScrollState = rememberScrollState()
    val personalNameFocusRequester = remember { FocusRequester() }
    val contactMobileFocusRequester = remember { FocusRequester() }
    val taxIdFocusRequester = remember { FocusRequester() }
    val creditLimitFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedTab) {
        tabScrollState.scrollTo(0)
        kotlinx.coroutines.delay(90)
        when (selectedTab) {
            CustomerDialogTab.Personal -> personalNameFocusRequester.requestFocus()
            CustomerDialogTab.Contact -> contactMobileFocusRequester.requestFocus()
            CustomerDialogTab.Tax -> taxIdFocusRequester.requestFocus()
            CustomerDialogTab.Accounting -> creditLimitFocusRequester.requestFocus()
        }
    }

    @Composable
    fun AppTextField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        placeholder: String? = null,
        singleLine: Boolean = true,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        isError: Boolean = false,
        supportingText: (@Composable () -> Unit)? = null,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        leadingIcon: (@Composable () -> Unit)? = null,
        trailingIcon: (@Composable () -> Unit)? = null,
    ) {
        CustomerDialogField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            placeholder = placeholder,
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            supportingText = supportingText,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(min = 360.dp, max = 900.dp)
                .heightIn(min = 700.dp, max = 700.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nuevo cliente",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Completa la ficha para registrar rápidamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "Requeridos $requiredCompleted/2",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                    CustomerDialogTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab.ordinal == index,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (selectedTab.ordinal == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            })
                    }
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .heightIn(min = tabBodyMinHeight, max = tabBodyMaxHeight)
                            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                            .verticalScroll(tabScrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            CustomerDialogTab.Personal -> {
                                AppTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = "Nombre del cliente",
                                    placeholder = "Cliente S.A.",
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next
                                    ),
                                    isError = nameInvalid,
                                    supportingText = if (nameInvalid) {
                                        {
                                            Text(
                                                text = "El nombre es obligatorio.",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Person, contentDescription = null
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(personalNameFocusRequester)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = typeExpanded,
                                    onExpandedChange = { typeExpanded = !typeExpanded }
                                ) {
                                    AppTextField(
                                        value = customerType,
                                        onValueChange = {},
                                        label = "Tipo de cliente",
                                        placeholder = "Seleccionar",
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor(
                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                        ).fillMaxWidth(),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Badge, contentDescription = null
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                                        })
                                    ExposedDropdownMenu(
                                        expanded = typeExpanded,
                                        onDismissRequest = { typeExpanded = false }
                                    ) {
                                        listOf("Individual", "Empresa").forEach { option ->
                                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                                customerType = option
                                                typeExpanded = false
                                            })
                                        }
                                    }
                                }
                                if (customerGroups.isNotEmpty()) {
                                    ExposedDropdownMenuBox(
                                        expanded = groupExpanded,
                                        onExpandedChange = { groupExpanded = !groupExpanded }
                                    ) {
                                        AppTextField(
                                            value = customerGroup,
                                            onValueChange = {},
                                            label = "Grupo de cliente",
                                            placeholder = "Seleccionar",
                                            readOnly = true,
                                            modifier = Modifier.menuAnchor(
                                                ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                            ).fillMaxWidth(),
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Group, contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded)
                                            })
                                        ExposedDropdownMenu(
                                            expanded = groupExpanded,
                                            onDismissRequest = { groupExpanded = false }
                                        ) {
                                            customerGroups.forEach { option ->
                                                val label =
                                                    option.displayName?.takeIf { it.isNotBlank() }
                                                        ?: option.name
                                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                                    customerGroup = option.name
                                                    groupExpanded = false
                                                })
                                            }
                                        }
                                    }
                                } else {
                                    AppTextField(
                                        value = customerGroup,
                                        onValueChange = { customerGroup = it },
                                        label = "Grupo de cliente",
                                        placeholder = "Retail",
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Group, contentDescription = null
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (territories.isNotEmpty()) {
                                    ExposedDropdownMenuBox(
                                        expanded = territoryExpanded,
                                        onExpandedChange = { territoryExpanded = !territoryExpanded }
                                    ) {
                                        AppTextField(
                                            value = territory,
                                            onValueChange = {},
                                            label = "Territorio",
                                            placeholder = "Seleccionar",
                                            readOnly = true,
                                            modifier = Modifier.menuAnchor(
                                                ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                            ).fillMaxWidth(),
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Place, contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = territoryExpanded)
                                            })
                                        ExposedDropdownMenu(
                                            expanded = territoryExpanded,
                                            onDismissRequest = { territoryExpanded = false }
                                        ) {
                                            territories.forEach { option ->
                                                val label =
                                                    option.displayName?.takeIf { it.isNotBlank() }
                                                        ?: option.name
                                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                                    territory = option.name
                                                    territoryExpanded = false
                                                })
                                            }
                                        }
                                    }
                                } else {
                                    AppTextField(
                                        value = territory,
                                        onValueChange = { territory = it },
                                        label = "Territorio",
                                        placeholder = "Managua",
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Place, contentDescription = null
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isInternalCustomer,
                                        onCheckedChange = { isInternalCustomer = it })
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
                                            onExpandedChange = { companyExpanded = !companyExpanded }
                                        ) {
                                            AppTextField(
                                                value = internalCompany,
                                                onValueChange = {},
                                                label = "Compañía",
                                                placeholder = "Seleccionar",
                                                readOnly = true,
                                                modifier = Modifier.menuAnchor(
                                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                                ).fillMaxWidth(),
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Business,
                                                        contentDescription = null
                                                    )
                                                },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded)
                                                })
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
                                                        })
                                                }
                                            }
                                        }
                                    } else {
                                        AppTextField(
                                            value = internalCompany,
                                            onValueChange = { internalCompany = it },
                                            label = "Compañía",
                                            placeholder = "Escribe la compañía",
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                            isError = internalCompanyInvalid,
                                            supportingText = if (internalCompanyInvalid) {
                                                {
                                                    Text(
                                                        text = "Selecciona o escribe la compañía.",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Business, contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                AppTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = "Notas",
                                    placeholder = "Observaciones internas",
                                    singleLine = false,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Note, contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            CustomerDialogTab.Contact -> {
                                ExposedDropdownMenuBox(
                                    expanded = phoneRegionExpanded,
                                    onExpandedChange = { phoneRegionExpanded = !phoneRegionExpanded }
                                ) {
                                    CustomerDialogField(
                                        value = "${selectedPhoneRegion.code} ${selectedPhoneRegion.dialCode}",
                                        onValueChange = {},
                                        label = "Región de teléfono",
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor(
                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                        ).fillMaxWidth(),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Place,
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = phoneRegionExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = phoneRegionExpanded,
                                        onDismissRequest = { phoneRegionExpanded = false }
                                    ) {
                                        regionOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("${option.code} ${option.dialCode} · ${option.country}")
                                                },
                                                onClick = {
                                                    phoneRegionCode = option.code
                                                    phoneRegionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                AppTextField(
                                    value = mobile,
                                    onValueChange = { mobile = it },
                                    label = "Móvil",
                                    placeholder = "8888 8888",
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Phone, contentDescription = null
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(contactMobileFocusRequester)
                                )
                                AppTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = "Teléfono",
                                    placeholder = "2222 2222",
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Call, contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Se guardará con prefijo regional ${selectedPhoneRegion.dialCode}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AppTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = "Correo",
                                    placeholder = "cliente@correo.com",
                                    isError = emailInvalid,
                                    supportingText = if (emailInvalid) {
                                        {
                                            Text(
                                                text = "Formato de correo inválido.",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Email, contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AppTextField(
                                    value = addressLine,
                                    onValueChange = { addressLine = it },
                                    label = "Dirección línea 1",
                                    placeholder = "Calle principal",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Home, contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AppTextField(
                                    value = addressLine2,
                                    onValueChange = { addressLine2 = it },
                                    label = "Dirección línea 2",
                                    placeholder = "Referencias, barrio",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AppTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = "Ciudad",
                                    placeholder = "Managua",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AppTextField(
                                    value = state,
                                    onValueChange = { state = it },
                                    label = "Departamento",
                                    placeholder = "Managua",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                AppTextField(
                                    value = country,
                                    onValueChange = { country = it },
                                    label = "País",
                                    placeholder = "Nicaragua",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            CustomerDialogTab.Tax -> {
                                ExposedDropdownMenuBox(
                                    expanded = rucRegionExpanded,
                                    onExpandedChange = { rucRegionExpanded = !rucRegionExpanded }
                                ) {
                                    CustomerDialogField(
                                        value = selectedRucRegion.code,
                                        onValueChange = {},
                                        label = "Región",
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor(
                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                        ).fillMaxWidth(),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Badge,
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = rucRegionExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = rucRegionExpanded,
                                        onDismissRequest = { rucRegionExpanded = false }
                                    ) {
                                        regionOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text("${option.code} · ${option.country}") },
                                                onClick = {
                                                    rucRegionCode = option.code
                                                    rucRegionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                if (rucRegionCode == "NI") {
                                    ExposedDropdownMenuBox(
                                        expanded = niTaxRegimeExpanded,
                                        onExpandedChange = { niTaxRegimeExpanded = !niTaxRegimeExpanded }
                                    ) {
                                        CustomerDialogField(
                                            value = selectedNicaraguanTaxRegime.label,
                                            onValueChange = {},
                                            label = "Tipo de RUC",
                                            readOnly = true,
                                            modifier = Modifier.menuAnchor(
                                                ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                            ).fillMaxWidth(),
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Badge,
                                                    contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = niTaxRegimeExpanded)
                                            }
                                        )
                                        ExposedDropdownMenu(
                                            expanded = niTaxRegimeExpanded,
                                            onDismissRequest = { niTaxRegimeExpanded = false }
                                        ) {
                                            NicaraguanTaxRegime.entries.forEach { regime ->
                                                DropdownMenuItem(
                                                    text = { Text(regime.label) },
                                                    onClick = {
                                                        niTaxRegime = regime.name
                                                        niTaxRegimeExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                AppTextField(
                                    value = taxId,
                                    onValueChange = { taxId = it },
                                    label = "RUC / NIT",
                                    placeholder = taxIdentifierHint,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Badge, contentDescription = null
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(taxIdFocusRequester)
                                )
                                Text(
                                    text = if (rucRegionCode == "NI") {
                                        "Formato sugerido (${selectedNicaraguanTaxRegime.label}): $taxIdentifierHint"
                                    } else {
                                        "Formato sugerido: $taxIdentifierHint"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AppTextField(
                                    value = taxCategory,
                                    onValueChange = { taxCategory = it },
                                    label = "Categoría de impuesto",
                                    placeholder = "IVA General",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            CustomerDialogTab.Accounting -> {
                                MoneyTextField(
                                    currencyCode = creditCurrency,
                                    rawValue = creditLimit,
                                    onRawValueChange = { creditLimit = it },
                                    label = "Límite de crédito",
                                    isError = creditInvalid,
                                    supportingText = if (creditInvalid) {
                                        {
                                            Text(
                                                text = "Debe ser un número válido.",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    } else {
                                        {
                                            Text(
                                                text = "Moneda empresa: ${creditCurrency.toCurrencySymbol()} $creditCurrency",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    },
                                    imeAction = ImeAction.Next,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(creditLimitFocusRequester)
                                )
                                if (paymentTermsOptions.isNotEmpty()) {
                                    ExposedDropdownMenuBox(
                                        expanded = paymentExpanded,
                                        onExpandedChange = { paymentExpanded = !paymentExpanded }
                                    ) {
                                        AppTextField(
                                            value = selectedPaymentTerm,
                                            onValueChange = {},
                                            label = "Términos de pago",
                                            placeholder = "Seleccionar",
                                            readOnly = true,
                                            modifier = Modifier.menuAnchor(
                                                ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                            ).fillMaxWidth(),
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Schedule,
                                                    contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded)
                                            })
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
                                                    })
                                            }
                                        }
                                    }
                                } else {
                                    AppTextField(
                                        value = selectedPaymentTerm,
                                        onValueChange = { selectedPaymentTerm = it },
                                        label = "Términos de pago",
                                        placeholder = "Contado / 30 días",
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Schedule, contentDescription = null
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            submitAttempted = true
                            if (!isValid) return@Button
                            val normalizedTaxId = normalizeTaxIdentifier(taxId.trim())
                            val normalizedMobile = buildRegionalPhone(
                                dialCode = selectedPhoneRegion.dialCode,
                                value = mobile.trim()
                            )
                            val normalizedPhone = buildRegionalPhone(
                                dialCode = selectedPhoneRegion.dialCode,
                                value = phone.trim()
                            )
                            onSubmit(
                                CreateCustomerInput(
                                    customerName = name.trim(),
                                    customerType = customerType,
                                    customerGroup = customerGroup.trim().ifBlank { null },
                                    territory = territory.trim().ifBlank { null },
                                    isInternalCustomer = isInternalCustomer,
                                    internalCompany = internalCompany.trim().ifBlank { null },
                                    taxId = normalizedTaxId.ifBlank { null },
                                    taxCategory = taxCategory.trim().ifBlank { null },
                                    email = email.trim().ifBlank { null },
                                    mobileNo = normalizedMobile.ifBlank { null },
                                    phone = normalizedPhone.ifBlank { null },
                                    addressLine1 = addressLine.trim().ifBlank { null },
                                    addressLine2 = addressLine2.trim().ifBlank { null },
                                    city = city.trim().ifBlank { null },
                                    state = state.trim().ifBlank { null },
                                    country = country.trim().ifBlank { null },
                                    creditLimit = creditLimit.toDoubleOrNull(),
                                    paymentTerms = selectedPaymentTerm.trim().ifBlank { null },
                                    notes = notes.trim().ifBlank { null })
                            )
                            onDismiss()
                        }, enabled = isValid || !submitAttempted, modifier = Modifier.weight(1f)
                    ) {
                        Text("Crear cliente")
                    }
                }
            }
        }
    }
}

private fun isValidEmailAddress(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isBlank()) return false
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    return emailRegex.matches(normalized)
}

private data class RegionInputOption(
    val code: String,
    val dialCode: String,
    val country: String,
    val taxIdHint: String
)

private fun customerRegionOptions(): List<RegionInputOption> = listOf(
    RegionInputOption("NI", "+505", "Nicaragua", "J0310000000001"),
    RegionInputOption("CR", "+506", "Costa Rica", "3-101-123456"),
    RegionInputOption("HN", "+504", "Honduras", "08011999123456"),
    RegionInputOption("SV", "+503", "El Salvador", "0614-290180-101-3"),
    RegionInputOption("GT", "+502", "Guatemala", "1234567-8"),
    RegionInputOption("PA", "+507", "Panamá", "1556789-1-123456"),
    RegionInputOption("MX", "+52", "México", "XAXX010101000"),
    RegionInputOption("US", "+1", "Estados Unidos", "12-3456789")
)

private fun resolveRegionCodeFromCountry(country: String?): String {
    val normalized = country?.trim()?.lowercase().orEmpty()
    return when {
        normalized.contains("nicaragua") -> "NI"
        normalized.contains("costa rica") -> "CR"
        normalized.contains("honduras") -> "HN"
        normalized.contains("el salvador") -> "SV"
        normalized.contains("guatemala") -> "GT"
        normalized.contains("panama") || normalized.contains("panamá") -> "PA"
        normalized.contains("mexico") || normalized.contains("méxico") -> "MX"
        normalized.contains("united states") || normalized.contains("estados unidos") -> "US"
        else -> "NI"
    }
}

private fun normalizeTaxIdentifier(value: String): String {
    if (value.isBlank()) return ""
    return value.trim().replace(Regex("^[A-Za-z]{2}\\s*-\\s*"), "")
}

private fun buildRegionalPhone(dialCode: String, value: String): String {
    if (value.isBlank()) return ""
    val normalized = value.trim()
    if (normalized.startsWith("+")) return normalized
    return "$dialCode $normalized"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun CustomerItem(
    customer: CustomerBO,
    posCurrency: String,
    companyCurrency: String,
    cashboxManager: CashBoxManager,
    isDesktop: Boolean,
    onSelect: (CustomerBO) -> Unit,
    onOpenQuickActions: () -> Unit,
    onQuickAction: (CustomerQuickActionType) -> Unit
) {
    val strings = LocalAppStrings.current
    val isOverLimit = (customer.availableCredit ?: 0.0) < 0 || (customer.currentBalance ?: 0.0) > 0
    val pendingInvoices = customer.pendingInvoices ?: 0
    var isMenuExpanded by remember { mutableStateOf(false) }
    val quickActions = remember { customerQuickActions() }
    val avatarSize = if (isDesktop) 48.dp else 40.dp
    val companyCurr = normalizeCurrency(companyCurrency)
    val posCurr = normalizeCurrency(posCurrency)
    val pendingCompany =
        bd(customer.totalPendingAmount ?: customer.currentBalance ?: 0.0).toDouble(2)
    var pendingPos by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(pendingCompany, companyCurr, posCurr) {
        pendingPos = if (posCurr.equals(companyCurr, ignoreCase = true)) {
            pendingCompany
        } else {
            resolveCompanyToTargetAmount(
                amountCompany = pendingCompany,
                companyCurrency = companyCurr,
                targetCurrency = posCurr,
                rateResolver = { from, to ->
                    cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
                }
            )
        }
    }
    val statusLabel = when {
        isOverLimit -> strings.customer.overdueLabel
        pendingInvoices > 0 || pendingCompany > 0.0 -> strings.customer.pendingLabel
        else -> strings.customer.activeLabel
    }
    val statusColor = when {
        isOverLimit -> MaterialTheme.colorScheme.error
        pendingInvoices > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val emphasis = pendingInvoices > 0 || isOverLimit
    val cardShape = RoundedCornerShape(18.dp)
    val cardBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    )
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp).clip(cardShape)
            .clickable { onSelect(customer) }.pointerInput(customer.name, isDesktop) {
                if (!isDesktop) {
                    val totalDrag = 0f
                    detectHorizontalDragGestures(onDragEnd = {
                        if (kotlin.math.abs(totalDrag) > 64) {
                            onOpenQuickActions()
                        }
                    }, onHorizontalDrag = { _, _ ->
                    })
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
            modifier = Modifier.fillMaxWidth().background(cardBrush, cardShape)
                .border(1.dp, Color.Transparent, shape = cardShape)
                .padding(if (isDesktop) 10.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val context = LocalPlatformContext.current
            Row(
                modifier = Modifier.fillMaxWidth().height(42.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!customer.image.isNullOrEmpty()) {
                    AsyncImage(
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(50.dp)),
                        model = remember(customer.image) {
                            ImageRequest.Builder(context)
                                .data(customer.image.ifBlank { "https://placehold.co/600x400" })
                                .crossfade(true).build()
                        },
                        contentDescription = customer.name,
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = customer.customerName,
                        modifier = Modifier.size(avatarSize).clip(CircleShape).padding(12.dp),
                        tint = statusColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        customer.customerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (customer.mobileNo?.isNotEmpty() == true) {
                        Text(
                            text = customer.mobileNo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Codigo: ${customer.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    onDismissRequest = { isMenuExpanded = false }) {
                    quickActions.forEach { action ->
                        DropdownMenuItem(text = { Text(action.label) }, leadingIcon = {
                            Icon(action.icon, contentDescription = null)
                        }, onClick = {
                            isMenuExpanded = false
                            onQuickAction(action.type)
                        })
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(label = statusLabel, isCritical = emphasis)
                Column {
                    Text(
                        text = formatCurrency(posCurr, pendingPos ?: pendingCompany),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (emphasis) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (pendingPos != null && !posCurr.equals(companyCurr, ignoreCase = true)) {
                        Text(
                            text = formatCurrency(companyCurr, pendingCompany),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (pendingInvoices > 0) {
                    HeaderChip(
                        label = "Pend.", value = pendingInvoices.toString(), isCritical = true
                    )
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
        color = background, shape = RoundedCornerShape(12.dp), tonalElevation = 0.dp
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
private fun FilterSummaryTile(
    label: String, value: String, selected: Boolean, color: Color, onClick: () -> Unit
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            Box(
                modifier = Modifier.fillMaxWidth().height(96.dp)
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
    cashboxManager: CashBoxManager,
    onDismiss: () -> Unit,
    onActionSelected: (CustomerQuickActionType) -> Unit
) {
    val quickActions = remember { customerQuickActions() }

    ModalBottomSheet(
        onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = customer.customerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            CustomerOutstandingSummary(
                customer = customer,
                invoices = emptyList(),
                posBaseCurrency = paymentState.baseCurrency,
                cashboxManager = cashboxManager
            )

            HorizontalDivider()

            quickActions.forEach { action ->
                ListItem(
                    headlineContent = { Text(action.label) },
                    leadingContent = { Icon(action.icon, contentDescription = null) },
                    modifier = Modifier.clickable { onActionSelected(action.type) })
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
    outstandingInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
    paymentState: CustomerPaymentState,
    onDismiss: () -> Unit,
    onRegisterPayment: (
        invoiceId: String, modeOfPayment: String, enteredAmount: Double, enteredCurrency: String, referenceNumber: String
    ) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        CustomerOutstandingInvoicesContent(
            customer = customer,
            invoicesState = invoicesState,
            outstandingInvoicesPagingItems = outstandingInvoicesPagingItems,
            paymentState = paymentState,
            onRegisterPayment = onRegisterPayment,
            onDownloadInvoicePdf = onDownloadInvoicePdf,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CustomerOutstandingInvoicesContent(
    customer: CustomerBO,
    invoicesState: CustomerInvoicesState,
    outstandingInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
    paymentState: CustomerPaymentState,
    onRegisterPayment: (
        invoiceId: String, modeOfPayment: String, enteredAmount: Double, enteredCurrency: String, referenceNumber: String
    ) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val cashboxManager: CashBoxManager = koinInject()
    val scrollState = rememberScrollState()
    var selectedInvoice by remember { mutableStateOf<SalesInvoiceBO?>(null) }
    var amountRaw by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf(0.0) }
    var lastAutoAmount by remember { mutableStateOf<String?>(null) }
    val companyCurrency = normalizeCurrency(paymentState.baseCurrency)
    val invoiceCurrency = normalizeCurrency(selectedInvoice?.currency)
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

    val preferredCurrencyByMode = paymentState.paymentModeCurrencyByMode ?: mapOf()
    var selectedCurrency by remember { mutableStateOf(invoiceCurrency.ifBlank { companyCurrency }) }
    LaunchedEffect(selectedMode, invoiceCurrency, companyCurrency) {
        val resolved = resolvePaymentCurrencyForMode(
            modeOfPayment = selectedMode,
            paymentModeDetails = paymentState.modeTypes ?: mapOf(),
            preferredCurrency = preferredCurrencyByMode[selectedMode],
            invoiceCurrency = invoiceCurrency
        )
        selectedCurrency = resolved.ifBlank { companyCurrency }
    }

    var modeExpanded by remember { mutableStateOf(false) }

    val cachedRates = (invoicesState as? CustomerInvoicesState.Success)
        ?.exchangeRateByCurrency
        .orEmpty()
    var rateBaseToPos by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(
        selectedCurrency,
        companyCurrency,
        cachedRates,
        selectedInvoice?.conversionRate
    ) {
        rateBaseToPos = resolveBaseToSelectedRate(
            cashboxManager = cashboxManager,
            companyCurrency = companyCurrency,
            selectedCurrency = selectedCurrency,
            invoiceCurrency = invoiceCurrency,
            invoiceConversionRate = selectedInvoice?.conversionRate,
            cachedRates = cachedRates
        )
    }

    val invoiceConversionRate = selectedInvoice?.conversionRate?.takeIf { it > 0.0 }
    val receivableCurrency =
        normalizeCurrency(selectedInvoice?.partyAccountCurrency ?: companyCurrency)
    val outstandingRc = selectedInvoice?.outstandingAmount ?: 0.0
    val baseToPosRate = rateBaseToPos
    val outstandingInSelectedCurrency = when {
        selectedCurrency.equals(receivableCurrency, ignoreCase = true) -> outstandingRc
        selectedCurrency.equals(invoiceCurrency, ignoreCase = true) -> {
            CurrencyService.amountReceivableToInvoice(
                outstandingRc,
                invoiceConversionRate
            )
        }

        receivableCurrency.equals(companyCurrency, ignoreCase = true) && baseToPosRate != null ->
            outstandingRc * baseToPosRate

        else -> {
            cachedRates[selectedCurrency]?.let { outstandingRc * it }
        }
    }
    val conversionError = outstandingInSelectedCurrency == null
    LaunchedEffect(selectedInvoice?.invoiceId, selectedCurrency, outstandingInSelectedCurrency) {
        val resolvedOutstanding = outstandingInSelectedCurrency ?: return@LaunchedEffect
        if (amountRaw.isBlank() || amountRaw == lastAutoAmount) {
            val formatted = formatAmountRawForCurrency(resolvedOutstanding)
            amountRaw = formatted
        }
    }
    val changeDue =
        outstandingInSelectedCurrency?.let { (amountValue - it).coerceAtLeast(0.0) } ?: 0.0
    val amountToApply = outstandingInSelectedCurrency?.let { minOf(amountValue, it) } ?: amountValue
    val amountInCompanyCurrency = when {
        selectedCurrency.equals(companyCurrency, ignoreCase = true) -> amountValue
        baseToPosRate != null && baseToPosRate > 0.0 -> amountValue / baseToPosRate
        else -> null
    }
    val isSubmitEnabled =
        !paymentState.isSubmitting && selectedInvoice?.invoiceId?.isNotBlank() == true && selectedMode.isNotBlank() && amountValue > 0.0 && !conversionError

    Column(
        modifier = modifier.verticalScroll(scrollState),
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
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
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
                val refreshState = outstandingInvoicesPagingItems.loadState.refresh
                val appendState = outstandingInvoicesPagingItems.loadState.append
                val isLoading = refreshState is LoadState.Loading
                val hasError = refreshState is LoadState.Error || appendState is LoadState.Error
                if (hasError) {
                    Text(
                        text = (refreshState as? LoadState.Error)?.error?.message
                            ?: (appendState as? LoadState.Error)?.error?.message
                            ?: "No se pudieron cargar las facturas pendientes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isLoading && outstandingInvoicesPagingItems.itemCount == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                } else if (outstandingInvoicesPagingItems.itemCount == 0) {
                    Text(
                        text = strings.customer.emptyOsInvoices,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = outstandingInvoicesPagingItems.itemCount,
                            key = { index ->
                                outstandingInvoicesPagingItems[index]?.invoiceId ?: "outstanding_$index"
                            }
                        ) { index ->
                            val invoice = outstandingInvoicesPagingItems[index] ?: return@items
                            val isSelected = invoice.invoiceId == selectedInvoice?.invoiceId
                            val display = resolveInvoiceDisplayAmounts(
                                invoice = invoice,
                                companyCurrency = companyCurrency
                            )
                            val baseOutstanding = bd(display.outstandingCompany).toDouble(0)

                            val baseLabel = formatCurrency(display.companyCurrency, baseOutstanding)
                            val posLabel = formatCurrency(
                                display.invoiceCurrency, bd(display.outstandingInvoice).toDouble(0)
                            )

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
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedInvoice = invoice
                                        val amountToUse = if (selectedCurrency.equals(
                                                display.companyCurrency,
                                                ignoreCase = true
                                            )
                                        ) {
                                            display.outstandingCompany
                                        } else {
                                            display.outstandingInvoice
                                        }
                                        val formatted = formatAmountRawForCurrency(
                                            amountToUse,
                                        )
                                        amountRaw = formatted
                                        lastAutoAmount = formatted
                                    }.padding(12.dp),
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
                                                        label = { Text("Sincronizacion pendiente") })
                                                }

                                                "Failed" -> {
                                                    AssistChip(
                                                        onClick = {},
                                                        label = { Text("Sincronizacion falló") },
                                                        colors = AssistChipDefaults.assistChipColors(
                                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                                                        )
                                                    )
                                                }
                                            }
                                            RadioButton(
                                                selected = isSelected, onClick = {
                                                    selectedInvoice = invoice
                                                    val amountToUse = if (selectedCurrency.equals(
                                                            display.companyCurrency,
                                                            ignoreCase = true
                                                        )
                                                    ) {
                                                        display.outstandingCompany
                                                    } else {
                                                        display.outstandingInvoice
                                                    }
                                                    val formatted = formatAmountRawForCurrency(
                                                        amountToUse,
                                                    )
                                                    amountRaw = formatted
                                                    lastAutoAmount = formatted
                                                })
                                        }
                                    }

                                    Text(
                                        text = "${strings.customer.outstandingLabel}: $posLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${strings.customer.baseCurrency}: $baseLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    var downloadMenuExpanded by remember(invoice.invoiceId) { mutableStateOf(false) }
                                    Box {
                                        TextButton(
                                            onClick = { downloadMenuExpanded = true },
                                            enabled = invoice.invoiceId.isNotBlank()
                                        ) {
                                            Text("Descargar PDF")
                                        }
                                        DropdownMenu(
                                            expanded = downloadMenuExpanded,
                                            onDismissRequest = { downloadMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Abrir ahora") },
                                                onClick = {
                                                    downloadMenuExpanded = false
                                                    onDownloadInvoicePdf(invoice.invoiceId, InvoicePdfActionOption.OPEN_NOW)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Guardar en...") },
                                                onClick = {
                                                    downloadMenuExpanded = false
                                                    onDownloadInvoicePdf(invoice.invoiceId, InvoicePdfActionOption.SAVE_AS)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Compartir") },
                                                onClick = {
                                                    downloadMenuExpanded = false
                                                    onDownloadInvoicePdf(invoice.invoiceId, InvoicePdfActionOption.SHARE)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (outstandingInvoicesPagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) { CircularProgressIndicator() }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ====== REGISTRAR PAGO ======
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.customer.registerPaymentTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                ExposedDropdownMenuBox(
                    expanded = modeExpanded, onExpandedChange = { modeExpanded = !modeExpanded }) {
                    AppTextField(
                        value = selectedMode,
                        onValueChange = {},
                        label = strings.customer.selectPaymentMode,
                        placeholder = strings.customer.selectPaymentMode,
                        readOnly = true,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded)
                        })
                    ExposedDropdownMenu(
                        expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        paymentModes.forEach { mode ->
                            DropdownMenuItem(text = { Text(mode.name) }, onClick = {
                                selectedMode = mode.name
                                selectedCurrency = resolvePaymentCurrencyForMode(
                                    modeOfPayment = mode.modeOfPayment,
                                    paymentModeDetails = paymentState.modeTypes ?: mapOf(),
                                    preferredCurrency = preferredCurrencyByMode[mode.name],
                                    invoiceCurrency = invoiceCurrency
                                )
                                modeExpanded = false
                            })
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
                                Icons.Default.ConfirmationNumber, contentDescription = null
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
                }

                MoneyTextField(
                    currencyCode = selectedCurrency,
                    rawValue = amountRaw,
                    onRawValueChange = {
                        amountRaw = it
                        lastAutoAmount = null
                    },
                    label = strings.customer.amountLabel,
                    onAmountChanged = { amountValue = it },
                    supportingText = {
                        if (conversionError) {
                            Text(
                                text = "Tasa de cambio no encontrada de $selectedCurrency a $companyCurrency.",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (!selectedCurrency.equals(companyCurrency, ignoreCase = true)) {
                            Text(
                                "Valor en ${companyCurrency.toCurrencySymbol()}: ${
                                    amountInCompanyCurrency?.let {
                                        formatCurrency(companyCurrency, it)
                                    } ?: "—"
                                }"
                            )
                        }
                    })
//                Text(
//                    text = "${strings.customer.outstandingLabel}: $posOutstandingLabel",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Text(
//                    text = "${strings.customer.baseCurrency}: $baseOutstandingLabel",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                ) if (changeDue > 0.0) {
                if (changeDue > 0.0) {
                    Text(
                        text = "Cambio: ${formatCurrency(selectedCurrency, changeDue)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                //}
            }

            Button(
                onClick = {
                    val invoiceId = selectedInvoice?.invoiceId?.trim().orEmpty()
                    onRegisterPayment(
                        invoiceId, selectedMode, amountToApply, selectedCurrency, referenceInput
                    )
                }, enabled = isSubmitEnabled, modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (paymentState.isSubmitting) strings.customer.processing
                    else strings.customer.registerPaymentButton
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerInvoiceHistorySheet(
    customer: CustomerBO,
    historyState: CustomerInvoiceHistoryState,
    historyInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
    historyMessage: String?,
    historyBusy: Boolean,
    paymentState: CustomerPaymentState,
    posBaseCurrency: String,
    cashboxManager: CashBoxManager,
    returnPolicy: ReturnPolicySettings,
    onAction: (String, InvoiceCancellationAction, String?, String?, String?, Boolean) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
    onDismiss: () -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments? = { null },
    onSubmitPartialReturn: (
        invoiceId: String, reason: String?, refundModeOfPayment: String?, refundReferenceNo: String?, applyRefund: Boolean, itemsToReturnByCode: Map<String, Double>
    ) -> Unit = { _, _, _, _, _, _ -> }
) {
    var returnInvoiceId by remember { mutableStateOf<String?>(null) }
    var returnInvoiceLocal by remember { mutableStateOf<SalesInvoiceWithItemsAndPayments?>(null) }
    var returnLoading by remember { mutableStateOf(false) }
    var returnError by remember { mutableStateOf<String?>(null) }
    var showReturnDialog by remember { mutableStateOf(false) }

    var refundMode by remember { mutableStateOf<String?>(null) }
    var refundReference by remember { mutableStateOf("") }
    var returnReason by remember { mutableStateOf("") }
    var qtyByItemCode by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var returnDestination by remember { mutableStateOf(defaultReturnDestination(returnPolicy)) }

    var showFullReturnDialog by remember { mutableStateOf(false) }
    var fullReturnInvoiceId by remember { mutableStateOf<String?>(null) }
    var fullReturnInvoice by remember { mutableStateOf<SalesInvoiceBO?>(null) }
    var fullRefundMode by remember { mutableStateOf<String?>(null) }
    var fullRefundReference by remember { mutableStateOf("") }
    var fullReturnReason by remember { mutableStateOf("") }
    var fullReturnDestination by remember { mutableStateOf(defaultReturnDestination(returnPolicy)) }

    val refundOptions = remember(paymentState.paymentModes) {
        paymentState.paymentModes
            .filter { it.allowInReturns }
            .mapNotNull { it.modeOfPayment.ifBlank { null } }
            .distinct()
    }

    fun canConfirmReturn(): Boolean = qtyByItemCode.values.any { it > 0.0 }

    fun closeFullReturnDialog() {
        showFullReturnDialog = false
        fullReturnInvoiceId = null
        fullReturnInvoice = null
        fullRefundMode = null
        fullRefundReference = ""
        fullReturnReason = ""
        fullReturnDestination = defaultReturnDestination(returnPolicy)
    }

    fun closeReturnDialog() {
        showReturnDialog = false
        returnInvoiceId = null
        returnInvoiceLocal = null
        returnError = null
        refundMode = null
        refundReference = ""
        returnReason = ""
        qtyByItemCode = emptyMap()
        returnDestination = defaultReturnDestination(returnPolicy)
    }

    if (showReturnDialog && returnInvoiceId != null) {
        val invoiceCurrency = normalizeCurrency(returnInvoiceLocal?.invoice?.currency)
        val returnTotal = remember(returnInvoiceLocal, qtyByItemCode) {
            returnInvoiceLocal?.let { local ->
                local.items.sumOf { item ->
                    val perUnit = if (item.qty != 0.0) item.amount / item.qty else item.rate
                    val qty = qtyByItemCode[item.itemCode] ?: 0.0
                    kotlin.math.abs(perUnit) * qty.coerceAtLeast(0.0)
                }
            } ?: 0.0
        }
        val outstanding = returnInvoiceLocal?.invoice?.outstandingAmount
        val projectedOutstanding =
            outstanding?.let { (it - returnTotal).coerceAtLeast(0.0) }
        val refundAllowed = returnPolicy.allowRefunds
        val effectiveDestination =
            if (refundAllowed) returnDestination else ReturnDestination.CREDIT
        val refundEnabled = refundAllowed && effectiveDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(refundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && refundMode.isNullOrBlank()
        val missingReference = needsReference && refundReference.isBlank()
        val missingReason = returnPolicy.requireReason && returnReason.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)
        val canConfirm = canConfirmRefund && !missingReason

        AlertDialog(onDismissRequest = {
            if (!historyBusy) {
                closeReturnDialog()
            }
        }, title = { Text("Retorno parcial") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Factura ${returnInvoiceId!!}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isPos = returnInvoiceLocal?.invoice?.isPos == true
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isPos) "POS" else "Crédito",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val statusLabel = returnInvoiceLocal?.invoice?.status?.ifBlank { null }
                                ?: "Sin estado"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = statusLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val postingDate = returnInvoiceLocal?.invoice?.postingDate
                        if (!postingDate.isNullOrBlank()) {
                            Text(
                                postingDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val baseCurrency = normalizeCurrency(posBaseCurrency)
                        val invoiceCurrency =
                            normalizeCurrency(returnInvoiceLocal?.invoice?.currency)
                        val total = returnInvoiceLocal?.invoice?.grandTotal ?: 0.0
                        val paid = returnInvoiceLocal?.invoice?.paidAmount ?: 0.0
                        val outstanding = returnInvoiceLocal?.invoice?.outstandingAmount ?: 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total")
                            Text(formatCurrency(invoiceCurrency, total))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pagado")
                            Text(formatCurrency(invoiceCurrency, paid))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pendiente")
                            Text(formatCurrency(invoiceCurrency, outstanding))
                        }
                        val rate = returnInvoiceLocal?.invoice?.conversionRate ?: 0.0
                        if (!invoiceCurrency.equals(baseCurrency, true) && rate > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tipo de cambio")
                                Text(
                                    "1 $invoiceCurrency = ${
                                        formatDoubleToString(
                                            rate,
                                            4
                                        )
                                    } $baseCurrency"
                                )
                            }
                        }
                    }
                }

                if (returnLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "Cargando detalle local...", style = MaterialTheme.typography.bodySmall
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

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Destino del monto devuelto",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (refundAllowed) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ReturnDestination.entries.forEach { destination ->
                                    FilterChip(
                                        selected = returnDestination == destination,
                                        onClick = {
                                            returnDestination = destination
                                        },
                                        label = { Text(destination.label) })
                                }
                            }
                        } else {
                            Text(
                                "Reembolsos deshabilitados; se aplicará crédito a favor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (refundEnabled && refundOptions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Reembolso",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            ExposedDropdownMenuBox(
                                expanded = refundModeExpanded,
                                onExpandedChange = { refundModeExpanded = !refundModeExpanded }) {
                                OutlinedTextField(
                                    value = refundMode ?: "",
                                    onValueChange = { },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                                    })
                                ExposedDropdownMenu(
                                    expanded = refundModeExpanded,
                                    onDismissRequest = { refundModeExpanded = false }) {
                                    refundOptions.forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = {
                                            refundMode = option
                                            refundModeExpanded = false
                                        })
                                    }
                                }
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

                ReturnAccountingSummary(
                    returnTotal = returnTotal,
                    currency = invoiceCurrency,
                    refundEnabled = refundEnabled,
                    creditEnabled = effectiveDestination == ReturnDestination.CREDIT,
                    projectedOutstanding = projectedOutstanding
                )

                if (returnPolicy.requireReason) {
                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Motivo (requerido)") },
                        singleLine = false,
                        minLines = 2,
                        enabled = !historyBusy
                    )
                }

                Text(
                    "Selecciona cantidades a devolver:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                returnInvoiceLocal?.let { local ->
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(local.items, key = { it.itemCode }) { item ->
                            val soldQty = item.qty
                            val current = qtyByItemCode[item.itemCode] ?: 0.0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.45f
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                    Text(
                                        text = item.itemName ?: item.itemCode,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Vendidos: ${formatDoubleToString(soldQty, 2)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Precio: ${formatDoubleToString(item.rate, 2)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Devolver", style = MaterialTheme.typography.bodySmall)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val next = (current - 1.0).coerceAtLeast(0.0)
                                                    qtyByItemCode =
                                                        qtyByItemCode.toMutableMap().apply {
                                                            put(item.itemCode, next)
                                                        }
                                                }, enabled = !historyBusy && current > 0.0
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
                                                    val next = (current + 1.0).coerceAtMost(soldQty)
                                                    qtyByItemCode =
                                                        qtyByItemCode.toMutableMap().apply {
                                                            put(item.itemCode, next)
                                                        }
                                                }, enabled = !historyBusy && current < soldQty
                                            ) {
                                                Icon(Icons.Default.Add, null)
                                            }
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
                    if (missingReason) {
                        Text(
                            "Debes indicar el motivo del retorno.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

            }
        }, confirmButton = {
            Button(
                enabled = !historyBusy && !returnLoading && returnInvoiceLocal != null && canConfirmReturn() && canConfirm,
                onClick = {
                    val invoiceId = returnInvoiceId ?: return@Button
                    val resolvedReason =
                        if (returnPolicy.requireReason) returnReason.takeIf { it.isNotBlank() } else null
                    onSubmitPartialReturn(
                        invoiceId,
                        resolvedReason,
                        refundMode?.takeIf { it.isNotBlank() },
                        refundReference.takeIf { it.isNotBlank() },
                        effectiveDestination == ReturnDestination.RETURN,
                        qtyByItemCode.filterValues { it > 0.0 })
                    closeReturnDialog()
                }) { Text("Confirmar retorno") }
        }, dismissButton = {
            OutlinedButton(
                enabled = !historyBusy, onClick = { closeReturnDialog() }) { Text("Cerrar") }
        })
    }

    if (showFullReturnDialog && fullReturnInvoiceId != null) {
        val invoiceCurrency = normalizeCurrency(fullReturnInvoice?.currency)
        val returnTotal = fullReturnInvoice?.total ?: 0.0
        val projectedOutstanding =
            fullReturnInvoice?.outstandingAmount?.let { (it - returnTotal).coerceAtLeast(0.0) }
        val refundAllowed = returnPolicy.allowRefunds
        val effectiveDestination =
            if (refundAllowed) fullReturnDestination else ReturnDestination.CREDIT
        val refundEnabled = refundAllowed && effectiveDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(fullRefundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && fullRefundMode.isNullOrBlank()
        val missingReference = needsReference && fullRefundReference.isBlank()
        val missingReason = returnPolicy.requireReason && fullReturnReason.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)
        val canConfirm = canConfirmRefund && !missingReason

        AlertDialog(onDismissRequest = {
            if (!historyBusy) {
                closeFullReturnDialog()
            }
        }, title = { Text("Retorno total") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Factura ${fullReturnInvoiceId!!}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isPos = fullReturnInvoice?.isPos == true
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isPos) "POS" else "Crédito",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val statusLabel =
                                fullReturnInvoice?.status?.ifBlank { null } ?: "Sin estado"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = statusLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val postingDate = fullReturnInvoice?.postingDate
                        if (!postingDate.isNullOrBlank()) {
                            Text(
                                postingDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val baseCurrency = normalizeCurrency(posBaseCurrency)
                        val invoiceCurrency = normalizeCurrency(fullReturnInvoice?.currency)
                        val total = fullReturnInvoice?.total ?: 0.0
                        val paid = fullReturnInvoice?.paidAmount ?: 0.0
                        val outstanding = fullReturnInvoice?.outstandingAmount ?: 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total")
                            Text(formatCurrency(invoiceCurrency, total))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pagado")
                            Text(formatCurrency(invoiceCurrency, paid))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pendiente")
                            Text(formatCurrency(invoiceCurrency, outstanding))
                        }
                        val rate = fullReturnInvoice?.conversionRate ?: 0.0
                        if (!invoiceCurrency.equals(baseCurrency, true) && rate > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tipo de cambio")
                                Text(
                                    "1 $invoiceCurrency = ${
                                        formatDoubleToString(
                                            rate,
                                            4
                                        )
                                    } $baseCurrency"
                                )
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Destino del monto devuelto",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (refundAllowed) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ReturnDestination.entries.forEach { destination ->
                                    FilterChip(
                                        selected = fullReturnDestination == destination,
                                        onClick = {
                                            fullReturnDestination = destination
                                        },
                                        label = { Text(destination.label) })
                                }
                            }
                        } else {
                            Text(
                                "Reembolsos deshabilitados; se aplicará crédito a favor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                var refundModeExpanded by remember { mutableStateOf(false) }
                if (refundEnabled && refundOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = refundModeExpanded,
                        onExpandedChange = { refundModeExpanded = !refundModeExpanded }) {
                        OutlinedTextField(
                            value = fullRefundMode ?: "",
                            onValueChange = { },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                            })
                        ExposedDropdownMenu(
                            expanded = refundModeExpanded,
                            onDismissRequest = { refundModeExpanded = false }) {
                            refundOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    fullRefundMode = option
                                    refundModeExpanded = false
                                })
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

                ReturnAccountingSummary(
                    returnTotal = returnTotal,
                    currency = invoiceCurrency,
                    refundEnabled = refundEnabled,
                    creditEnabled = effectiveDestination == ReturnDestination.CREDIT,
                    projectedOutstanding = projectedOutstanding
                )

                if (returnPolicy.requireReason) {
                    OutlinedTextField(
                        value = fullReturnReason,
                        onValueChange = { fullReturnReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Motivo (requerido)") },
                        singleLine = false,
                        minLines = 2,
                        enabled = !historyBusy
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
                if (missingReason) {
                    Text(
                        "Debes indicar el motivo del retorno.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }, confirmButton = {
            Button(
                enabled = !historyBusy && canConfirm, onClick = {
                    val invoiceId = fullReturnInvoiceId ?: return@Button
                    val resolvedReason =
                        if (returnPolicy.requireReason) fullReturnReason.takeIf { it.isNotBlank() } else null
                    onAction(
                        invoiceId,
                        InvoiceCancellationAction.RETURN,
                        resolvedReason,
                        fullRefundMode?.takeIf { it.isNotBlank() },
                        fullRefundReference.takeIf { it.isNotBlank() },
                        effectiveDestination == ReturnDestination.RETURN
                    )
                    closeFullReturnDialog()
                }) { Text("Confirmar retorno") }
        }, dismissButton = {
            OutlinedButton(
                enabled = !historyBusy, onClick = { closeFullReturnDialog() }) { Text("Cerrar") }
        })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        CustomerInvoiceHistoryContent(
            customer = customer,
            historyState = historyState,
            historyInvoicesPagingItems = historyInvoicesPagingItems,
            historyMessage = historyMessage,
            historyBusy = historyBusy,
            paymentState = paymentState,
            posBaseCurrency = posBaseCurrency,
            cashboxManager = cashboxManager,
            returnPolicy = returnPolicy,
            onAction = onAction,
            onDownloadInvoicePdf = onDownloadInvoicePdf,
            loadLocalInvoice = loadLocalInvoice,
            onSubmitPartialReturn = onSubmitPartialReturn,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CustomerInvoiceHistoryContent(
    customer: CustomerBO,
    historyState: CustomerInvoiceHistoryState,
    historyInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
    historyMessage: String?,
    historyBusy: Boolean,
    paymentState: CustomerPaymentState,
    posBaseCurrency: String,
    cashboxManager: CashBoxManager,
    returnPolicy: ReturnPolicySettings,
    onAction: (String, InvoiceCancellationAction, String?, String?, String?, Boolean) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments? = { null },
    onSubmitPartialReturn: (
        invoiceId: String, reason: String?, refundModeOfPayment: String?, refundReferenceNo: String?, applyRefund: Boolean, itemsToReturnByCode: Map<String, Double>
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
    var returnReason by remember { mutableStateOf("") }
    var qtyByItemCode by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var returnDestination by remember { mutableStateOf(defaultReturnDestination(returnPolicy)) }
    var destinationTouched by remember { mutableStateOf(false) }

    var showFullReturnDialog by remember { mutableStateOf(false) }
    var fullReturnInvoiceId by remember { mutableStateOf<String?>(null) }
    var fullReturnInvoice by remember { mutableStateOf<SalesInvoiceBO?>(null) }
    var fullRefundMode by remember { mutableStateOf<String?>(null) }
    var fullRefundReference by remember { mutableStateOf("") }
    var fullReturnReason by remember { mutableStateOf("") }
    var fullReturnDestination by remember { mutableStateOf(defaultReturnDestination(returnPolicy)) }
    var fullDestinationTouched by remember { mutableStateOf(false) }

    val refundOptions = remember(paymentState.paymentModes) {
        paymentState.paymentModes
            .filter { it.allowInReturns }
            .mapNotNull { it.modeOfPayment.ifBlank { null } }
            .distinct()
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
        returnReason = ""
        returnDestination = defaultReturnDestination(returnPolicy)
        destinationTouched = false

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
        fullReturnInvoice = historyState.let { state ->
            if (state is CustomerInvoiceHistoryState.Success) {
                state.invoices.firstOrNull { it.invoiceId == invoiceId }
            } else {
                null
            }
        }
        fullRefundMode = refundOptions.firstOrNull()
        fullRefundReference = ""
        fullReturnReason = ""
        fullReturnDestination = defaultReturnDestination(returnPolicy)
        fullDestinationTouched = false
    }

    fun closeFullReturnDialog() {
        showFullReturnDialog = false
        fullReturnInvoiceId = null
        fullReturnInvoice = null
        fullRefundMode = null
        fullRefundReference = ""
        fullReturnReason = ""
        fullReturnDestination = defaultReturnDestination(returnPolicy)
        fullDestinationTouched = false
    }

    fun closeReturnDialog() {
        showReturnDialog = false
        returnInvoiceId = null
        returnInvoiceLocal = null
        returnError = null
        refundMode = null
        refundReference = ""
        qtyByItemCode = emptyMap()
        returnReason = ""
        returnDestination = defaultReturnDestination(returnPolicy)
        destinationTouched = false
    }

    LaunchedEffect(showReturnDialog, returnPolicy, destinationTouched) {
        if (showReturnDialog && !destinationTouched) {
            returnDestination = defaultReturnDestination(returnPolicy)
        }
    }

    LaunchedEffect(showFullReturnDialog, returnPolicy, fullDestinationTouched) {
        if (showFullReturnDialog && !fullDestinationTouched) {
            fullReturnDestination = defaultReturnDestination(returnPolicy)
        }
    }

    if (showDialogs && showReturnDialog && returnInvoiceId != null) {
        val refundAllowed = returnPolicy.allowRefunds
        val effectiveDestination =
            if (refundAllowed) returnDestination else ReturnDestination.CREDIT
        val refundEnabled = refundAllowed && effectiveDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(refundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && refundMode.isNullOrBlank()
        val missingReference = needsReference && refundReference.isBlank()
        val missingReason = returnPolicy.requireReason && returnReason.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)
        val canConfirm = canConfirmRefund && !missingReason

        AlertDialog(onDismissRequest = {
            if (!historyBusy) {
                closeReturnDialog()
            }
        }, title = { Text("Retorno parcial") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Factura: ${returnInvoiceId!!}")

                if (returnLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "Cargando detalle local...", style = MaterialTheme.typography.bodySmall
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
                if (refundAllowed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReturnDestination.entries.forEach { destination ->
                            FilterChip(
                                selected = returnDestination == destination,
                                onClick = {
                                    returnDestination = destination
                                    destinationTouched = true
                                },
                                label = { Text(destination.label) })
                        }
                    }
                } else {
                    Text(
                        "Reembolsos deshabilitados; se aplicará crédito a favor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))

                if (refundEnabled && refundOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = refundModeExpanded,
                        onExpandedChange = { refundModeExpanded = !refundModeExpanded }) {
                        OutlinedTextField(
                            value = refundMode ?: "",
                            onValueChange = { },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                            })
                        ExposedDropdownMenu(
                            expanded = refundModeExpanded,
                            onDismissRequest = { refundModeExpanded = false }) {
                            refundOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    refundMode = option
                                    refundModeExpanded = false
                                })
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

                if (returnPolicy.requireReason) {
                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Motivo (requerido)") },
                        singleLine = false,
                        minLines = 2,
                        enabled = !historyBusy
                    )
                }

                Text(
                    "Selecciona cantidades a devolver:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                returnInvoiceLocal?.let { local ->
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(local.items, key = { it.itemCode }) { item ->
                            val soldQty = item.qty
                            val current = qtyByItemCode[item.itemCode] ?: 0.0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.45f
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
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
                                                    soldQty, 2
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
                                                val next = (current - 1.0).coerceAtLeast(0.0)
                                                qtyByItemCode = qtyByItemCode.toMutableMap().apply {
                                                    put(item.itemCode, next)
                                                }
                                            }, enabled = !historyBusy && current > 0.0
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
                                                val next = (current + 1.0).coerceAtMost(soldQty)
                                                qtyByItemCode = qtyByItemCode.toMutableMap().apply {
                                                    put(item.itemCode, next)
                                                }
                                            }, enabled = !historyBusy && current < soldQty
                                        ) {
                                            Icon(Icons.Default.Add, null)
                                        }
                                    }
                                }
                                val perUnit =
                                    if (item.qty != 0.0) item.amount / item.qty else item.rate
                                val lineTotal = kotlin.math.abs(perUnit) * current
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Subtotal devolución",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatCurrency(local.invoice.currency, lineTotal),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                    if (missingReason) {
                        Text(
                            "Debes indicar el motivo del retorno.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }, confirmButton = {
            TextButton(
                onClick = {
                    if (!historyBusy && returnInvoiceId != null) {
                        val resolvedReason =
                            if (returnPolicy.requireReason) returnReason.takeIf { it.isNotBlank() } else null
                        onSubmitPartialReturn(
                            returnInvoiceId!!,
                            resolvedReason,
                            if (refundEnabled) refundMode else null,
                            if (refundEnabled) refundReference else null,
                            effectiveDestination == ReturnDestination.RETURN,
                            qtyByItemCode
                        )
                        closeReturnDialog()
                    }
                }, enabled = !historyBusy && canConfirmReturn() && canConfirm
            ) {
                Text("Confirmar")
            }
        }, dismissButton = {
            TextButton(
                onClick = {
                    if (!historyBusy) {
                        closeReturnDialog()
                    }
                }) {
                Text("Cancelar")
            }
        })
    }

    if (showDialogs && showFullReturnDialog && fullReturnInvoiceId != null) {
        val invoiceCurrency = normalizeCurrency(fullReturnInvoice?.currency)
        val returnTotal = fullReturnInvoice?.total ?: 0.0
        val projectedOutstanding =
            fullReturnInvoice?.outstandingAmount?.let { (it - returnTotal).coerceAtLeast(0.0) }
        val refundAllowed = returnPolicy.allowRefunds
        val effectiveDestination =
            if (refundAllowed) fullReturnDestination else ReturnDestination.CREDIT
        val refundEnabled = refundAllowed && effectiveDestination == ReturnDestination.RETURN
        val selectedMode = paymentState.paymentModes.firstOrNull {
            it.modeOfPayment.equals(fullRefundMode, true)
        }
        val needsReference = refundEnabled && requiresReference(selectedMode)
        val missingRefundMode = refundEnabled && fullRefundMode.isNullOrBlank()
        val missingReference = needsReference && fullRefundReference.isBlank()
        val missingReason = returnPolicy.requireReason && fullReturnReason.isBlank()
        val hasRefundOptions = refundOptions.isNotEmpty()
        val canConfirmRefund =
            !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)
        val canConfirm = canConfirmRefund && !missingReason

        AlertDialog(onDismissRequest = {
            if (!historyBusy) {
                closeFullReturnDialog()
            }
        }, title = { Text("Retorno total") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Factura: ${fullReturnInvoiceId!!}")

                Text(
                    text = "Destino del monto devuelto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (refundAllowed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReturnDestination.entries.forEach { destination ->
                            FilterChip(
                                selected = fullReturnDestination == destination,
                                onClick = {
                                    fullReturnDestination = destination
                                    fullDestinationTouched = true
                                },
                                label = { Text(destination.label) })
                        }
                    }
                } else {
                    Text(
                        "Reembolsos deshabilitados; se aplicará crédito a favor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))

                if (refundEnabled && refundOptions.isNotEmpty()) {
                    var fullRefundModeExpanded by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Reembolso",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            ExposedDropdownMenuBox(
                                expanded = fullRefundModeExpanded,
                                onExpandedChange = { fullRefundModeExpanded = !fullRefundModeExpanded }) {
                                OutlinedTextField(
                                    value = fullRefundMode ?: "",
                                    onValueChange = { },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                                    })
                                ExposedDropdownMenu(
                                    expanded = fullRefundModeExpanded,
                                    onDismissRequest = { fullRefundModeExpanded = false }) {
                                    refundOptions.forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = {
                                            fullRefundMode = option
                                            fullRefundModeExpanded = false
                                        })
                                    }
                                }
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

                ReturnAccountingSummary(
                    returnTotal = returnTotal,
                    currency = invoiceCurrency,
                    refundEnabled = refundEnabled,
                    creditEnabled = effectiveDestination == ReturnDestination.CREDIT,
                    projectedOutstanding = projectedOutstanding
                )

                if (returnPolicy.requireReason) {
                    OutlinedTextField(
                        value = fullReturnReason,
                        onValueChange = { fullReturnReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Motivo (requerido)") },
                        singleLine = false,
                        minLines = 2,
                        enabled = !historyBusy
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
                if (missingReason) {
                    Text(
                        "Debes indicar el motivo del retorno.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }, confirmButton = {
            Button(
                enabled = !historyBusy && canConfirm, onClick = {
                    val invoiceId = fullReturnInvoiceId ?: return@Button
                    val resolvedReason =
                        if (returnPolicy.requireReason) fullReturnReason.takeIf { it.isNotBlank() } else null
                    onAction(
                        invoiceId,
                        InvoiceCancellationAction.RETURN,
                        resolvedReason,
                        fullRefundMode?.takeIf { it.isNotBlank() },
                        fullRefundReference.takeIf { it.isNotBlank() },
                        effectiveDestination == ReturnDestination.RETURN
                    )
                    closeFullReturnDialog()
                }) { Text("Confirmar retorno") }
        }, dismissButton = {
            OutlinedButton(
                enabled = !historyBusy, onClick = { closeFullReturnDialog() }) { Text("Cerrar") }
        })
    }

    Column(
        modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape
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
                        val count = historyInvoicesPagingItems.itemSnapshotList.items.count {
                            isWithinDays(it.postingDate, selectedRangeDays)
                        }
                        "$count facturas en $selectedRangeDays días"
                    }

                    CustomerInvoiceHistoryState.Loading -> "Cargando historial..."
                    is CustomerInvoiceHistoryState.Error -> "Historial no disponible"
                    else -> "Historial de facturas"
                }
                Row {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " - Total pendiente: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryRangeChip(
                label = "7 días",
                selected = selectedRangeDays == 7,
                onClick = { selectedRangeDays = 7 })
            HistoryRangeChip(
                label = "30 días",
                selected = selectedRangeDays == 30,
                onClick = { selectedRangeDays = 30 })
            HistoryRangeChip(
                label = "90 días",
                selected = selectedRangeDays == 90,
                onClick = { selectedRangeDays = 90 })
        }
        if (historyState is CustomerInvoiceHistoryState.Success) {
            val invoices = historyInvoicesPagingItems.itemSnapshotList.items.filter {
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
                    label = "Pagadas", value = paidCount.toString(), modifier = Modifier.weight(1f)
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
                val refreshState = historyInvoicesPagingItems.loadState.refresh
                val appendState = historyInvoicesPagingItems.loadState.append
                val isLoading = refreshState is LoadState.Loading
                val hasError = refreshState is LoadState.Error || appendState is LoadState.Error
                val invoices = historyInvoicesPagingItems.itemSnapshotList.items.filter {
                    isWithinDays(it.postingDate, selectedRangeDays)
                }
                if (hasError) {
                    Text(
                        (refreshState as? LoadState.Error)?.error?.message
                            ?: (appendState as? LoadState.Error)?.error?.message
                            ?: "No se pudo cargar el historial de facturas.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (isLoading && historyInvoicesPagingItems.itemCount == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Cargando historial...")
                    }
                } else if (historyInvoicesPagingItems.itemCount == 0 || invoices.isEmpty()) {
                    Text("No se encontraron facturas en los últimos $selectedRangeDays días.")
                } else {
                    InvoiceHistorySummary(
                        invoices = invoices,
                        posBaseCurrency = posBaseCurrency,
                        cashboxManager = cashboxManager
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            count = historyInvoicesPagingItems.itemCount,
                            key = { index ->
                                historyInvoicesPagingItems[index]?.invoiceId ?: "history_$index"
                            }
                        ) { index ->
                            val invoice = historyInvoicesPagingItems[index] ?: return@items
                            if (!isWithinDays(invoice.postingDate, selectedRangeDays)) return@items
                            InvoiceHistoryRow(
                                invoice = invoice,
                                isBusy = historyBusy,
                                posBaseCurrency = posBaseCurrency,
                                cashboxManager = cashboxManager,
                                returnPolicy = returnPolicy,
                                onCancel = { invoiceId ->
                                    onAction(
                                        invoiceId,
                                        InvoiceCancellationAction.CANCEL,
                                        null,
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
                                },
                                onDownloadPdf = onDownloadInvoicePdf
                            )
                        }
                        if (historyInvoicesPagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) { CircularProgressIndicator() }
                            }
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
    label: String, value: String, modifier: Modifier = Modifier
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
private fun SummaryStatChip(
    label: String, value: String, modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
private fun HeaderChip(
    label: String, value: String, isCritical: Boolean, modifier: Modifier = Modifier
) {
    val background = if (isCritical) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isCritical) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier, color = background, shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
private fun HistoryRangeChip(
    label: String, selected: Boolean, onClick: () -> Unit
) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun InvoiceHistorySummary(
    invoices: List<SalesInvoiceBO>, posBaseCurrency: String, cashboxManager: CashBoxManager
) {
    val companyCurrency = normalizeCurrency(posBaseCurrency)
    val invoiceCurrency = normalizeCurrency(invoices.firstOrNull()?.currency)
    val totalBase = invoices.sumOf {
        resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingCompany
    }
    val totalPos = invoices.sumOf {
        resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingInvoice
    }

    if (totalBase > 0.0 || totalPos > 0.0) {
        Column(
            modifier = Modifier.fillMaxWidth().background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                RoundedCornerShape(12.dp)
            ).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (invoiceCurrency.isNotBlank()) {
                    Text(invoiceCurrency, style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatCurrency(invoiceCurrency, totalPos),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(companyCurrency, style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatCurrency(companyCurrency, totalBase),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(companyCurrency, style = MaterialTheme.typography.labelSmall)
                Text(
                    formatCurrency(companyCurrency, totalBase),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// TODO: Localizar los estados de las facturas
@Composable
private fun normalizedStatus(status: String?): String {
    val strings = LocalAppStrings.current.invoice
    return when (status) {
        "draft" -> strings.draft
        "unpaid" -> strings.unpaid
        "paid" -> strings.paid
        "partly paid" -> strings.partlyPaid
        "canceled" -> strings.canceled
        "credit note" -> strings.creditNote
        "return" -> strings.returned
        else -> strings.draft
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun InvoiceHistoryRow(
    invoice: SalesInvoiceBO,
    isBusy: Boolean,
    posBaseCurrency: String,
    cashboxManager: CashBoxManager,
    returnPolicy: ReturnPolicySettings,
    onCancel: (String) -> Unit,
    onReturnTotal: (String) -> Unit,
    onPartialReturn: (String) -> Unit = {},
    onDownloadPdf: (String, InvoicePdfActionOption) -> Unit = { _, _ -> }
) {
    val display = resolveInvoiceDisplayAmounts(
        invoice = invoice,
        companyCurrency = normalizeCurrency(posBaseCurrency)
    )
    val baseTotal = bd(display.totalCompany).toDouble(0)
    val baseOutstanding = bd(display.outstandingCompany).toDouble(0)
    val posTotal = bd(display.totalInvoice).toDouble(0)
    val posOutstanding = bd(display.outstandingInvoice).toDouble(0)
    val statusLabel = invoice.status ?: "Sin estado"
    val statusKey = invoice.status?.trim()?.lowercase()
    val localizedStatus = normalizedStatus(statusKey)
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
    val isDraftOrUnpaid = statusKey in unpaidStatuses
    val isPaidOrPartly = statusKey in paidStatuses
    val allowCancel = isDraftOrUnpaid && !hasPayments
    val allowReturn = isPaidOrPartly || hasPayments
    val allowFullReturn = allowReturn && returnPolicy.allowFullReturns
    val allowPartialReturn = allowReturn && returnPolicy.allowPartialReturns
    val (statusBg, statusText) = when (statusLabel.lowercase()) {
        "paid" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "partly paid" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        "overdue" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        "unpaid", "draft" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "cancelled" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp
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
                        color = statusBg, shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = localizedStatus,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusText
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrency(display.invoiceCurrency, posTotal),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        formatCurrency(display.companyCurrency, baseTotal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrency(display.invoiceCurrency, posOutstanding),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (invoice.outstandingAmount > 0.0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formatCurrency(display.companyCurrency, baseOutstanding),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                var downloadMenuExpanded by remember(invoice.invoiceId) { mutableStateOf(false) }
                Box {
                    TextButton(
                        onClick = { downloadMenuExpanded = true },
                        enabled = invoice.invoiceId.isNotBlank()
                    ) {
                        Text("Descargar PDF")
                    }
                    DropdownMenu(
                        expanded = downloadMenuExpanded,
                        onDismissRequest = { downloadMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Abrir ahora") },
                            onClick = {
                                downloadMenuExpanded = false
                                onDownloadPdf(invoice.invoiceId, InvoicePdfActionOption.OPEN_NOW)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Guardar en...") },
                            onClick = {
                                downloadMenuExpanded = false
                                onDownloadPdf(invoice.invoiceId, InvoicePdfActionOption.SAVE_AS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir") },
                            onClick = {
                                downloadMenuExpanded = false
                                onDownloadPdf(invoice.invoiceId, InvoicePdfActionOption.SHARE)
                            }
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
                                modifier = Modifier.size(16.dp), strokeWidth = 2.dp
                            )
                        } else {
                            Text("Cancelar")
                        }
                    }
                }
                if (allowFullReturn) {
                    OutlinedButton(
                        onClick = { onReturnTotal(invoice.invoiceId) },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retorno total")
                    }
                }
                if (allowPartialReturn) {
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

private fun toBaseAmount(
    amount: Double, invoiceCurrency: String, baseCurrency: String, conversionRate: Double?
): Double {
    if (invoiceCurrency.equals(baseCurrency, ignoreCase = true)) return amount
    return if (conversionRate != null && conversionRate > 0.0) amount * conversionRate else amount
}

private fun isPaidStatus(status: String?): Boolean {
    return status?.trim()?.equals("paid", ignoreCase = true) == true
}

private fun filterPendingInvoices(invoices: List<SalesInvoiceBO>): List<SalesInvoiceBO> {
    return invoices.filter { it.outstandingAmount > 0.0 && !isPaidStatus(it.status) }
}

private fun formatAmountRawForCurrency(amount: Double): String {
    val rounded = roundToCurrency(amount)
    val decimals = 2
    return formatDoubleToString(rounded, decimals)
}

private suspend fun resolveBaseToSelectedRate(
    cashboxManager: CashBoxManager,
    companyCurrency: String,
    selectedCurrency: String,
    invoiceCurrency: String,
    invoiceConversionRate: Double?,
    cachedRates: Map<String, Double>
): Double? {
    if (selectedCurrency.isBlank() || selectedCurrency.equals(companyCurrency, ignoreCase = true)) {
        return 1.0
    }
    cachedRates[selectedCurrency]?.takeIf { it > 0.0 }?.let { return it }
    invoiceConversionRate?.takeIf { it > 0.0 }?.let { rate ->
        if (selectedCurrency.equals(invoiceCurrency, ignoreCase = true)) {
            return 1.0 / rate
        }
    }

    val direct = cashboxManager.resolveExchangeRateBetween(
        fromCurrency = companyCurrency,
        toCurrency = selectedCurrency,
        allowNetwork = false
    )
    if (direct != null && direct > 0.0) return direct

    val reverse = cashboxManager.resolveExchangeRateBetween(
        fromCurrency = selectedCurrency,
        toCurrency = companyCurrency,
        allowNetwork = false
    )?.takeIf { it > 0.0 }?.let { 1.0 / it }
    if (reverse != null && reverse > 0.0) return reverse

    val ctx = cashboxManager.getContext()
    val ctxCurrency = normalizeCurrency(ctx?.currency)
    val ctxRate = ctx?.exchangeRate ?: 0.0
    if (ctxRate > 0.0) {
        if (companyCurrency.equals(ctxCurrency, true) && selectedCurrency.equals("USD", true)) {
            return 1.0 / ctxRate
        }
        if (selectedCurrency.equals(ctxCurrency, true) && companyCurrency.equals("USD", true)) {
            return ctxRate
        }
    }
    return null
}

private fun defaultReturnDestination(policy: ReturnPolicySettings): ReturnDestination {
    return if (policy.allowRefunds && policy.defaultDestination == ReturnDestinationPolicy.REFUND) {
        ReturnDestination.RETURN
    } else {
        ReturnDestination.CREDIT
    }
}

@Composable
private fun ReturnAccountingSummary(
    returnTotal: Double,
    currency: String?,
    refundEnabled: Boolean,
    creditEnabled: Boolean,
    projectedOutstanding: Double?
) {
    val normalizedCurrency = normalizeCurrency(currency)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Resumen contable",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total devolución")
            Text(formatCurrency(normalizedCurrency, returnTotal))
        }
        if (refundEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Reembolso")
                Text(formatCurrency(normalizedCurrency, returnTotal))
            }
        } else if (creditEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Crédito a favor")
                Text(formatCurrency(normalizedCurrency, returnTotal))
            }
        }
        if (projectedOutstanding != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saldo estimado")
                Text(formatCurrency(normalizedCurrency, projectedOutstanding))
            }
            Text(
                text = "Nota cajero: el saldo en ERPNext puede verse igual hasta la conciliación o cierre de caja.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "Stock: se reintegra al almacén en el retorno.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private enum class ReturnDestination(val label: String) {
    RETURN("Reembolso"), CREDIT("Crédito a favor")
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun CustomerOutstandingSummary(
    customer: CustomerBO,
    invoices: List<SalesInvoiceBO>,
    posBaseCurrency: String,
    cashboxManager: CashBoxManager
) {
    val strings = LocalAppStrings.current
    val companyCurrency = normalizeCurrency(posBaseCurrency)
    val invoiceCurrency = normalizeCurrency(invoices.firstOrNull()?.currency)
    val totalBase = if (invoices.isNotEmpty()) {
        invoices.sumOf {
            resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingCompany
        }
    } else {
        customer.totalPendingAmount ?: customer.currentBalance ?: 0.0
    }
    val totalPos = if (invoices.isNotEmpty()) {
        invoices.sumOf {
            resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingInvoice
        }
    } else {
        totalBase
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)
        ).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = strings.customer.outstandingSummaryTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(strings.customer.outstandingSummaryInvoicesLabel)
            Text("${if (invoices.isNotEmpty()) invoices.size else (customer.pendingInvoices ?: 0)}")
        }
        if (totalBase <= 0.0) {
            Text(strings.customer.outstandingSummaryAmountLabel)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (invoiceCurrency.isNotBlank()) {
                    Text(invoiceCurrency)
                    Text(formatCurrency(invoiceCurrency, totalPos))
                } else {
                    Text(companyCurrency)
                    Text(formatCurrency(companyCurrency, totalBase))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(companyCurrency)
                Text(formatCurrency(companyCurrency, totalBase))
            }
        }
    }
}

private fun handleQuickAction(
    actions: CustomerAction, customer: CustomerBO, actionType: CustomerQuickActionType
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
        initialValue = 0f, targetValue = gradientWidth, animationSpec = infiniteRepeatable(
            tween(durationMillis = durationMillis, easing = FastOutSlowInEasing), RepeatMode.Restart
        ), label = "shimmerTranslateAnim"
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
        ), shape = shape
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
