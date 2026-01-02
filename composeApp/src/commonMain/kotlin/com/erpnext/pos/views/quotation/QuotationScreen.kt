@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.quotation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.erpnext.pos.views.salesflow.SalesFlowContext
import com.erpnext.pos.views.salesflow.SalesFlowContextSummary
import com.erpnext.pos.views.salesflow.SalesFlowReferenceField
import com.erpnext.pos.views.salesflow.SalesFlowActionItem

@Composable
fun QuotationScreen(
    state: QuotationState,
    action: QuotationAction,
    context: SalesFlowContext?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cotizaciones") },
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
                QuotationState.Loading -> CircularProgressIndicator()
                is QuotationState.Error -> Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                QuotationState.Ready -> {
                    var referenceId by rememberSaveable { mutableStateOf(context?.sourceId.orEmpty()) }
                    var customerName by rememberSaveable { mutableStateOf(context?.customerName.orEmpty()) }
                    var validUntil by rememberSaveable { mutableStateOf("") }
                    var itemName by rememberSaveable { mutableStateOf("") }
                    var itemQty by rememberSaveable { mutableStateOf("") }
                    var itemRate by rememberSaveable { mutableStateOf("") }
                    var items by remember { mutableStateOf(listOf<DraftItem>()) }

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
                                text = "Quotation details",
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
                                value = validUntil,
                                onValueChange = { validUntil = it },
                                label = { Text("Valid until") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SalesFlowReferenceField(
                                label = "Quotation reference (optional)",
                                value = referenceId,
                                onValueChange = { referenceId = it },
                                helperText = "Use this if you are continuing from an existing quotation."
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
                                    val qty = itemQty.toDoubleOrNull() ?: 1.0
                                    val rate = itemRate.toDoubleOrNull() ?: 0.0
                                    if (itemName.isNotBlank()) {
                                        items = items + DraftItem(itemName, qty, rate)
                                        itemName = ""
                                        itemQty = ""
                                        itemRate = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Add item")
                            }

                            val subtotal = items.sumOf { it.amount }
                            items.forEach { item ->
                                Text(
                                    text = "${item.name} | Qty: ${item.qty} | Rate: ${item.rate} | Amount: ${item.amount}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Subtotal: $subtotal",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SalesFlowActionButtons(
                        primary = SalesFlowActionItem(
                            label = "Continue to Sales Order",
                            onClick = { action.onCreateSalesOrder(referenceId.takeIf { it.isNotBlank() }) }
                        ),
                        secondary = listOf(
                            SalesFlowActionItem(
                                label = "Create Sales Invoice",
                                onClick = { action.onCreateInvoice(referenceId.takeIf { it.isNotBlank() }) }
                            )
                        )
                    )
                }
            }
        }
    }
}

private data class DraftItem(
    val name: String,
    val qty: Double,
    val rate: Double
) {
    val amount: Double = qty * rate
}
