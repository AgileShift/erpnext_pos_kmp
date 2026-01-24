package com.erpnext.pos.views.paymententry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    state: PaymentEntryState,
    action: PaymentEntryAction
) {
    val snackbar = koinInject<SnackbarController>()

    state.errorMessage?.let {
        snackbar.show(it, SnackbarType.Error, SnackbarPosition.Top)
    }

    state.successMessage?.let {
        snackbar.show(it, SnackbarType.Success, SnackbarPosition.Top)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.invoiceId,
                onValueChange = action.onInvoiceIdChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ID de factura") },
                singleLine = true
            )

            OutlinedTextField(
                value = state.modeOfPayment,
                onValueChange = action.onModeOfPaymentChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Modo de pago") },
                singleLine = true
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange = action.onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monto") },
                singleLine = true
            )

            Button(
                onClick = action.onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Registrar pago")
            }

            Text(
                text = "Los pagos se guardan localmente y se sincronizan en el próximo ciclo.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentEntryScreenPreview() {
    PaymentEntryScreen(
        state = PaymentEntryState(invoiceId = "SINV-0001"),
        action = PaymentEntryAction()
    )
}
