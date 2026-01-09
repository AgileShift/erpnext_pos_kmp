package com.erpnext.pos

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.views.customer.CustomerAction
import com.erpnext.pos.views.customer.CustomerInvoicesState
import com.erpnext.pos.views.customer.CustomerItem
import com.erpnext.pos.views.customer.CustomerListScreen
import com.erpnext.pos.views.customer.CustomerPaymentState
import com.erpnext.pos.views.customer.CustomerState
import com.erpnext.pos.views.customer.MetricBlock

@Preview
@Composable
fun MetricBlockPreview() {
    MetricBlock(
        label = "Disponible",
        value = $"$ ${1000.toString()}",
        secondaryValue = "C$ ${36_000.toString()}",
        isCritical = true
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