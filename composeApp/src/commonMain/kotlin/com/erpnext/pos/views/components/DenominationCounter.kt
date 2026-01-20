package com.erpnext.pos.views.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.utils.DenominationCatalog
import com.erpnext.pos.utils.DecimalFormatter
import com.erpnext.pos.utils.normalizeCurrency

data class DenominationUi(
    val value: Double,
    val label: String,
    val type: DenominationType,
    val count: Int
)

enum class DenominationType {
    Bill,
    Coin
}

data class DenominationCounterLabels(
    val title: String,
    val subtitle: String,
    val billsLabel: String,
    val coinsLabel: String,
    val totalLabel: String
)

@Composable
fun CurrencySelector(
    currencies: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    if (currencies.size <= 1) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        currencies.forEach { code ->
            FilterChip(
                selected = code == selected,
                onClick = { onSelect(code) },
                label = { Text(code) }
            )
        }
    }
}

@Composable
fun DenominationCounter(
    denominations: List<DenominationUi>,
    onCountChange: (value: Double, count: Int) -> Unit,
    total: Double,
    formatAmount: (Double) -> String,
    labels: DenominationCounterLabels,
    countCurrencies: List<String>,
    selectedCountCurrency: String,
    onCurrencyChange: (String) -> Unit,
) {
    var activeTab by remember { mutableStateOf(DenominationType.Bill) }
    val bills = denominations.filter { it.type == DenominationType.Bill }
    val coins = denominations.filter { it.type == DenominationType.Coin }
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    labels.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (labels.subtitle.isNotBlank()) {
                    Text(
                        " (${labels.subtitle})",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                CurrencySelector(
                    currencies = countCurrencies,
                    selected = selectedCountCurrency,
                    onSelect = onCurrencyChange
                )
            }

            if (coins.isNotEmpty() && bills.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTab == DenominationType.Bill,
                        onClick = { activeTab = DenominationType.Bill },
                        label = { Text(labels.billsLabel) }
                    )
                    FilterChip(
                        selected = activeTab == DenominationType.Coin,
                        onClick = { activeTab = DenominationType.Coin },
                        label = { Text(labels.coinsLabel) }
                    )
                }
            }

            when (activeTab) {
                DenominationType.Bill -> DenominationSection(
                    title = labels.billsLabel,
                    denominations = bills,
                    onCountChange = onCountChange,
                    formatAmount = formatAmount
                )

                DenominationType.Coin -> DenominationSection(
                    title = labels.coinsLabel,
                    denominations = coins,
                    onCountChange = onCountChange,
                    formatAmount = formatAmount
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(labels.totalLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    formatAmount(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DenominationSection(
    title: String,
    denominations: List<DenominationUi>,
    onCountChange: (value: Double, count: Int) -> Unit,
    formatAmount: (Double) -> String
) {
    if (denominations.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            denominations.forEach { denom ->
                DenominationRow(
                    denom = denom,
                    onCountChange = onCountChange,
                    formatAmount = formatAmount
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DenominationRow(
    denom: DenominationUi,
    onCountChange: (value: Double, count: Int) -> Unit,
    formatAmount: (Double) -> String
) {
    val animatedCount by animateIntAsState(denom.count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                denom.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Text(
                formatAmount(denom.value * denom.count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onCountChange(denom.value, (denom.count - 1).coerceAtLeast(0)) },
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    animatedCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                IconButton(
                    onClick = { onCountChange(denom.value, denom.count + 1) },
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

fun buildDenominationsForCurrency(
    currency: String,
    symbolOverride: String? = null,
    formatter: DecimalFormatter
): List<DenominationUi> {
    val normalized = normalizeCurrency(currency)
    val definitions = DenominationCatalog.forCurrency(normalized)
    val rawDenoms: List<Pair<Double, DenominationType>> = buildList {
        addAll(definitions.bills.map { it to DenominationType.Bill })
        addAll(definitions.coins.map { it to DenominationType.Coin })
    }
    val symbol = symbolOverride?.ifBlank { null } ?: normalized
    return rawDenoms.map { (value, type) ->
        val decimals = if (value < 1.0) 2 else 0
        val labelValue = formatter.format(value, decimals, includeSeparator = true)
        val label = if (symbol.isNotBlank()) "$symbol$labelValue" else "$labelValue $normalized"
        DenominationUi(value = value, label = label, type = type, count = 0)
    }
}
