@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erpnext.pos.views.salesflow.SalesFlowActionButtons
import com.erpnext.pos.views.salesflow.SalesFlowActionItem
import com.erpnext.pos.views.salesflow.SalesFlowContext
import com.erpnext.pos.views.salesflow.SalesFlowContextSummary
import com.erpnext.pos.views.salesflow.SalesFlowReferenceField
import com.erpnext.pos.views.salesorder.SalesOrderAction
import com.erpnext.pos.views.salesorder.SalesOrderState

@Composable
fun SalesOrderScreen(
    state: SalesOrderState,
    action: SalesOrderAction,
    context: SalesFlowContext?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Órdenes de venta") },
                navigationIcon = {
                    IconButton(onClick = action.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                SalesOrderState.Loading -> CircularProgressIndicator()
                is SalesOrderState.Error -> Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                SalesOrderState.Ready -> {
                    var referenceId by rememberSaveable { mutableStateOf(context?.sourceId.orEmpty()) }
                    var customerName by rememberSaveable { mutableStateOf(context?.customerName.orEmpty()) }
                    var deliveryDate by rememberSaveable { mutableStateOf("") }
                    var itemName by rememberSaveable { mutableStateOf("") }
                    var itemQty by rememberSaveable { mutableStateOf("") }
                    var itemRate by rememberSaveable { mutableStateOf("") }
                    var items by rememberSaveable { mutableStateOf(listOf<String>()) }

                    SalesFlowContextSummary(context)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Sales order details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = deliveryDate,
                                onValueChange = { deliveryDate = it },
                                label = { Text("Delivery date") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SalesFlowReferenceField(
                                label = "Sales order reference (optional)",
                                value = referenceId,
                                onValueChange = { referenceId = it },
                                helperText = "Use this to link a delivery note or invoice to an existing order."
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Item") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = itemQty,
                                    onValueChange = { itemQty = it },
                                    label = { Text("Qty") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = itemRate,
                                    onValueChange = { itemRate = it },
                                    label = { Text("Rate") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    if (itemName.isNotBlank()) {
                                        items = items + "$itemName | Qty: ${itemQty.ifBlank { "1" }} | Rate: ${itemRate.ifBlank { "0" }}"
                                        itemName = ""
                                        itemQty = ""
                                        itemRate = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Add item")
                            }

                            items.forEach { item ->
                                Text(text = item, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SalesFlowActionButtons(
                        primary = SalesFlowActionItem(
                            label = "Continue to Delivery Note",
                            onClick = {
                                action.onCreateDeliveryNote(referenceId.takeIf { it.isNotBlank() })
                            }
                        ),
                        secondary = listOf(
                            SalesFlowActionItem(
                                label = "Create Sales Invoice",
                                onClick = {
                                    action.onCreateInvoice(referenceId.takeIf { it.isNotBlank() })
                                }
                            )
                        )
                    )
                }
            }
        }
    }
}
