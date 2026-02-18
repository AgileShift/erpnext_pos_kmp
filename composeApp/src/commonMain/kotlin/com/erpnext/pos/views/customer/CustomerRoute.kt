package com.erpnext.pos.views.customer

import androidx.paging.PagingData
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.views.salesflow.SalesFlowContext
import com.erpnext.pos.views.salesflow.SalesFlowContextStore
import com.erpnext.pos.views.salesflow.SalesFlowSource
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRoute(
    coordinator: CustomerCoordinator = rememberCustomerCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState(CustomerState.Loading)
    val customersPagingFlow by coordinator.customersPagingFlow.collectAsState(flowOf(PagingData.empty()))
    val outstandingInvoicesPagingFlow by coordinator.outstandingInvoicesPagingFlow.collectAsState(flowOf(PagingData.empty<SalesInvoiceBO>()))
    val historyInvoicesPagingFlow by coordinator.historyInvoicesPagingFlow.collectAsState(flowOf(PagingData.empty<SalesInvoiceBO>()))
    val invoicesState by coordinator.invoicesState.collectAsState()
    val paymentState by coordinator.paymentState.collectAsState()
    val historyState by coordinator.historyState.collectAsState(CustomerInvoiceHistoryState.Idle)
    val historyMessage by coordinator.historyMessage.collectAsState(null)
    val returnInfoMessage by coordinator.returnInfoMessage.collectAsState(null)
    val historyBusy by coordinator.historyActionBusy.collectAsState(false)
    val customerMessage by coordinator.customerMessage.collectAsState(null)
    val dialogDataState by coordinator.dialogDataState.collectAsState(CustomerDialogDataState())
    val returnPolicy by coordinator.returnPolicy.collectAsState()
    val navManager: NavigationManager = koinInject()
    val salesFlowStore: SalesFlowContextStore = koinInject()
    val actions = rememberCustomerActions(coordinator, navManager, salesFlowStore)

    LaunchedEffect(Unit) {
        actions.fetchAll()
    }

    CustomerListScreen(
        state = uiState,
        customersPagingFlow = customersPagingFlow,
        outstandingInvoicesPagingFlow = outstandingInvoicesPagingFlow,
        historyInvoicesPagingFlow = historyInvoicesPagingFlow,
        invoicesState = invoicesState,
        paymentState = paymentState,
        historyState = historyState,
        historyMessage = historyMessage,
        returnInfoMessage = returnInfoMessage,
        historyBusy = historyBusy,
        customerMessage = customerMessage,
        dialogDataState = dialogDataState,
        returnPolicy = returnPolicy,
        actions = actions
    )
}

@Composable
fun rememberCustomerActions(
    coordinator: CustomerCoordinator,
    navManager: NavigationManager,
    salesFlowStore: SalesFlowContextStore
): CustomerAction {
    return remember(coordinator, navManager, salesFlowStore) {
        CustomerAction(
            fetchAll = coordinator::fetchAll,
            onStateSelected = coordinator::onTerritorySelected,
            checkCredit = coordinator::checkCredit,
            onRefresh = coordinator::onRefresh,
            onSearchQueryChanged = coordinator::onSearchQueryChanged,
            onViewPendingInvoices = { coordinator.loadOutstandingInvoices(it.name) },
            onViewInvoiceHistory = { coordinator.loadInvoiceHistory(it.name) },
            onCreateQuotation = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.Quotation)
            },
            onCreateSalesOrder = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.SalesOrder)
            },
            onCreateDeliveryNote = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.DeliveryNote)
            },
            onCreateInvoice = { customer ->
                salesFlowStore.set(
                    SalesFlowContext(
                        customerId = customer.name,
                        customerName = customer.customerName,
                        sourceType = SalesFlowSource.Customer
                    )
                )
                navManager.navigateTo(NavRoute.Billing)
            },
            onRegisterPayment = { coordinator.loadOutstandingInvoices(it.name) },
            onDownloadInvoicePdf = coordinator::downloadInvoicePdf,
            loadOutstandingInvoices = { coordinator.loadOutstandingInvoices(it.name) },
            clearOutstandingInvoices = coordinator::clearOutstandingInvoices,
            registerPayment = coordinator::registerPayment,
            clearPaymentMessages = coordinator::clearPaymentMessages,
            clearInvoiceHistory = coordinator::clearInvoiceHistory,
            clearInvoiceHistoryMessages = coordinator::clearInvoiceHistoryMessages,
            clearCustomerMessages = coordinator::clearCustomerMessages,
            onInvoiceHistoryAction = { invoiceId, action, reason, refundMode, refundReference, applyRefund ->
                coordinator.performInvoiceHistoryAction(
                    invoiceId,
                    action,
                    reason,
                    refundMode,
                    refundReference,
                    applyRefund
                )
            },
            loadInvoiceLocal = { invoiceId -> coordinator.loadInvoiceLocal(invoiceId) },
            onInvoicePartialReturn = { invoiceId, reason, refundMode, refundReference, applyRefund, items ->
                coordinator.submitPartialReturn(
                    invoiceId,
                    reason,
                    refundMode,
                    refundReference,
                    applyRefund,
                    items
                )
            },
            onCreateCustomer = coordinator::createCustomer
        )
    }
}
