package com.erpnext.pos.views.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localization.ReconciliationStrings
import com.erpnext.pos.utils.DecimalFormatter
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.components.DenominationCounter
import com.erpnext.pos.views.components.DenominationCounterLabels
import com.erpnext.pos.views.components.DenominationUi
import com.erpnext.pos.views.components.buildDenominationsForCurrency
import kotlin.math.abs

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
    // Conteo por moneda (usa monedas de métodos de efectivo si están configuradas)
    val countCurrencies = remember(summary?.currency, summary?.cashCurrencies) {
        val fromProfile = summary?.cashCurrencies?.map { it.uppercase() }?.distinct().orEmpty()
        if (fromProfile.isNotEmpty()) {
            fromProfile.sorted()
        } else {
            buildList {
                summary?.currency?.uppercase()?.let { add(it) }
                add("USD")
                add("NIO")
            }.distinct()
        }
    }
    var selectedCountCurrency by remember(summary?.currency, countCurrencies) {
        mutableStateOf(countCurrencies.firstOrNull() ?: summary?.currency?.uppercase() ?: "USD")
    }
    val countState =
        remember(summary?.openingEntryId) { mutableStateMapOf<String, List<DenominationUi>>() }

    fun ensureDenomsFor(currency: String) {
        if (!countState.containsKey(currency)) {
            val symbol = when {
                currency.equals(summary?.currency, ignoreCase = true) -> summary?.currencySymbol
                else -> currency.toCurrencySymbol().ifBlank { null }
            }
            countState[currency] = buildDenominationsForCurrency(currency, symbol, formatter)
        }
    }
    countCurrencies.forEach { ensureDenomsFor(it) }
    var denominations by remember(selectedCountCurrency, summary?.openingEntryId) {
        mutableStateOf(countState[selectedCountCurrency] ?: emptyList())
    }
    val cashTotal = if (summary == null) 0.0 else denominations.sumOf { it.value * it.count }

    val expectedCashByMode: Map<String, Double> = summary?.let {
        val cashKeys = it.cashModes.ifEmpty { it.expectedByMode.keys }
        it.expectedByMode.filterKeys { key -> cashKeys.contains(key) }
    } ?: emptyMap()
    val expectedCashByCurrency: Map<String, Double> = summary?.let { s ->
        val map = mutableMapOf<String, Double>()
        s.openingCashByCurrency.forEach { (code, amount) ->
            val key = code.uppercase()
            map[key] = (map[key] ?: 0.0) + amount
        }
        s.cashByCurrency.forEach { (code, amount) ->
            val key = code.uppercase()
            map[key] = (map[key] ?: 0.0) + amount
        }
        if (map.isEmpty()) {
            map[s.currency.uppercase()] = s.expectedTotal
        }
        map
    } ?: emptyMap()
    // Totales contados por moneda
    val cashTotalsByCurrency = countState.mapValues { entry ->
        entry.value.sumOf { it.value * it.count }
    }
    // Mapear conteos a modos de pago según moneda, sin duplicar el total en múltiples modos.
    val countedByMode = run {
        val cashModes = summary?.cashModes?.ifEmpty { expectedCashByMode.keys } ?: emptySet()
        val fallbackCurrency = summary?.currency?.uppercase()
            ?: countCurrencies.firstOrNull()
            ?: "USD"
        val cashModeCurrency = summary?.cashModeCurrency.orEmpty()
        val modesByCurrency = cashModes.groupBy { mode ->
            cashModeCurrency[mode]?.uppercase() ?: fallbackCurrency
        }
        val primaryModes = modesByCurrency.mapValues { (_, modes) ->
            modes.firstOrNull { isPrimaryCashModeName(it) } ?: modes.first()
        }
        primaryModes.mapNotNull { (currency, mode) ->
            val counted = cashTotalsByCurrency[currency] ?: 0.0
            mode to counted
        }.toMap()
    }
    val fallbackCurrency = summary?.currency?.uppercase() ?: countCurrencies.firstOrNull() ?: "USD"
    val countedByCurrency = cashTotalsByCurrency.ifEmpty {
        mapOf(fallbackCurrency to 0.0)
    }
    val differenceByCurrency = buildDifferenceByCurrency(expectedCashByCurrency, countedByCurrency)

    Scaffold(
        topBar = {
            ReconciliationHeader(
                state = state,
                strings = strings,
            )
        },
        bottomBar = {
            if (summary != null && mode == ReconciliationMode.Close) {
                ReconciliationActionsBar(
                    differenceByCurrency = differenceByCurrency,
                    isClosing = closeState.isClosing,
                    onClose = { actions.onConfirmClose(countedByMode) },
                    onSaveDraft = actions.onSaveDraft,
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
                        expectedCashByCurrency = expectedCashByCurrency,
                        countCurrencies = countCurrencies,
                        selectedCountCurrency = selectedCountCurrency,
                        onCurrencyChange = { currency ->
                            selectedCountCurrency = currency
                            denominations = countState[currency] ?: emptyList()
                        },
                        //openingTotalsByCurrency = openingTotalsByCurrency,
                        denominations = denominations,
                        onDenominationChange = { value, count ->
                            val updated = denominations.map { denom ->
                                if (denom.value == value) denom.copy(count = count) else denom
                            }
                            denominations = updated
                            countState[selectedCountCurrency] = updated
                        },
                        cashTotal = cashTotal,
                        countedByCurrency = countedByCurrency,
                        strings = strings,
                    )
                }
            }
            if (closeState.isSyncing) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text("Sincronizando...") },
                    text = {
                        Column {
                            Text(
                                closeState.syncMessage
                                    ?: "Esperando que la caja se alinee con el servidor.",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun isPrimaryCashModeName(mode: String): Boolean {
    val normalized = mode.trim().lowercase()
    return normalized.contains("efectivo") || normalized.contains("cash")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReconciliationHeader(
    state: ReconciliationState,
    strings: ReconciliationStrings,
) {
    if (state !is ReconciliationState.Success) {
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
                /*IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = backLabel
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))*/
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
        }
    }
}

@Composable
private fun ReconciliationContent(
    modifier: Modifier,
    summary: ReconciliationSummaryUi,
    expectedCashByCurrency: Map<String, Double>,
    countCurrencies: List<String>,
    selectedCountCurrency: String,
    onCurrencyChange: (String) -> Unit,
    denominations: List<DenominationUi>,
    onDenominationChange: (Double, Int) -> Unit,
    cashTotal: Double,
    countedByCurrency: Map<String, Double>,
    strings: ReconciliationStrings,
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
        if (selectedCountCurrency.equals(summary.currency, ignoreCase = true)) {
            summary.currencySymbol?.takeIf { it.isNotBlank() }
                ?: selectedCountCurrency.toCurrencySymbol().ifBlank { selectedCountCurrency }
        } else {
            selectedCountCurrency.toCurrencySymbol().ifBlank { selectedCountCurrency }
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
                val leftScrollState = rememberScrollState()
                val rightScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(leftScrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OpeningBalanceCard(
                        summary = summary,
                        formatAmount = formatAmount,
                        formatAmountFor = { value, code ->
                            formatCurrency(
                                value,
                                currencyCode = code,
                                currencySymbol = code.toCurrencySymbol().ifBlank { code },
                                formatter = formatter
                            )
                        },
                        strings = strings
                    )
                    val (creditPartial, creditPending) = computeCreditAmounts(summary)
                    SystemSummaryCardMultiCurrency(
                        summary = summary,
                        expectedCashByCurrency = expectedCashByCurrency,
                        salesByCurrency = summary.salesByCurrency,
                        creditPartialByCurrency = summary.creditPartialByCurrency,
                        creditPendingByCurrency = summary.creditPendingByCurrency,
                        expensesByCurrency = summary.expensesByCurrency,
                        creditPartial = creditPartial,
                        creditPending = creditPending,
                        formatAmountFor = { value, code ->
                            formatCurrency(
                                value,
                                currencyCode = code,
                                currencySymbol = code.toCurrencySymbol().ifBlank { code },
                                formatter = formatter
                            )
                        },
                        strings = strings
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1.6f)
                        .fillMaxHeight()
                        .verticalScroll(rightScrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DifferenceAlertsByCurrency(
                        expectedByCurrency = expectedCashByCurrency,
                        countedByCurrency = countedByCurrency,
                        strings = strings
                    )
                    if (summary.cashModes.isNotEmpty()) {
                        DenominationCounter(
                            denominations = denominations,
                            onCountChange = onDenominationChange,
                            total = cashTotal,
                            formatAmount = formatCountAmount,
                            labels = DenominationCounterLabels(
                                title = strings.cashCountTitle,
                                subtitle = strings.cashCountSubtitle,
                                billsLabel = strings.billsLabel,
                                coinsLabel = strings.coinsLabel,
                                totalLabel = strings.totalCountedLabel
                            ),
                            countCurrencies = countCurrencies,
                            selectedCountCurrency = selectedCountCurrency,
                            onCurrencyChange = onCurrencyChange
                        )
                    }
                    //NotesCard(strings = strings)
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
                    formatAmountFor = { value, code ->
                        formatCurrency(
                            value,
                            currencyCode = code,
                            currencySymbol = code.toCurrencySymbol().ifBlank { code },
                            formatter = formatter
                        )
                    },
                    strings = strings
                )
                val (creditPartial, creditPending) = computeCreditAmounts(summary)
                SystemSummaryCardMultiCurrency(
                    summary = summary,
                    expectedCashByCurrency = expectedCashByCurrency,
                    salesByCurrency = summary.salesByCurrency,
                    creditPartialByCurrency = summary.creditPartialByCurrency,
                    creditPendingByCurrency = summary.creditPendingByCurrency,
                    expensesByCurrency = summary.expensesByCurrency,
                    creditPartial = creditPartial,
                    creditPending = creditPending,
                    formatAmountFor = { value, code ->
                        formatCurrency(
                            value,
                            currencyCode = code,
                            currencySymbol = code.toCurrencySymbol().ifBlank { code },
                            formatter = formatter
                        )
                    },
                    strings = strings
                )
                DifferenceAlertsByCurrency(
                    expectedByCurrency = expectedCashByCurrency,
                    countedByCurrency = countedByCurrency,
                    strings = strings
                )
                if (summary.cashModes.isNotEmpty()) {
                    DenominationCounter(
                        denominations = denominations,
                        onCountChange = onDenominationChange,
                        total = cashTotal,
                        formatAmount = formatCountAmount,
                        labels = DenominationCounterLabels(
                            title = strings.cashCountTitle,
                            subtitle = strings.cashCountSubtitle,
                            billsLabel = strings.billsLabel,
                            coinsLabel = strings.coinsLabel,
                            totalLabel = strings.totalCountedLabel
                        ),
                        countCurrencies = countCurrencies,
                        selectedCountCurrency = selectedCountCurrency,
                        onCurrencyChange = onCurrencyChange
                    )
                }
                //NotesCard(strings = strings)
            }
        }
    }
}

@Composable
private fun SystemSummaryCardMultiCurrency(
    summary: ReconciliationSummaryUi,
    expectedCashByCurrency: Map<String, Double>,
    salesByCurrency: Map<String, Double>,
    creditPartialByCurrency: Map<String, Double>,
    creditPendingByCurrency: Map<String, Double>,
    expensesByCurrency: Map<String, Double>,
    creditPartial: Double,
    creditPending: Double,
    formatAmountFor: (Double, String) -> String,
    strings: ReconciliationStrings
) {
    val currencies = expectedCashByCurrency.ifEmpty {
        mapOf(summary.currency.uppercase() to summary.expectedTotal)
    }
    val expectedCombined = currencies.entries.joinToString(" / ") { (code, amount) ->
        formatAmountFor(amount, code)
    }
    val salesBreakdown = buildCurrencySummaryLine(
        salesByCurrency.ifEmpty { mapOf(summary.currency.uppercase() to summary.salesTotal) },
        formatAmountFor
    )
    val creditPartialBreakdown = buildCurrencySummaryLine(
        creditPartialByCurrency.ifEmpty { mapOf(summary.currency.uppercase() to creditPartial) },
        formatAmountFor
    )
    val creditPendingBreakdown = buildCurrencySummaryLine(
        creditPendingByCurrency.ifEmpty { mapOf(summary.currency.uppercase() to creditPending) },
        formatAmountFor
    )
    val expensesBreakdown = buildCurrencySummaryLine(
        expensesByCurrency.ifEmpty { mapOf(summary.currency.uppercase() to summary.expensesTotal) },
        formatAmountFor
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            //horizontalAlignment = Alignment.End
        ) {
            Text(strings.systemSummaryTitle, style = MaterialTheme.typography.titleMedium)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        strings.expectedTotalLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        expectedCombined,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${strings.invoicesLabel}: ${summary.invoiceCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (summary.pendingSubmitCount > 0) {
                        Text(
                            "${strings.pendingSubmitLabel}: ${summary.pendingSubmitCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryRow(
                    label = strings.salesLabel,
                    value = salesBreakdown,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Filled.ShoppingCart
                )
                /*SummaryRow(
                    label = strings.paymentsLabel,
                    value = nonCashBreakdown,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Filled.ArrowDownward
                )*/
                SummaryRow(
                    label = strings.creditPartialLabel,
                    value = creditPartialBreakdown,
                    valueColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.ArrowDownward,
                    badgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                SummaryRow(
                    label = strings.creditPendingLabel,
                    value = creditPendingBreakdown,
                    valueColor = MaterialTheme.colorScheme.error,
                    icon = Icons.Filled.ArrowUpward,
                    badgeColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                )
                SummaryRow(
                    label = strings.expensesLabel,
                    value = expensesBreakdown,
                    valueColor = MaterialTheme.colorScheme.error,
                    icon = Icons.Filled.ArrowUpward,
                    badgeColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                )
            }
        }
    }
}

@Composable
private fun OpeningBalanceCard(
    summary: ReconciliationSummaryUi,
    formatAmount: (Double) -> String,
    formatAmountFor: (Double, String) -> String,
    strings: ReconciliationStrings
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
            if (summary.openingCashByCurrency.isNotEmpty()) {
                val openingLines = summary.openingCashByCurrency
                    .toList()
                    .sortedBy { it.first.uppercase() }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    openingLines.forEach { (code, amount) ->
                        Text(
                            formatAmountFor(amount, code),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    formatAmount(summary.openingAmount),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
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

private const val EPS = 0.01

private enum class BalanceState { DEFICIT, NEUTRAL, OK_OR_SURPLUS }

private fun stateForCurrency(counted: Double, expected: Double): BalanceState {
    val delta = counted - expected
    return when {
        delta < -EPS -> BalanceState.DEFICIT
        abs(delta) <= EPS && (abs(counted) <= EPS || abs(expected) <= EPS) -> BalanceState.NEUTRAL
        else -> BalanceState.OK_OR_SURPLUS
    }
}

private fun overallState(
    currencies: List<String>,
    countedByCurrency: Map<String, Double>,
    expectedByCurrency: Map<String, Double>
): BalanceState {
    var hasGreen = false

    for (code in currencies) {
        val counted = countedByCurrency[code] ?: 0.0
        val expected = expectedByCurrency[code] ?: 0.0

        when (stateForCurrency(counted, expected)) {
            BalanceState.DEFICIT -> return BalanceState.DEFICIT
            BalanceState.OK_OR_SURPLUS -> hasGreen = true
            BalanceState.NEUTRAL -> Unit
        }
    }
    return if (hasGreen) BalanceState.OK_OR_SURPLUS else BalanceState.NEUTRAL
}

@Composable
private fun DifferenceAlertsByCurrency(
    expectedByCurrency: Map<String, Double>,
    countedByCurrency: Map<String, Double>,
    strings: ReconciliationStrings
) {
    if (expectedByCurrency.isEmpty()) return

    // Normalizamos keys a uppercase para que coincidan al consultar los mapas.
    val expectedUp = expectedByCurrency.mapKeys { it.key.uppercase() }
    val countedUp = countedByCurrency.mapKeys { it.key.uppercase() }

    val currencies = (expectedUp.keys + countedUp.keys).distinct()

    val state = overallState(
        currencies = currencies,
        countedByCurrency = countedUp,
        expectedByCurrency = expectedUp
    )

    val cardColors = when (state) {
        BalanceState.DEFICIT ->
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))

        BalanceState.NEUTRAL ->
            // Azul/neutro: si tu theme no tiende a azul en secondary, cambia a surfaceVariant.
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))

        BalanceState.OK_OR_SURPLUS ->
            // Verde “conceptual”: en Material3 no siempre hay green. Si querés VERDE real,
            // necesitás un color custom. Por ahora uso primary como “ok”.
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
    }

    val (icon, iconTint, title) = when (state) {
        BalanceState.DEFICIT -> Triple(
            Icons.Filled.Warning,
            MaterialTheme.colorScheme.error,
            strings.differenceTitle
        )

        BalanceState.NEUTRAL -> Triple(
            Icons.Filled.Info,
            MaterialTheme.colorScheme.secondary,
            strings.differenceTitle // o un string tipo "Sin movimiento"
        )

        BalanceState.OK_OR_SURPLUS -> Triple(
            Icons.Filled.CheckCircle,
            MaterialTheme.colorScheme.primary,
            strings.differenceBalancedTitle
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val differenceSummary = currencies.joinToString(" / ") { code ->
                val expected = expectedUp[code] ?: 0.0
                val counted = countedUp[code] ?: 0.0
                val difference = counted - expected

                val label = when {
                    difference > EPS -> strings.differenceOverLabel
                    difference < -EPS -> strings.differenceShortLabel
                    else -> ""
                }

                val amount = abs(difference).formatCurrencyWithCode(code)
                if (label.isBlank()) amount else "$label $amount"
            }

            Text(
                "${strings.differenceLabel}: $differenceSummary",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReconciliationActionsBar(
    differenceByCurrency: Map<String, Double>,
    isClosing: Boolean,
    onClose: () -> Unit,
    onSaveDraft: () -> Unit,
    strings: ReconciliationStrings
) {
    val hasDifference = differenceByCurrency.values.any { abs(it) > 0.01 }
    val differenceSummary = buildCurrencySummaryLine(
        differenceByCurrency,
        formatAmountFor = { value, code ->
            formatCurrency(
                value = value,
                currencyCode = code,
                currencySymbol = code.toCurrencySymbol().ifBlank { code },
                formatter = DecimalFormatter()
            )
        }
    )
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
                        if (differenceSummary.isNotBlank()) {
                            Text(
                                differenceSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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
    icon: ImageVector,
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
private fun EmptyReconciliationState(strings: ReconciliationStrings) {
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

fun computeCreditPartial(summary: ReconciliationSummaryUi): Double {
    // Usa el total calculado por facturas del turno.
    return summary.creditPartialTotal
}

fun computeCreditAmounts(summary: ReconciliationSummaryUi): Pair<Double, Double> {
    // Separa pagos parciales de pendientes usando el resumen ya calculado.
    val creditPartial = computeCreditPartial(summary)
    val creditPending = summary.creditPendingTotal
    return creditPartial to creditPending
}

fun formatCurrency(
    value: Double,
    currencyCode: String,
    currencySymbol: String?,
    formatter: DecimalFormatter
): String {
    val prefix = currencySymbol?.takeIf { it.isNotBlank() } ?: currencyCode
    val formatted = formatter.format(value, 2, includeSeparator = true)
    return "$prefix $formatted"
}

fun Double.formatCurrencyWithCode(code: String): String {
    val formatter = DecimalFormatter()
    val symbol = code.toCurrencySymbol().ifBlank { code }
    return formatCurrency(this, code, symbol, formatter)
}

fun buildCurrencySummaryLine(
    values: Map<String, Double>,
    formatAmountFor: (Double, String) -> String
): String {
    if (values.isEmpty()) return ""
    return values.entries.joinToString(" / ") { (code, amount) ->
        formatAmountFor(amount, code)
    }
}

fun buildDifferenceByCurrency(
    expectedByCurrency: Map<String, Double>,
    countedByCurrency: Map<String, Double>
): Map<String, Double> {
    val codes = (expectedByCurrency.keys + countedByCurrency.keys).map { it.uppercase() }.distinct()
    return codes.associateWith { code ->
        val expected = expectedByCurrency[code] ?: 0.0
        val counted = countedByCurrency[code] ?: 0.0
        counted - expected
    }
}
