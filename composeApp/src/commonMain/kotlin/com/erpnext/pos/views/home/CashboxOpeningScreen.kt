@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package com.erpnext.pos.views.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.utils.CashDenomination
import com.erpnext.pos.utils.availableCurrencies
import com.erpnext.pos.utils.currencyChoices
import com.erpnext.pos.utils.denominationsFor
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.PaymentModeWithAmount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private enum class PaymentCategory { CASH, OTHER }

@Composable
fun CashboxOpeningScreen(
    uiState: HomeState,
    profiles: List<POSProfileSimpleBO>,
    user: UserBO?,
    onSelectProfile: (POSProfileSimpleBO) -> Unit,
    onOpenCashbox: (POSProfileSimpleBO, List<PaymentModeWithAmount>) -> Unit,
    onDismiss: () -> Unit,
    snackbar: SnackbarController
) {
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<POSProfileSimpleBO?>(null) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var cashQuantities by remember { mutableStateOf<Map<String, Map<Double, Int>>>(emptyMap()) }
    var modeCurrencies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profiles) {
        if (profiles.size == 1) {
            selectedProfile = profiles.first()
            onSelectProfile(profiles.first())
            profileMenuExpanded = false
        }
    }

    val paymentModes = (uiState as? HomeState.POSInfoLoaded)?.info?.paymentModes.orEmpty()
    val baseCurrency = when (uiState) {
        is HomeState.POSInfoLoaded -> uiState.currency
        else -> profiles.firstOrNull()?.currency ?: "USD"
    }
    val normalizedBaseCurrency = normalizeCurrency(baseCurrency)
    val currencySymbol = normalizedBaseCurrency.toCurrencySymbol()
    val availableCurrencies = availableCurrencies(normalizedBaseCurrency, paymentModes)

    val categorized = paymentModes.groupBy { categorizePaymentMode(it) }
    val cashModes = categorized[PaymentCategory.CASH].orEmpty()

    val cashTotalsByMode: Map<String, Double> = cashModes.associate { mode ->
        val currency = normalizeCurrency(modeCurrencies[mode.name] ?: baseCurrency)
        val denoms = denominationsFor(currency)
        val perMode = cashQuantities[mode.name].orEmpty()
        val total = denoms.sumOf { denom -> denom.value * (perMode[denom.value] ?: 0) }
        mode.name to total
    }
    val totalsByCurrency: Map<String, Double> =
        cashModes.groupBy { mode -> normalizeCurrency(modeCurrencies[mode.name] ?: baseCurrency) }
            .mapValues { (cur, modes) ->
                modes.sumOf { m -> cashTotalsByMode[m.name] ?: 0.0 }
            }
    val cashTotal = totalsByCurrency[normalizedBaseCurrency] ?: cashTotalsByMode.values.sum()
    val canOpen =
        selectedProfile != null && uiState is HomeState.POSInfoLoaded && cashModes.isNotEmpty()

    val handleOpen: () -> Unit = {
        val profile = selectedProfile
        if (profile == null) {
            scope.launch {
                snackbar.show(
                    "Selecciona un perfil de POS", SnackbarType.Error, SnackbarPosition.Top
                )
            }
        } else {
            val amounts = paymentModes.map { mode ->
                val amount = when (categorizePaymentMode(mode)) {
                    PaymentCategory.CASH -> cashTotalsByMode[mode.name] ?: 0.0
                    PaymentCategory.OTHER -> 0.0
                }
                PaymentModeWithAmount(mode = mode, amount = amount)
            }
            isSubmitting = true
            scope.launch {
                val totalsMsg = totalsByCurrency.entries.joinToString(" · ") { (cur, total) ->
                    "${cur.toCurrencySymbol()} ${formatMoney(total)}"
                }
                runCatching { onOpenCashbox(profile, amounts) }.onSuccess {
                    snackbar.show(
                        "Caja abierta: $totalsMsg", SnackbarType.Success, SnackbarPosition.Top
                    )
                    onDismiss()
                }.onFailure {
                    snackbar.show(
                        it.message ?: "Error al abrir caja",
                        SnackbarType.Error,
                        SnackbarPosition.Top
                    )
                }
                isSubmitting = false
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 900.dp

            Column(modifier = Modifier.fillMaxSize()) {
                OpeningHeader(onDismiss = onDismiss)

                val bottomPadding = if (isCompact) 96.dp else 24.dp

                if (isCompact) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .padding(bottom = bottomPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OpeningFormSection(
                            user = user,
                            profiles = profiles,
                            selectedProfile = selectedProfile,
                            onSelectProfile = {
                                selectedProfile = it
                                onSelectProfile(it)
                            },
                            expanded = profileMenuExpanded,
                            onExpandedChange = { profileMenuExpanded = it },
                            currencySymbol = currencySymbol,
                            totalsByCurrency = totalsByCurrency,
                            isLoading = isSubmitting || uiState is HomeState.POSInfoLoading,
                            canOpen = canOpen && !isSubmitting,
                            onOpen = handleOpen,
                            onCancel = onDismiss
                        )

                        CashContent(
                            baseCurrency = normalizedBaseCurrency,
                            currencies = availableCurrencies,
                            cashModes = cashModes,
                            modeCurrencies = modeCurrencies,
                            onCurrencyChange = { modeName, cur ->
                                modeCurrencies =
                                    modeCurrencies.toMutableMap().apply { this[modeName] = cur }
                                cashQuantities = cashQuantities.toMutableMap()
                                    .apply { this[modeName] = emptyMap() }
                            },
                            cashQuantities = cashQuantities,
                            onDenominationChange = { modeName, value, quantity ->
                                cashQuantities = cashQuantities.toMutableMap().apply {
                                    val current = this[modeName].orEmpty()
                                    this[modeName] = current + (value to max(0, quantity))
                                }
                            },
                            cashTotalsByMode = cashTotalsByMode,
                            aggregateTotal = cashTotal
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .padding(bottom = bottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(4f).fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OpeningFormSection(
                                user = user,
                                profiles = profiles,
                                selectedProfile = selectedProfile,
                                onSelectProfile = {
                                    selectedProfile = it
                                    onSelectProfile(it)
                                },
                                expanded = profileMenuExpanded,
                                onExpandedChange = { profileMenuExpanded = it },
                                currencySymbol = currencySymbol,
                                totalsByCurrency = totalsByCurrency,
                                isLoading = isSubmitting || uiState is HomeState.POSInfoLoading,
                                canOpen = canOpen && !isSubmitting,
                                onOpen = handleOpen,
                                onCancel = onDismiss
                            )
                        }

                        Column(
                            modifier = Modifier.weight(8f).fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CashContent(
                                baseCurrency = normalizedBaseCurrency,
                                currencies = availableCurrencies,
                                cashModes = cashModes,
                                modeCurrencies = modeCurrencies,
                                onCurrencyChange = { modeName, cur ->
                                    modeCurrencies =
                                        modeCurrencies.toMutableMap().apply { this[modeName] = cur }
                                    cashQuantities = cashQuantities.toMutableMap()
                                        .apply { this[modeName] = emptyMap() }
                                },
                                cashQuantities = cashQuantities,
                                onDenominationChange = { modeName, value, quantity ->
                                    cashQuantities = cashQuantities.toMutableMap().apply {
                                        val current = this[modeName].orEmpty()
                                        this[modeName] = current + (value to max(0, quantity))
                                    }
                                },
                                cashTotalsByMode = cashTotalsByMode,
                                aggregateTotal = cashTotal
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                snackbar = snackbar.snackbar.collectAsState().value,
                onDismiss = { snackbar.dismiss() },
                modifier = Modifier.fillMaxSize()
            )

            if (isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun OpeningHeader(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = "Apertura de caja",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Nueva entrada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun OpeningFormSection(
    user: UserBO?,
    profiles: List<POSProfileSimpleBO>,
    selectedProfile: POSProfileSimpleBO?,
    onSelectProfile: (POSProfileSimpleBO) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    currencySymbol: String,
    totalsByCurrency: Map<String, Double>,
    isLoading: Boolean,
    canOpen: Boolean,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
) {
    SectionCard(title = "Detalles de Apertura") {
        val nowInstant by produceState(initialValue = Clock.System.now()) {
            while (true) {
                value = Clock.System.now()
                delay(1000)
            }
        }
        val now = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val outline = MaterialTheme.colorScheme.outlineVariant
        var notes by remember { mutableStateOf("") }
        val cashierName = listOfNotNull(
            user?.firstName?.takeIf { it.isNotBlank() },
            user?.lastName?.takeIf { !it.isNullOrBlank() }
        ).joinToString(" ").ifBlank { user?.name ?: "" }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, outline.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Fecha", style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                            ), color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${dayName(now.dayOfWeek)} ${now.dayOfMonth} ${monthName(now.monthNumber)} ${now.year}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Hora", style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                            ), color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${now.hour.toString().padStart(2, '0')}:${
                                now.minute.toString().padStart(2, '0')
                            }",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
            ) {
                OutlinedTextField(
                    value = selectedProfile?.name ?: "Seleccionar POS",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    label = { Text("Perfil de POS") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    profiles.forEach { profile ->
                        androidx.compose.material3.DropdownMenuItem(text = {
                            Column {
                                Text(profile.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    profile.company,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }, onClick = {
                            onSelectProfile(profile)
                            onExpandedChange(false)
                        })
                    }
                }
            }

            OutlinedTextField(
                value = cashierName,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cajero / Usuario") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                readOnly = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notas / Observaciones") },
                placeholder = { Text("Detalles adicionales para la apertura") },
                singleLine = false,
                maxLines = 3
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Resumen de Apertura", style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                    ), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                totalsByCurrency.forEach { (cur, total) ->
                    SummaryRow(
                        icon = Icons.Outlined.Wallet,
                        label = "Efectivo $cur",
                        value = "${cur.toCurrencySymbol()} ${formatMoney(total)}"
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Apertura",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        totalsByCurrency.forEach { (cur, total) ->
                            Text(
                                text = "${cur.toCurrencySymbol()} ${formatMoney(total)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onOpen,
                    enabled = canOpen,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Abrir Caja", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Apertura solo en efectivo. Si tienes otros métodos, regístralos luego como pagos.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun CashContent(
    baseCurrency: String,
    currencies: List<String>,
    cashModes: List<PaymentModesBO>,
    modeCurrencies: Map<String, String>,
    onCurrencyChange: (String, String) -> Unit,
    cashQuantities: Map<String, Map<Double, Int>>,
    onDenominationChange: (String, Double, Int) -> Unit,
    cashTotalsByMode: Map<String, Double>,
    aggregateTotal: Double,
) {
    SectionCard(title = "Desglose de efectivo") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cashModes.forEach { mode ->
                val selectedCurrency = normalizeCurrency(modeCurrencies[mode.name] ?: baseCurrency)
                val choices = currencyChoices(baseCurrency, currencies)
                val denoms = denominationsFor(selectedCurrency)
                val quantities = cashQuantities[mode.name].orEmpty()
                val total = cashTotalsByMode[mode.name] ?: 0.0
                SectionCard(title = mode.modeOfPayment) {
                    if (choices.size > 1) {
                        TabRow(
                            selectedTabIndex = choices.indexOf(selectedCurrency).coerceAtLeast(0),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            choices.forEachIndexed { idx, cur ->
                                Tab(
                                    selected = cur == selectedCurrency,
                                    onClick = { onCurrencyChange(mode.name, cur) },
                                    text = {
                                        Text(
                                            cur,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    DenominationTableModern(
                        modeName = mode.name,
                        quantities = quantities,
                        onQuantityChange = onDenominationChange,
                        currencySymbol = selectedCurrency.toCurrencySymbol(),
                        denoms = denoms
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total ${mode.modeOfPayment}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${selectedCurrency.toCurrencySymbol()} ${formatMoney(total)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    Text(
                        text = "Total efectivo por moneda",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    val totalsByCurrency = cashTotalsByMode.entries.groupBy { modeName ->
                        val mode = cashModes.firstOrNull { it.name == modeName.key }
                        val cur = normalizeCurrency(modeCurrencies[mode?.name] ?: baseCurrency)
                        cur
                    }.mapValues { entry -> entry.value.sumOf { cashTotalsByMode[it.key] ?: 0.0 } }

                    totalsByCurrency.forEach { (cur, total) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cur,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${cur.toCurrencySymbol()} ${formatMoney(total)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun DenominationTableModern(
    modeName: String,
    quantities: Map<Double, Int>,
    onQuantityChange: (String, Double, Int) -> Unit,
    currencySymbol: String,
    denoms: List<CashDenomination>,
) {
    val bills = denoms.filter { it.value >= 1.0 }
    val coins = denoms.filter { it.value < 1.0 }

    @Composable
    fun renderGroup(title: String, items: List<CashDenomination>) {
        if (items.isEmpty()) return
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { denom ->
                val quantity = quantities[denom.value] ?: 0
                val animated = animateIntAsState(quantity)
                val subtotal = denom.value * quantity
                Row(
                    modifier = Modifier.fillMaxWidth().background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp)
                    ).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$currencySymbol${denom.label}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "$currencySymbol ${formatDoubleToString(subtotal, 2)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onQuantityChange(
                                    modeName,
                                    denom.value,
                                    max(0, quantity - 1)
                                )
                            },
                            modifier = Modifier.size(28.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Disminuir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = animated.value.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onQuantityChange(modeName, denom.value, quantity + 1) },
                            modifier = Modifier.size(28.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Aumentar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        renderGroup("Billetes", bills)
        if (bills.isNotEmpty() && coins.isNotEmpty()) {
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
        renderGroup("Monedas", coins)
    }
}

private fun formatMoney(amount: Double): String = formatDoubleToString(amount, 2)

private fun categorizePaymentMode(mode: PaymentModesBO): PaymentCategory {
    val key = mode.modeOfPayment.lowercase()
    return when {
        key.contains("cash") || key.contains("efectivo") -> PaymentCategory.CASH
        else -> PaymentCategory.OTHER
    }
}

private fun monthName(month: Int): String {
    return when (month) {
        1 -> "enero"
        2 -> "febrero"
        3 -> "marzo"
        4 -> "abril"
        5 -> "mayo"
        6 -> "junio"
        7 -> "julio"
        8 -> "agosto"
        9 -> "septiembre"
        10 -> "octubre"
        11 -> "noviembre"
        else -> "diciembre"
    }
}

private fun dayName(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "lunes"
        DayOfWeek.TUESDAY -> "martes"
        DayOfWeek.WEDNESDAY -> "miércoles"
        DayOfWeek.THURSDAY -> "jueves"
        DayOfWeek.FRIDAY -> "viernes"
        DayOfWeek.SATURDAY -> "sábado"
        DayOfWeek.SUNDAY -> "domingo"
    }
}
