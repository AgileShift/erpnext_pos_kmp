package com.erpnext.pos.views.salesflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.LocalAppStrings

data class SalesFlowActionItem(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun SalesFlowContextSummary(context: SalesFlowContext?) {
    if (context == null) return

    val strings = LocalAppStrings.current
    val sourceLabel = context.sourceLabel()
    val sourceInfo = sourceLabel?.let { label ->
        context.sourceId?.let { id -> "$label (ID: $id)" } ?: label
    }

    val customerLabel = context.customerName ?: context.customerId
    if (customerLabel == null && sourceInfo == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (customerLabel != null) {
                InfoRow(label = strings.salesFlow.customerLabel, value = customerLabel, isCompact = true)
            }
            if (sourceInfo != null) {
                InfoRow(label = strings.salesFlow.sourceLabel, value = sourceInfo, isCompact = true)
            }
        }
    }
}

@Composable
fun SalesFlowReferenceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helperText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SalesFlowActionButtons(
    primary: SalesFlowActionItem,
    secondary: List<SalesFlowActionItem> = emptyList()
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = primary.onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(primary.label)
        }
        secondary.forEach { action ->
            Button(
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(action.label)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isCompact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
