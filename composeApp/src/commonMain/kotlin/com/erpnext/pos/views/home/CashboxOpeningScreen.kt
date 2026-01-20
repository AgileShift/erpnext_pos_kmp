@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package com.erpnext.pos.views.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.erpnext.pos.domain.models.DenominationCount
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.models.OpeningSessionDraft
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.preferences.OpeningSessionPreferences
import com.erpnext.pos.utils.DecimalFormatter
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.PaymentModeWithAmount
import com.erpnext.pos.views.components.DenominationCounter
import com.erpnext.pos.views.components.DenominationCounterLabels
import com.erpnext.pos.views.components.DenominationUi
import com.erpnext.pos.views.components.buildDenominationsForCurrency
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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
    onOpenCashbox: suspend (POSProfileSimpleBO, List<PaymentModeWithAmount>) -> Unit,
    onDismiss: () -> Unit,
    snackbar: SnackbarController
) {
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<POSProfileSimpleBO?>(null) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf("") }
    var lastDraftKey by remember { mutableStateOf<String?>(null) }
    val denominationState = remember { androidx.compose.runtime.mutableStateMapOf<String, List<DenominationUi>>() }
    val openingSessionPreferences: OpeningSessionPreferences = org.koin.compose.koinInject()
    val modeOfPaymentDao: ModeOfPaymentDao = org.koin.compose.koinInject()
    val openingDraft by openingSessionPreferences.draft.collectAsState(initial = null)
    val formatter = remember { DecimalFormatter() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profiles) {
        if (profiles.size == 1) {
            selectedProfile = profiles.first()
            onSelectProfile(profiles.first())
            profileMenuExpanded = false
        }
    }

    LaunchedEffect(selectedProfile?.name) {
        selectedCurrency = ""
        denominationState.clear()
        lastDraftKey = null
    }

    val paymentModes = (uiState as? HomeState.POSInfoLoaded)?.info?.paymentModes.orEmpty()
    val baseCurrency = when (uiState) {
        is HomeState.POSInfoLoaded -> uiState.currency
        else -> profiles.firstOrNull()?.currency ?: "USD"
    }
    val normalizedBaseCurrency = normalizeCurrency(baseCurrency)
    var modeOfPaymentLookup by remember { mutableStateOf<Map<String, ModeOfPaymentEntity>>(emptyMap()) }

    LaunchedEffect(selectedProfile?.company) {
        val company = selectedProfile?.company ?: return@LaunchedEffect
        modeOfPaymentLookup = modeOfPaymentDao.getAllModes(company).associateBy {
            it.modeOfPayment.uppercase()
        }
    }

    val resolvedCashModes = remember(paymentModes, modeOfPaymentLookup, normalizedBaseCurrency) {
        resolveCashModes(paymentModes, modeOfPaymentLookup, normalizedBaseCurrency)
    }
    val cashModesByCurrency = resolvedCashModes.groupBy { it.currency }
    val cashModeByCurrency = cashModesByCurrency.mapValues { it.value.first() }
    val duplicateCashCurrencies = cashModesByCurrency.filterValues { it.size > 1 }.keys.toList()
    val cashModesMissingCurrency = resolvedCashModes.filter { it.entity?.currency.isNullOrBlank() }
    val openingCurrencies = remember(resolvedCashModes, normalizedBaseCurrency) {
        resolveOpeningCurrencies(resolvedCashModes, normalizedBaseCurrency)
    }

    LaunchedEffect(openingDraft, profiles, user?.email) {
        val userEmail = user?.email ?: return@LaunchedEffect
        val draftProfileId = openingDraft?.takeIf { it.user == userEmail }?.posProfileId
        if (draftProfileId != null && selectedProfile == null) {
            profiles.firstOrNull { it.name == draftProfileId }?.let { profile ->
                selectedProfile = profile
                onSelectProfile(profile)
                profileMenuExpanded = false
            }
        }
    }

    LaunchedEffect(openingCurrencies) {
        if (openingCurrencies.isNotEmpty() && selectedCurrency !in openingCurrencies) {
            selectedCurrency = openingCurrencies.first()
        }
        openingCurrencies.forEach { currency ->
            if (!denominationState.containsKey(currency)) {
                denominationState[currency] = buildDenominationsForCurrency(
                    currency = currency,
                    symbolOverride = currency.toCurrencySymbol(),
                    formatter = formatter
                )
            }
        }
    }

    LaunchedEffect(openingDraft, selectedProfile?.name, user?.email, openingCurrencies) {
        val profileId = selectedProfile?.name ?: return@LaunchedEffect
        val userEmail = user?.email ?: return@LaunchedEffect
        val draft = openingDraft?.takeIf { it.posProfileId == profileId && it.user == userEmail }
        val draftKey = draft?.let { "${it.posProfileId}-${it.user}" }
        if (draft != null && draftKey != null && draftKey != lastDraftKey) {
            openingCurrencies.forEach { currency ->
                val baseDenoms = buildDenominationsForCurrency(
                    currency = currency,
                    symbolOverride = currency.toCurrencySymbol(),
                    formatter = formatter
                )
                val updated = applyDraftCounts(
                    baseDenoms,
                    draft.denominationCounts[currency].orEmpty()
                )
                denominationState[currency] = updated
            }
            lastDraftKey = draftKey
        }
    }

    LaunchedEffect(selectedProfile?.name, user?.email, openingCurrencies) {
        val profileId = selectedProfile?.name ?: return@LaunchedEffect
        val userEmail = user?.email ?: return@LaunchedEffect
        kotlinx.coroutines.flow.snapshotFlow {
            denominationState.toMap().mapValues { (_, denoms) -> denoms.map { it.copy() } }
        }.debounce(500).collectLatest { current ->
            if (current.isEmpty()) return@collectLatest
            val totals = current.mapValues { entry ->
                entry.value.sumOf { it.value * it.count }
            }
            val counts = current.mapValues { entry ->
                entry.value.map { DenominationCount(value = it.value, count = it.count) }
            }
            openingSessionPreferences.saveDraft(
                OpeningSessionDraft(
                    posProfileId = profileId,
                    user = userEmail,
                    createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                    openingCashByCurrency = totals,
                    denominationCounts = counts
                )
            )
        }
    }

    val totalsByCurrency: Map<String, Double> = openingCurrencies.associateWith { currency ->
        denominationState[currency].orEmpty().sumOf { it.value * it.count }
    }
    val canOpen =
        selectedProfile != null &&
            uiState is HomeState.POSInfoLoaded &&
            resolvedCashModes.isNotEmpty() &&
            duplicateCashCurrencies.isEmpty()

    val handleOpen: () -> Unit = {
        val profile = selectedProfile
        if (profile == null) {
            scope.launch {
                snackbar.show(
                    "Selecciona un perfil de POS", SnackbarType.Error, SnackbarPosition.Top
                )
            }
        } else {
            val amountByMode = cashModeByCurrency.entries.associate { (currency, resolved) ->
                resolved.mode.name to (totalsByCurrency[currency] ?: 0.0)
            }
            val amounts = paymentModes.map { mode ->
                val amount = amountByMode[mode.name] ?: 0.0
                PaymentModeWithAmount(mode = mode, amount = amount)
            }
            isSubmitting = true
            scope.launch {
                val totalsMsg = totalsByCurrency.entries.joinToString(" · ") { (cur, total) ->
                    "${cur.toCurrencySymbol()} ${formatMoney(total)}"
                }
                try {
                    onOpenCashbox(profile, amounts)
                    openingSessionPreferences.clearDraft()
                    snackbar.show(
                        "Caja abierta: $totalsMsg", SnackbarType.Success, SnackbarPosition.Top
                    )
                    onDismiss()
                } catch (e: Exception) {
                    snackbar.show(
                        e.message ?: "Error al abrir caja",
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
                            totalsByCurrency = totalsByCurrency,
                            isLoading = isSubmitting || uiState is HomeState.POSInfoLoading,
                            canOpen = canOpen && !isSubmitting,
                            duplicateCashCurrencies = duplicateCashCurrencies,
                            cashModesMissingCurrency = cashModesMissingCurrency.map { it.mode.modeOfPayment },
                            onOpen = handleOpen,
                            onCancel = onDismiss
                        )

                        val activeCurrency = selectedCurrency.ifBlank { normalizedBaseCurrency }
                        OpeningCashContent(
                            countCurrencies = openingCurrencies,
                            selectedCurrency = activeCurrency,
                            denominations = denominationState[activeCurrency].orEmpty(),
                            onCurrencyChange = { selectedCurrency = it },
                            onDenominationChange = { value, count ->
                                val current = denominationState[activeCurrency].orEmpty()
                                denominationState[activeCurrency] = current.map { denom ->
                                    if (denom.value == value) denom.copy(count = max(0, count)) else denom
                                }
                            },
                            totalsByCurrency = totalsByCurrency
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
                                totalsByCurrency = totalsByCurrency,
                                isLoading = isSubmitting || uiState is HomeState.POSInfoLoading,
                                canOpen = canOpen && !isSubmitting,
                                duplicateCashCurrencies = duplicateCashCurrencies,
                                cashModesMissingCurrency = cashModesMissingCurrency.map { it.mode.modeOfPayment },
                                onOpen = handleOpen,
                                onCancel = onDismiss
                            )
                        }

                        Column(
                            modifier = Modifier.weight(8f).fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val activeCurrency = selectedCurrency.ifBlank { normalizedBaseCurrency }
                            OpeningCashContent(
                                countCurrencies = openingCurrencies,
                                selectedCurrency = activeCurrency,
                                denominations = denominationState[activeCurrency].orEmpty(),
                                onCurrencyChange = { selectedCurrency = it },
                                onDenominationChange = { value, count ->
                                    val current = denominationState[activeCurrency].orEmpty()
                                    denominationState[activeCurrency] = current.map { denom ->
                                        if (denom.value == value) denom.copy(count = max(0, count)) else denom
                                    }
                                },
                                totalsByCurrency = totalsByCurrency
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
    totalsByCurrency: Map<String, Double>,
    isLoading: Boolean,
    canOpen: Boolean,
    duplicateCashCurrencies: List<String>,
    cashModesMissingCurrency: List<String>,
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

            if (duplicateCashCurrencies.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Multiple cash modes are configured for the same currency: ${
                            duplicateCashCurrencies.joinToString(", ")
                        }",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (cashModesMissingCurrency.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "ASSUMPTION: these cash modes have no currency configured and use the base currency: ${
                            cashModesMissingCurrency.joinToString(", ")
                        }",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
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
private fun OpeningCashContent(
    countCurrencies: List<String>,
    selectedCurrency: String,
    denominations: List<DenominationUi>,
    onCurrencyChange: (String) -> Unit,
    onDenominationChange: (Double, Int) -> Unit,
    totalsByCurrency: Map<String, Double>,
) {
    SectionCard(title = "Conteo de efectivo") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DenominationCounter(
                denominations = denominations,
                onCountChange = onDenominationChange,
                total = totalsByCurrency[selectedCurrency] ?: 0.0,
                formatAmount = { amount ->
                    "${selectedCurrency.toCurrencySymbol()} ${formatMoney(amount)}"
                },
                labels = DenominationCounterLabels(
                    title = "Detalle por denominación",
                    subtitle = "billetes y monedas",
                    billsLabel = "Billetes",
                    coinsLabel = "Monedas",
                    totalLabel = "Total contado"
                ),
                countCurrencies = countCurrencies,
                selectedCountCurrency = selectedCurrency,
                onCurrencyChange = onCurrencyChange
            )

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

private fun applyDraftCounts(
    denominations: List<DenominationUi>,
    counts: List<DenominationCount>
): List<DenominationUi> {
    if (counts.isEmpty()) return denominations
    val countMap = counts.associate { it.value to it.count }
    return denominations.map { denom ->
        denom.copy(count = countMap[denom.value] ?: 0)
    }
}

private data class ResolvedCashMode(
    val mode: PaymentModesBO,
    val currency: String,
    val entity: ModeOfPaymentEntity?
)

private fun resolveOpeningCurrencies(
    cashModes: List<ResolvedCashMode>,
    baseCurrency: String
): List<String> {
    val normalizedBase = normalizeCurrency(baseCurrency)
    val currencies = cashModes.map { it.currency }.distinct()
    return if (currencies.isEmpty()) listOf(normalizedBase) else currencies
}

private fun resolveCashModes(
    paymentModes: List<PaymentModesBO>,
    modeOfPaymentLookup: Map<String, ModeOfPaymentEntity>,
    baseCurrency: String
): List<ResolvedCashMode> {
    return paymentModes.mapNotNull { mode ->
        val entity = resolveModeEntity(mode, modeOfPaymentLookup)
        val isCash = entity?.type?.equals("Cash", ignoreCase = true)
            ?: (categorizePaymentMode(mode) == PaymentCategory.CASH)
        if (!isCash) return@mapNotNull null
        // ASSUMPTION: when currency is missing on the Mode of Payment, use the POS base currency.
        val currency = normalizeCurrency(entity?.currency ?: baseCurrency)
        ResolvedCashMode(mode = mode, currency = currency, entity = entity)
    }
}

private fun resolveModeEntity(
    mode: PaymentModesBO,
    modeOfPaymentLookup: Map<String, ModeOfPaymentEntity>
): ModeOfPaymentEntity? {
    val key = mode.modeOfPayment.uppercase()
    return modeOfPaymentLookup[key]
        ?: modeOfPaymentLookup[mode.name.uppercase()]
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
