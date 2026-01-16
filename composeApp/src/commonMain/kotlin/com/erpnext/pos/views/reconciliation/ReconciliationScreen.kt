package com.erpnext.pos.views.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localization.ReconciliationStrings
import com.erpnext.pos.utils.DenominationCatalog
import com.erpnext.pos.utils.DecimalFormatter
import com.erpnext.pos.utils.normalizeCurrency

private data class DenominationUi(
    val value: Double,
    val label: String,
    val type: DenominationType,
    val count: Int
)

private enum class DenominationType {
    Bill,
    Coin
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    state: ReconciliationState,
    mode: ReconciliationMode,
    closeState: CloseCashboxState,
    actions: ReconciliationAction
) {
    val appStrings = LocalAppStrings.current
    val strings = appStrings.reconciliation
    val summary = (state as? ReconciliationState.Success)?.summary
    val formatter = remember { DecimalFormatter() }
    // Conteo por moneda (POS + soportar USD/NIO si difieren)
    val countCurrencies = remember(summary?.currency) {
        buildList {
            summary?.currency?.uppercase()?.let { add(it) }
            add("USD")
            add("NIO")
        }.distinct()
    }
    var selectedCountCurrency by remember(summary?.currency) {
        mutableStateOf(summary?.currency?.uppercase() ?: "USD")
    }
    val countState =
        remember(summary?.openingEntryId) { mutableStateMapOf<String, List<DenominationUi>>() }

    fun ensureDenomsFor(currency: String) {
        if (!countState.containsKey(currency)) {
            val symbol = when {
                currency.equals(summary?.currency, ignoreCase = true) -> summary?.currencySymbol
                currency == "USD" -> "$"
                currency == "NIO" -> "C$"
                else -> null
            }
            countState[currency] = buildDenominationsForCurrency(currency, symbol, formatter)
        }
    }
    countCurrencies.forEach { ensureDenomsFor(it) }
    var denominations by remember(selectedCountCurrency, summary?.openingEntryId) {
        mutableStateOf(countState[selectedCountCurrency] ?: emptyList())
    }
    val cashTotal = if (summary == null) 0.0 else denominations.sumOf { it.value * it.count }
    fun inferCurrencyFromMode(mode: String, posCode: String): String {
        return when {
            mode.contains("USD", ignoreCase = true) || mode.contains("$") -> "USD"
            mode.contains("NIO", ignoreCase = true) || mode.contains("C$", ignoreCase = true) -> "NIO"
            else -> posCode
        }
    }
    val expectedCashByMode: Map<String, Double> = summary?.let {
        val cashKeys = it.cashModes.ifEmpty { it.expectedByMode.keys }
        it.expectedByMode.filterKeys { key -> cashKeys.contains(key) }
    } ?: emptyMap()
    val expectedCashByCurrency: Map<String, Double> = summary?.cashByCurrency ?: emptyMap()
    val expectedCashTotal = when {
        summary == null -> 0.0
        expectedCashByCurrency.isNotEmpty() -> expectedCashByCurrency.values.sum()
        summary.cashModes.isEmpty() -> summary.expectedTotal
        else -> expectedCashByMode.values.sum()
    }
    val openingTotalsByCurrency: Map<String, Double> = summary?.openingCashByCurrency ?: emptyMap()
    // Totales contados por moneda
    val cashTotalsByCurrency = countState.mapValues { entry ->
        entry.value.sumOf { it.value * it.count }
    }
    // Mapear conteos a modos de pago según moneda en el nombre del modo
    val countedByMode = run {
        val cashModes = summary?.cashModes?.ifEmpty { expectedCashByMode.keys } ?: emptySet()
        val totalPos = cashTotalsByCurrency[summary?.currency?.uppercase()]
            ?: cashTotalsByCurrency.values.sum()
        val totalUsd = cashTotalsByCurrency["USD"] ?: 0.0
        val totalNio = cashTotalsByCurrency["NIO"] ?: 0.0
        cashModes.associateWith { mode ->
            when {
                mode.contains("USD", ignoreCase = true) || mode.contains("$") -> totalUsd
                mode.contains("NIO", ignoreCase = true) || mode.contains(
                    "C$",
                    ignoreCase = true
                ) -> totalNio

                else -> totalPos
            }
        }
    }
    val totalCounted = countedByMode.values.sum()
    val difference = totalCounted - expectedCashTotal
    val formatAmount = remember(summary?.currency, summary?.currencySymbol) {
        { value: Double ->
            if (summary == null) value.toString() else formatCurrency(
                value = value,
                currencyCode = summary.currency,
                currencySymbol = summary.currencySymbol,
                formatter = formatter
            )
        }
    }

    Scaffold(
        topBar = {
            ReconciliationHeader(
                state = state,
                onBack = actions.onBack,
                strings = strings,
                backLabel = appStrings.common.back
            )
        },
        bottomBar = {
            if (summary != null && mode == ReconciliationMode.Close) {
                ReconciliationActionsBar(
                    difference = difference,
                    isClosing = closeState.isClosing,
                    onClose = { actions.onConfirmClose(countedByMode) },
                    onSaveDraft = actions.onSaveDraft,
                    formatAmount = formatAmount,
                    strings = strings
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                ReconciliationState.Loading -> {
                    Text(strings.loadingLabel)
                }

                ReconciliationState.Empty -> {
                    EmptyReconciliationState(strings)
                }

                is ReconciliationState.Error -> {
                    Text(state.message)
                }

                is ReconciliationState.Success -> {
            ReconciliationContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1240.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                        summary = state.summary,
                        countCurrencies = countCurrencies,
                        selectedCountCurrency = selectedCountCurrency,
                        onCurrencyChange = { currency ->
                            selectedCountCurrency = currency
                            denominations = countState[currency] ?: emptyList()
                        },
                        openingTotalsByCurrency = openingTotalsByCurrency,
                        expectedCashTotal = expectedCashTotal,
                        denominations = denominations,
                    onDenominationChange = { value, count ->
                        val updated = denominations.map { denom ->
                            if (denom.value == value) denom.copy(count = count) else denom
                        }
                        denominations = updated
                        countState[selectedCountCurrency] = updated
                    },
                    cashTotal = cashTotal,
                    countedByMode = countedByMode,
                    onCountedByModeChange = { _, _ -> },
                    totalCounted = totalCounted,
                    strings = strings,
                    onReload = actions.onReload
                )
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReconciliationHeader(
    state: ReconciliationState,
    onBack: () -> Unit,
    strings: ReconciliationStrings,
    backLabel: String
) {
    if (state !is ReconciliationState.Success) {
        Surface(shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = backLabel)
                }
                Text(strings.title, style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    val summary = state.summary
    Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = backLabel
                    )
                }
                IconButton(onClick = actions.onReload) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        strings.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${summary.openingEntryId.ifBlank { summary.posProfile }} • ${summary.periodStart}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    summary.cashierName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        strings.onlineLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReconciliationContent(
    modifier: Modifier,
    summary: ReconciliationSummaryUi,
    countCurrencies: List<String>,
    selectedCountCurrency: String,
    onCurrencyChange: (String) -> Unit,
    openingTotalsByCurrency: Map<String, Double>,
    expectedCashTotal: Double,
    denominations: List<DenominationUi>,
    onDenominationChange: (Double, Int) -> Unit,
    cashTotal: Double,
    countedByMode: Map<String, Double>,
    onCountedByModeChange: (String, Double) -> Unit,
    totalCounted: Double,
    strings: ReconciliationStrings,
    onReload: () -> Unit
) {
    val formatter = remember { DecimalFormatter() }
    val formatAmount = remember(summary.currency, summary.currencySymbol) {
        { value: Double ->
            formatCurrency(
                value = value,
                currencyCode = summary.currency,
                currencySymbol = summary.currencySymbol,
                formatter = formatter
            )
        }
    }
    val countSymbol = remember(selectedCountCurrency, summary.currencySymbol) {
        when {
            selectedCountCurrency.equals(summary.currency, ignoreCase = true) ->
                summary.currencySymbol?.takeIf { it.isNotBlank() } ?: selectedCountCurrency

            selectedCountCurrency == "USD" -> "$"
            selectedCountCurrency == "NIO" -> "C$"
            else -> selectedCountCurrency
        }
    }
    val formatCountAmount = remember(selectedCountCurrency, countSymbol) {
        { value: Double ->
            formatCurrency(
                value = value,
                currencyCode = selectedCountCurrency,
                currencySymbol = countSymbol,
                formatter = formatter
            )
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= 1000.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OpeningBalanceCard(
                        summary = summary,
                        formatAmount = formatAmount,
                        openingTotalsByCurrency = openingTotalsByCurrency,
                        strings = strings
                    )
                    val (creditPartial, creditPending) = computeCreditAmounts(summary)
                SystemSummaryCard(
                    summary = summary,
                    expectedTotalDisplay = expectedCashTotal,
                    creditPartial = creditPartial,
                    creditPending = creditPending,
                    formatAmount = formatAmount,
                    strings = strings,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(
                modifier = Modifier.weight(1.6f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
            CurrencySelector(
                currencies = countCurrencies,
                selected = selectedCountCurrency,
                onSelect = onCurrencyChange
            )
            DifferenceAlert(
                expected = expectedCashTotal,
                actual = totalCounted,
                formatAmount = formatAmount,
                strings = strings
            )
            ReloadChip(onReload = onReload)
            if (summary.cashModes.isNotEmpty()) {
                DenominationCounter(
                    denominations = denominations,
                    onCountChange = onDenominationChange,
                    total = cashTotal,
                            formatAmount = formatCountAmount,
                            strings = strings
                        )
                    }
                    NotesCard(strings = strings)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OpeningBalanceCard(
                    summary = summary,
                    formatAmount = formatAmount,
                    openingTotalsByCurrency = openingTotalsByCurrency,
                    strings = strings
                )
                val (creditPartial, creditPending) = computeCreditAmounts(summary)
                SystemSummaryCard(
                    summary = summary,
                    expectedTotalDisplay = expectedCashTotal,
                    creditPartial = creditPartial,
                    creditPending = creditPending,
                    formatAmount = formatAmount,
                    strings = strings
                )
            CurrencySelector(
                currencies = countCurrencies,
                selected = selectedCountCurrency,
                onSelect = onCurrencyChange
            )
            DifferenceAlert(
                expected = expectedCashTotal,
                actual = totalCounted,
                formatAmount = formatAmount,
                strings = strings
            )
            ReloadChip(onReload = onReload)
            if (summary.cashModes.isNotEmpty()) {
                DenominationCounter(
                    denominations = denominations,
                    onCountChange = onDenominationChange,
                    total = cashTotal,
                        formatAmount = formatCountAmount,
                        strings = strings
                    )
                }
                NotesCard(strings = strings)
            }
        }
    }
}

@Composable
private fun OpeningBalanceCard(
    summary: ReconciliationSummaryUi,
    formatAmount: (Double) -> String,
    openingTotalsByCurrency: Map<String, Double>,
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(gradient, shape = MaterialTheme.shapes.large)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                strings.openingAmountTitle,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                formatAmount(summary.openingAmount),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (openingTotalsByCurrency.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    openingTotalsByCurrency.forEach { (code, amount) ->
                        Text(
                            "${strings.openingAmountTitle} $code: ${formatAmount(amount)}",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoLabelValue(
                    strings.cashierLabel,
                    summary.cashierName,
                    MaterialTheme.colorScheme.onPrimary
                )
                InfoLabelValue(
                    strings.shiftLabel,
                    summary.posProfile,
                    MaterialTheme.colorScheme.onPrimary
                )
                InfoLabelValue(
                    strings.dateLabel,
                    summary.periodStart,
                    MaterialTheme.colorScheme.onPrimary
                )
            }
            if (summary.openingEntryId.isNotBlank()) {
                Text(
                    "${strings.openingLabel}: ${summary.openingEntryId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun SystemSummaryCard(
    summary: ReconciliationSummaryUi,
    expectedTotalDisplay: Double,
    creditPartial: Double,
    creditPending: Double,
    formatAmount: (Double) -> String,
    strings: com.erpnext.pos.localization.ReconciliationStrings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(strings.systemSummaryTitle, style = MaterialTheme.typography.titleMedium)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        strings.expectedTotalLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        formatAmount(expectedTotalDisplay),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${strings.invoicesLabel}: ${summary.invoiceCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryRow(
                    label = "Ventas de contado",
                    value = formatAmount(summary.salesTotal),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Filled.ShoppingCart
                )
                SummaryRow(
                    label = "Pagos recibidos",
                    value = formatAmount(summary.paymentsTotal),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Filled.ArrowDownward
                )
                SummaryRow(
                    label = "Ventas crédito / pagos parciales",
                    value = formatAmount(creditPartial),
                    valueColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.ArrowDownward,
                    badgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                SummaryRow(
                    label = "Pagos pendientes (crédito)",
                    value = formatAmount(creditPending),
                    valueColor = MaterialTheme.colorScheme.error,
                    icon = Icons.Filled.ArrowUpward,
                    badgeColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                )
                SummaryRow(
                    label = strings.expensesLabel,
                    value = formatAmount(summary.expensesTotal),
                    valueColor = MaterialTheme.colorScheme.error,
                    icon = Icons.Filled.ArrowUpward,
                    badgeColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                )
            }
        }
    }
}

@Composable
private fun DifferenceAlert(
    expected: Double,
    actual: Double,
    formatAmount: (Double) -> String,
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    val difference = actual - expected
    val isBalanced = kotlin.math.abs(difference) < 0.01
    val cardColors = when {
        isBalanced -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(
                alpha = 0.1f
            )
        )

        difference < 0 -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(
                alpha = 0.12f
            )
        )

        else -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(
                alpha = 0.15f
            )
        )
    }
    val icon = when {
        isBalanced -> Icons.Filled.CheckCircle
        difference < 0 -> Icons.Filled.Warning
        else -> Icons.Filled.Sync
    }
    val title = when {
        isBalanced -> strings.differenceBalancedTitle
        difference < 0 -> strings.differenceShortTitle
        else -> strings.differenceOverTitle
    }
    val badge = when {
        isBalanced -> strings.differencePerfectBadge
        difference < 0 -> strings.differenceAttentionBadge
        else -> strings.differenceReviewBadge
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isBalanced) {
                    Text(
                        formatAmount(kotlin.math.abs(difference)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(strings.differenceBalancedBody, style = MaterialTheme.typography.bodySmall)
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun CurrencySelector(
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
private fun DenominationCounter(
    denominations: List<DenominationUi>,
    onCountChange: (value: Double, count: Int) -> Unit,
    total: Double,
    formatAmount: (Double) -> String,
    strings: com.erpnext.pos.localization.ReconciliationStrings
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
            Text(strings.cashCountTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.cashCountSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (coins.isNotEmpty() && bills.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTab == DenominationType.Bill,
                        onClick = { activeTab = DenominationType.Bill },
                        label = { Text(strings.billsLabel) }
                    )
                    FilterChip(
                        selected = activeTab == DenominationType.Coin,
                        onClick = { activeTab = DenominationType.Coin },
                        label = { Text(strings.coinsLabel) }
                    )
                }
            }

            when (activeTab) {
                DenominationType.Bill -> DenominationSection(
                    title = strings.billsLabel,
                    denominations = bills,
                    onCountChange = onCountChange,
                    formatAmount = formatAmount
                )

                DenominationType.Coin -> DenominationSection(
                    title = strings.coinsLabel,
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
                Text(strings.totalCountedLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    formatAmount(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
                fontWeight = FontWeight.SemiBold
            )
            Text(
                formatAmount(denom.value * denom.count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onCountChange(denom.value, (denom.count - 1).coerceAtLeast(0)) },
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                animatedCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { onCountChange(denom.value, denom.count + 1) },
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun NotesCard(strings: com.erpnext.pos.localization.ReconciliationStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(strings.notesTitle, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.notesPlaceholder) }
            )
        }
    }
}

@Composable
private fun ReconciliationActionsBar(
    difference: Double,
    isClosing: Boolean,
    onClose: () -> Unit,
    onSaveDraft: () -> Unit,
    formatAmount: (Double) -> String,
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    val hasDifference = kotlin.math.abs(difference) > 0.01
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (hasDifference) {
                        Text(
                            strings.closeWithDifferenceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatAmount(kotlin.math.abs(difference)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            strings.closeCashboxLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSaveDraft,
                        enabled = !isClosing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(strings.saveDraftLabel)
                    }
                    Button(
                        onClick = onClose,
                        enabled = !isClosing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasDifference) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isClosing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (hasDifference) strings.closeWithDifferenceLabel else strings.closeCashboxLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = badgeColor,
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun InfoLabelValue(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyReconciliationState(strings: com.erpnext.pos.localization.ReconciliationStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(strings.emptyTitle, style = MaterialTheme.typography.titleMedium)
            Text(strings.emptyMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun computeCreditPartial(summary: ReconciliationSummaryUi): Double {
    val credit = summary.expectedTotal - summary.salesTotal - summary.paymentsTotal
    return if (credit < 0) 0.0 else credit
}

private fun computeCreditAmounts(summary: ReconciliationSummaryUi): Pair<Double, Double> {
    val creditPartial = computeCreditPartial(summary)
    val creditPending = (summary.expectedTotal - summary.salesTotal - summary.paymentsTotal)
        .takeIf { it > 0 } ?: 0.0
    return creditPartial to creditPending
}

private fun buildDenominationsForCurrency(
    currencyCode: String,
    symbol: String?,
    formatter: DecimalFormatter
): List<DenominationUi> {
    val currency = currencyCode.uppercase()
    val normalized = normalizeCurrency(currency)
    val definitions = DenominationCatalog.forCurrency(normalized)
    val rawDenoms: List<Pair<Double, DenominationType>> = buildList {
        addAll(definitions.bills.map { it to DenominationType.Bill })
        addAll(definitions.coins.map { it to DenominationType.Coin })
    }
    return rawDenoms.map { (value, type) ->
        val decimals = if (value < 1.0) 2 else 0
        val labelValue = formatter.format(value, decimals, includeSeparator = true)
        val label = if (symbol != null) "$symbol$labelValue" else "$labelValue $currency"
        DenominationUi(value = value, label = label, type = type, count = 0)
    }
}

private fun formatCurrency(
    value: Double,
    currencyCode: String,
    currencySymbol: String?,
    formatter: DecimalFormatter
): String {
    val prefix = currencySymbol?.takeIf { it.isNotBlank() } ?: currencyCode
    val formatted = formatter.format(value, 2, includeSeparator = true)
    return "$prefix $formatted"
}
