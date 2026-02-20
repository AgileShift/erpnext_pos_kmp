@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.settings

import AppColorTheme
import AppThemeMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.domain.models.ReturnDestinationPolicy
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlin.math.ceil

@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun SettingsScreenPreview() {
    PosSettingsScreen(
        POSSettingState.Success(
            settings = POSSettingBO(
                company = "Clothing Center",
                posProfile = "Main",
                openingEntryId = "POS-OPE-2026-00001",
                warehouse = "Almacén Principal",
                priceList = "Standard Price List",
                taxesIncluded = false,
                offlineMode = true,
                printerEnabled = true,
                cashDrawerEnabled = true,
                allowNegativeStock = false
            ),
            hasContext = true,
            syncSettings = SyncSettings(
                autoSync = true,
                syncOnStartup = true,
                wifiOnly = false,
                lastSyncAt = null,
                useTtl = false,
                ttlHours = 6
            ),
            syncState = SyncState.IDLE,
            language = AppLanguage.Spanish,
            theme = AppColorTheme.Noir,
            themeMode = AppThemeMode.System,
            returnPolicy = com.erpnext.pos.domain.models.ReturnPolicySettings(),
            inventoryAlertsEnabled = true,
            inventoryAlertHour = 9,
            inventoryAlertMinute = 0,
            salesTargetMonthly = 12000.0,
            salesTargetWeekly = 2775.0,
            salesTargetDaily = 400.0,
            salesTargetBaseCurrency = "USD",
            salesTargetSecondaryCurrency = "NIO",
            salesTargetConvertedMonthly = 450000.0,
            salesTargetConvertedWeekly = 104000.0,
            salesTargetConvertedDaily = 15000.0,
            salesTargetConversionStale = false,
            syncLog = emptyList()
        ),
        POSSettingAction()
    )
}

@Composable
fun PosSettingsScreen(
    state: POSSettingState,
    action: POSSettingAction
) {
    val snackbar = koinInject<SnackbarController>()
    val networkMonitor = koinInject<NetworkMonitor>()
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()
    val isOnline by networkMonitor.isConnected.collectAsState(false)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isCompact = maxWidth < 640.dp
        val contentPadding = if (isCompact) 12.dp else 16.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            when (state) {
                is POSSettingState.Success -> {
                    var showAlertTimeDialog by remember { mutableStateOf(false) }
                    var showSalesTargetDialog by remember { mutableStateOf(false) }
                    var showReturnDaysDialog by remember { mutableStateOf(false) }
                    var showReturnDestinationDialog by remember { mutableStateOf(false) }
                    var showTtlHoursDialog by remember { mutableStateOf(false) }

                    if (showAlertTimeDialog) {
                        InventoryAlertTimeDialog(
                            initialHour = state.inventoryAlertHour,
                            initialMinute = state.inventoryAlertMinute,
                            onDismiss = { showAlertTimeDialog = false },
                            onConfirm = { hour, minute ->
                                showAlertTimeDialog = false
                                action.onInventoryAlertTimeChanged(hour, minute)
                            }
                        )
                    }
                    if (showSalesTargetDialog) {
                        SalesTargetDialog(
                            initialValue = state.salesTargetMonthly,
                            baseCurrency = state.salesTargetBaseCurrency,
                            onDismiss = { showSalesTargetDialog = false },
                            onConfirm = { value ->
                                showSalesTargetDialog = false
                                action.onSalesTargetChanged(value)
                            }
                        )
                    }
                    if (showReturnDaysDialog) {
                        ReturnPolicyDaysDialog(
                            initialValue = state.returnPolicy.maxDaysAfterInvoice,
                            onDismiss = { showReturnDaysDialog = false },
                            onConfirm = { value ->
                                showReturnDaysDialog = false
                                action.onReturnPolicyChanged(
                                    state.returnPolicy.copy(maxDaysAfterInvoice = value)
                                )
                            }
                        )
                    }
                    if (showReturnDestinationDialog) {
                        ReturnPolicyDestinationDialog(
                            current = state.returnPolicy.defaultDestination,
                            allowRefunds = state.returnPolicy.allowRefunds,
                            onDismiss = { showReturnDestinationDialog = false },
                            onConfirm = { value ->
                                showReturnDestinationDialog = false
                                action.onReturnPolicyChanged(
                                    state.returnPolicy.copy(defaultDestination = value)
                                )
                            }
                        )
                    }
                    if (showTtlHoursDialog) {
                        TtlHoursDialog(
                            initialValue = state.syncSettings.ttlHours,
                            onDismiss = { showTtlHoursDialog = false },
                            onConfirm = { value ->
                                showTtlHoursDialog = false
                                action.onTtlHoursChanged(value)
                            }
                        )
                    }

                    SettingsOverviewRow(
                        settings = state.settings,
                        syncSettings = state.syncSettings,
                        syncState = state.syncState,
                        onSyncNow = action.onSyncNow,
                        onCancelSync = action.onCancelSync,
                        compact = isCompact,
                        isOnline = isOnline
                    )

                    if (!state.hasContext) {
                        MissingContextBanner()
                    }

                    SyncSection(
                        syncSettings = state.syncSettings,
                        onAutoSyncChanged = action.onAutoSyncChanged,
                        onSyncOnStartupChanged = action.onSyncOnStartupChanged,
                        onWifiOnlyChanged = action.onWifiOnlyChanged,
                        onUseTtlChanged = action.onUseTtlChanged,
                        onTtlHoursClick = { showTtlHoursDialog = true },
                        compact = isCompact
                    )

                    SettingSection(
                        title = strings.settings.operationTitle,
                        icon = Icons.Outlined.Tune,
                        compact = isCompact
                    ) {
                        SettingToggle(
                            label = strings.settings.taxesIncludedLabel,
                            checked = state.settings.taxesIncluded,
                            onCheckedChange = action.onTaxesIncludedChanged,
                            compact = isCompact
                        )
                        SettingToggle(
                            label = strings.settings.offlineModeLabel,
                            checked = state.settings.offlineMode,
                            onCheckedChange = action.onOfflineModeChanged,
                            supportingText = strings.settings.offlineModeHelp,
                            compact = isCompact
                        )
                        SettingToggle(
                            label = "Permitir venta con stock negativo",
                            checked = state.settings.allowNegativeStock,
                            onCheckedChange = {},
                            enabled = false,
                            supportingText = "Controlado por ERPNext (Desk).",
                            compact = isCompact,
                            showDivider = false
                        )
                    }

                    ExpandableSection(
                        title = "Política de devoluciones",
                        description = "Se aplica localmente y alinea los retornos con ERPNext v16.",
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        compact = isCompact,
                        initiallyExpanded = false
                    ) {
                        SettingItem(
                            label = "Destino por defecto",
                            value = if (state.returnPolicy.defaultDestination == ReturnDestinationPolicy.REFUND) {
                                "Reembolso"
                            } else {
                                "Crédito a favor"
                            },
                            onClick = { showReturnDestinationDialog = true },
                            compact = isCompact
                        )
                        SettingItem(
                            label = "Límite de días para retornar",
                            value = if (state.returnPolicy.maxDaysAfterInvoice <= 0) {
                                "Sin límite"
                            } else {
                                "${state.returnPolicy.maxDaysAfterInvoice} días"
                            },
                            onClick = { showReturnDaysDialog = true },
                            compact = isCompact
                        )
                        SettingToggle(
                            label = "Permitir reembolsos",
                            checked = state.returnPolicy.allowRefunds,
                            onCheckedChange = { enabled ->
                                val destination = if (enabled) {
                                    state.returnPolicy.defaultDestination
                                } else {
                                    ReturnDestinationPolicy.CREDIT
                                }
                                action.onReturnPolicyChanged(
                                    state.returnPolicy.copy(
                                        allowRefunds = enabled,
                                        defaultDestination = destination
                                    )
                                )
                            },
                            compact = isCompact
                        )
                        SettingToggle(
                            label = "Reembolso solo con factura pagada",
                            checked = state.returnPolicy.requirePaidInvoiceForRefund,
                            onCheckedChange = { enabled ->
                                action.onReturnPolicyChanged(
                                    state.returnPolicy.copy(requirePaidInvoiceForRefund = enabled)
                                )
                            },
                            enabled = state.returnPolicy.allowRefunds,
                            compact = isCompact
                        )
                        SettingToggle(
                            label = "Permitir retornos parciales",
                            checked = state.returnPolicy.allowPartialReturns,
                            onCheckedChange = { enabled ->
                                action.onReturnPolicyChanged(
                                    state.returnPolicy.copy(allowPartialReturns = enabled)
                                )
                            },
                            compact = isCompact
                        )
                        SettingToggle(
                            label = "Permitir retornos totales",
                            checked = state.returnPolicy.allowFullReturns,
                            onCheckedChange = { enabled ->
                                action.onReturnPolicyChanged(
                                    state.returnPolicy.copy(allowFullReturns = enabled)
                                )
                            },
                            compact = isCompact
                        )
                        SettingToggle(
                            label = "Requerir motivo",
                            checked = state.returnPolicy.requireReason,
                            onCheckedChange = { enabled ->
                                action.onReturnPolicyChanged(
                                    state.returnPolicy.copy(requireReason = enabled)
                                )
                            },
                            compact = isCompact,
                            showDivider = false
                        )
                    }

                    SettingSection(
                        title = strings.settings.inventoryAlertsTitle,
                        description = strings.settings.inventoryAlertsTimeHint,
                        icon = Icons.Outlined.Notifications,
                        compact = isCompact
                    ) {
                        SettingToggle(
                            label = strings.settings.inventoryAlertsEnabledLabel,
                            checked = state.inventoryAlertsEnabled,
                            onCheckedChange = action.onInventoryAlertsEnabledChanged,
                            compact = isCompact
                        )
                        SettingItem(
                            label = strings.settings.inventoryAlertsTimeLabel,
                            value = formatTime(state.inventoryAlertHour, state.inventoryAlertMinute),
                            onClick = { showAlertTimeDialog = true },
                            enabled = state.inventoryAlertsEnabled,
                            compact = isCompact,
                            showDivider = false
                        )
                    }

                    ExpandableSection(
                        title = strings.settings.salesTargetTitle,
                        description = strings.settings.salesTargetHint,
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        compact = isCompact,
                        initiallyExpanded = false
                    ) {
                        SettingItem(
                            label = strings.settings.salesTargetEditLabel,
                            value = formatCurrency(
                                state.salesTargetBaseCurrency,
                                state.salesTargetMonthly
                            ),
                            onClick = { showSalesTargetDialog = true },
                            compact = isCompact,
                            showDivider = false
                        )
                        TargetRow(
                            label = strings.settings.salesTargetMonthlyLabel,
                            baseCurrency = state.salesTargetBaseCurrency,
                            baseAmount = state.salesTargetMonthly,
                            secondaryCurrency = state.salesTargetSecondaryCurrency,
                            secondaryAmount = state.salesTargetConvertedMonthly
                        )
                        TargetRow(
                            label = strings.settings.salesTargetWeeklyLabel,
                            baseCurrency = state.salesTargetBaseCurrency,
                            baseAmount = state.salesTargetWeekly,
                            secondaryCurrency = state.salesTargetSecondaryCurrency,
                            secondaryAmount = state.salesTargetConvertedWeekly
                        )
                        TargetRow(
                            label = strings.settings.salesTargetDailyLabel,
                            baseCurrency = state.salesTargetBaseCurrency,
                            baseAmount = state.salesTargetDaily,
                            secondaryCurrency = state.salesTargetSecondaryCurrency,
                            secondaryAmount = state.salesTargetConvertedDaily
                        )
                        if (state.salesTargetSecondaryCurrency != null &&
                            state.salesTargetConvertedMonthly == null
                        ) {
                            Text(
                                text = strings.settings.salesTargetConversionMissingHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (state.salesTargetConversionStale) {
                            Text(
                                text = strings.settings.salesTargetConversionStaleHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Button(
                            onClick = action.onSyncSalesTarget,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(top = if (isCompact) 6.dp else 10.dp)
                        ) {
                            Text(strings.settings.salesTargetSyncLabel)
                        }
                    }

                    SettingSection(
                        title = strings.settings.hardwareTitle,
                        icon = Icons.Outlined.Print,
                        compact = isCompact
                    ) {
                        SettingToggle(
                            label = strings.settings.printerEnabledLabel,
                            checked = state.settings.printerEnabled,
                            onCheckedChange = action.onPrinterEnabledChanged,
                            compact = isCompact
                        )
                        SettingToggle(
                            label = strings.settings.cashDrawerEnabledLabel,
                            checked = state.settings.cashDrawerEnabled,
                            onCheckedChange = action.onCashDrawerEnabledChanged,
                            compact = isCompact,
                            showDivider = false
                        )
                    }

                    SettingSection(
                        title = strings.settings.languageTitle,
                        description = strings.settings.languageInstantHint,
                        icon = Icons.Outlined.Language,
                        compact = isCompact
                    ) {
                        LanguageSelector(
                            currentLanguage = state.language,
                            onLanguageSelected = action.onLanguageSelected,
                            compact = isCompact
                        )
                        ThemeChipSelector(
                            currentTheme = state.theme,
                            onThemeSelected = action.onThemeSelected
                        )
                        ThemeModeChipSelector(
                            currentMode = state.themeMode,
                            onModeSelected = action.onThemeModeSelected
                        )
                    }

                    SyncLogSection(
                        entries = state.syncLog,
                        compact = isCompact
                    )
                }

                is POSSettingState.Loading -> {
                    Spacer(modifier = Modifier.height(120.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is POSSettingState.Error -> {
                    snackbar.show(state.message, SnackbarType.Error, SnackbarPosition.Top)
                }
            }
        }
    }
}

@Composable
private fun SettingsOverviewRow(
    settings: POSSettingBO,
    syncSettings: SyncSettings,
    syncState: SyncState,
    onSyncNow: () -> Unit,
    onCancelSync: () -> Unit,
    compact: Boolean,
    isOnline: Boolean
) {
    BoxWithConstraints {
        val isWide = maxWidth > 780.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsInfoCard(settings = settings, modifier = Modifier.weight(0.42f), compact = compact)
                SettingsHeroCard(
                    syncSettings = syncSettings,
                    syncState = syncState,
                    onSyncNow = onSyncNow,
                    onCancelSync = onCancelSync,
                    modifier = Modifier.weight(0.65f),
                    compact = compact,
                    isOnline = isOnline,
                    offlineMode = settings.offlineMode
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsInfoCard(settings = settings, compact = compact)
                SettingsHeroCard(
                    syncSettings = syncSettings,
                    syncState = syncState,
                    onSyncNow = onSyncNow,
                    onCancelSync = onCancelSync,
                    compact = compact,
                    isOnline = isOnline,
                    offlineMode = settings.offlineMode
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(
    settings: POSSettingBO,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    val strings = LocalAppStrings.current
    val tokens = settingsTokens()
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = tokens.cardContainer
        ),
    ) {
        Column(modifier = Modifier.padding(if (compact) 14.dp else 18.dp)) {
            Text(
                text = "Contexto POS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp
                ),
                color = tokens.titleColor
            )
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
            SummaryGrid(
                items = listOf(
                    SummaryField(strings.settings.companyLabel, settings.company),
                    SummaryField(strings.settings.posProfileLabel, settings.posProfile),
                    SummaryField("Apertura (POE)", settings.openingEntryId),
                    SummaryField(strings.settings.warehouseLabel, settings.warehouse),
                    SummaryField(strings.settings.priceListLabel, settings.priceList)
                )
            )
        }
    }
}

@Composable
private fun SettingsHeroCard(
    syncSettings: SyncSettings,
    syncState: SyncState,
    onSyncNow: () -> Unit,
    onCancelSync: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean,
    isOnline: Boolean,
    offlineMode: Boolean
) {
    val tokens = settingsTokens()
    val gradient = Brush.verticalGradient(
        colors = listOf(
            tokens.heroPrimary,
            tokens.heroSecondary
        )
    )
    val strings = LocalAppStrings.current
    val statusLabel = when (syncState) {
        SyncState.IDLE -> strings.settings.syncStatusIdle
        SyncState.SUCCESS -> strings.settings.syncStatusSuccess
        is SyncState.ERROR -> strings.settings.syncStatusError
        is SyncState.SYNCING -> strings.settings.syncStatusSyncing
    }
    val statusStyle = when (syncState) {
        SyncState.IDLE -> StatusStyle(
            container = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
        SyncState.SUCCESS -> StatusStyle(
            container = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
        is SyncState.ERROR -> StatusStyle(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
        is SyncState.SYNCING -> StatusStyle(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .graphicsLayer { clip = true }
                .padding(if (compact) 14.dp else 18.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.settings.syncTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.heroText
                    )
                    StatusPill(
                        text = statusLabel,
                        containerColor = statusStyle.container,
                        contentColor = statusStyle.content
                    )
                }
                Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        text = if (isOnline) "Conectado" else "Sin conexión",
                        containerColor = if (isOnline) {
                            tokens.heroText.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        contentColor = if (isOnline) {
                            tokens.heroText
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    StatusPill(
                        text = if (offlineMode) "Modo offline activo" else "Modo offline: off",
                        containerColor = tokens.heroText.copy(alpha = 0.12f),
                        contentColor = tokens.heroText
                    )
                }
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
                Text(
                    text = syncSettings.lastSyncAt?.toErpDateTime()
                        ?: strings.settings.lastSyncNever,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.heroText
                )
                Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
                Button(
                    onClick = onSyncNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.heroButton,
                        contentColor = tokens.heroButtonText
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.settings.syncNowButton)
                }
                if (syncState is SyncState.SYNCING) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onCancelSync,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = tokens.heroButtonText
                        )
                    ) {
                        Text(strings.settings.syncCancelButton)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = settingsTokens()
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = tokens.cardContainer
        ),
    ) {
        Column(modifier = Modifier.padding(if (compact) 14.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tokens.iconTint
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.titleColor
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.subtleText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    compact: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = settingsTokens()
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = tokens.cardContainer
        ),
    ) {
        Column(modifier = Modifier.padding(if (compact) 14.dp else 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tokens.iconTint
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.titleColor
                    )
                    if (description != null && expanded) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.subtleText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = tokens.iconTint
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = if (compact) 10.dp else 12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    compact: Boolean = false,
    showDivider: Boolean = true
) {
    val tokens = settingsTokens()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = if (compact) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    color = tokens.titleColor
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        tokens.valueColor
                    } else {
                        tokens.mutedText
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) {
                    tokens.accent
                } else {
                    tokens.mutedText
                }
            )
        }
        if (showDivider) {
            HorizontalDivider(color = tokens.divider)
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
    compact: Boolean = false,
    showDivider: Boolean = true
) {
    val tokens = settingsTokens()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(vertical = if (compact) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    color = tokens.titleColor
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.subtleText
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = tokens.accent,
                    uncheckedThumbColor = tokens.titleColor
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(color = tokens.divider)
        }
    }
}

@Composable
private fun InventoryAlertTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var hour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }
    var hourExpanded by remember { mutableStateOf(false) }
    var minuteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settings.inventoryAlertsTimeLabel) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = hourExpanded,
                    onExpandedChange = { hourExpanded = !hourExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = hour.toString().padStart(2, '0'),
                        onValueChange = {},
                        label = { Text("HH") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = hourExpanded,
                        onDismissRequest = { hourExpanded = false }
                    ) {
                        (0..23).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.toString().padStart(2, '0')) },
                                onClick = {
                                    hour = option
                                    hourExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = minuteExpanded,
                    onExpandedChange = { minuteExpanded = !minuteExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = minute.toString().padStart(2, '0'),
                        onValueChange = {},
                        label = { Text("MM") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minuteExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = minuteExpanded,
                        onDismissRequest = { minuteExpanded = false }
                    ) {
                        (0..59).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.toString().padStart(2, '0')) },
                                onClick = {
                                    minute = option
                                    minuteExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(hour, minute) }) {
                Text(strings.settings.inventoryAlertsTimeSaveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.settings.inventoryAlertsTimeCancelLabel)
            }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.coerceIn(0, 23).toString().padStart(2, '0')}:${minute.coerceIn(0, 59).toString().padStart(2, '0')}"

@Composable
private fun TargetRow(
    label: String,
    baseCurrency: String,
    baseAmount: Double,
    secondaryCurrency: String?,
    secondaryAmount: Double?
) {
    val roundedBase = ceil(baseAmount)
    val roundedSecondary = secondaryAmount?.let { ceil(it) }
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatCurrency(baseCurrency, roundedBase),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        if (secondaryCurrency != null && roundedSecondary != null) {
            Text(
                text = formatCurrency(secondaryCurrency, roundedSecondary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SalesTargetDialog(
    initialValue: Double,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var input by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settings.salesTargetEditLabel) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(baseCurrency) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                val value = input.toDoubleOrNull() ?: 0.0
                onConfirm(value)
            }) {
                Text(strings.settings.inventoryAlertsTimeSaveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.settings.inventoryAlertsTimeCancelLabel)
            }
        }
    )
}

@Composable
private fun TtlHoursDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var input by remember { mutableStateOf(initialValue.coerceIn(1, 168).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settings.ttlHoursLabel) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit) },
                label = { Text(strings.settings.ttlHoursInputLabel) },
                supportingText = { Text(strings.settings.ttlHoursRangeHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
        },
        confirmButton = {
            Button(onClick = {
                val value = input.toIntOrNull()?.coerceIn(1, 168) ?: 6
                onConfirm(value)
            }) {
                Text(strings.settings.inventoryAlertsTimeSaveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.settings.inventoryAlertsTimeCancelLabel)
            }
        }
    )
}

@Composable
private fun ReturnPolicyDaysDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var input by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Límite de días") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Días (0 = sin límite)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                )
            )
        },
        confirmButton = {
            Button(onClick = {
                val value = input.toIntOrNull()?.coerceAtLeast(0) ?: 0
                onConfirm(value)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ReturnPolicyDestinationDialog(
    current: ReturnDestinationPolicy,
    allowRefunds: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ReturnDestinationPolicy) -> Unit
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Destino por defecto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Define si el retorno genera reembolso inmediato o crédito a favor."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selected == ReturnDestinationPolicy.CREDIT,
                        onClick = { selected = ReturnDestinationPolicy.CREDIT },
                        label = { Text("Crédito") }
                    )
                    FilterChip(
                        selected = selected == ReturnDestinationPolicy.REFUND,
                        onClick = { selected = ReturnDestinationPolicy.REFUND },
                        label = { Text("Reembolso") },
                        enabled = allowRefunds
                    )
                }
                if (!allowRefunds) {
                    Text(
                        "Los reembolsos están deshabilitados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SyncSection(
    syncSettings: SyncSettings,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onUseTtlChanged: (Boolean) -> Unit,
    onTtlHoursClick: () -> Unit,
    compact: Boolean
) {
    val strings = LocalAppStrings.current
    val tokens = settingsTokens()
    SettingSection(
        title = strings.settings.syncTitle,
        icon = Icons.Outlined.Sync,
        compact = compact
    ) {
        SyncTogglesRow(
            autoSync = syncSettings.autoSync,
            syncOnStartup = syncSettings.syncOnStartup,
            wifiOnly = syncSettings.wifiOnly,
            useTtl = syncSettings.useTtl,
            onAutoSyncChanged = onAutoSyncChanged,
            onSyncOnStartupChanged = onSyncOnStartupChanged,
            onWifiOnlyChanged = onWifiOnlyChanged,
            onUseTtlChanged = onUseTtlChanged,
            ttlHours = syncSettings.ttlHours,
            onTtlHoursClick = onTtlHoursClick,
            compact = compact
        )
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
        Text(
            text = strings.settings.syncBackgroundHint,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.subtleText
        )
    }
}

@Composable
private fun SyncLogSection(
    entries: List<com.erpnext.pos.domain.models.SyncLogEntry>,
    compact: Boolean
) {
    val strings = LocalAppStrings.current
    val tokens = settingsTokens()
    ExpandableSection(
        title = strings.settings.syncLogTitle,
        icon = Icons.Outlined.History,
        compact = compact,
        initiallyExpanded = false
    ) {
        if (entries.isEmpty()) {
            Text(
                text = strings.settings.syncLogEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.subtleText
            )
            return@ExpandableSection
        }
        entries.take(6).forEach { entry ->
            val statusLabel = when (entry.status) {
                com.erpnext.pos.domain.models.SyncLogStatus.SUCCESS -> strings.settings.syncStatusSuccess
                com.erpnext.pos.domain.models.SyncLogStatus.PARTIAL -> strings.settings.syncLogStatusPartial
                com.erpnext.pos.domain.models.SyncLogStatus.ERROR -> strings.settings.syncStatusError
                com.erpnext.pos.domain.models.SyncLogStatus.CANCELED -> strings.settings.syncLogStatusCanceled
            }
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = when (entry.status) {
                        com.erpnext.pos.domain.models.SyncLogStatus.SUCCESS -> tokens.accent
                        com.erpnext.pos.domain.models.SyncLogStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
                        com.erpnext.pos.domain.models.SyncLogStatus.ERROR -> MaterialTheme.colorScheme.error
                        com.erpnext.pos.domain.models.SyncLogStatus.CANCELED -> tokens.mutedText
                    }
                )
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.subtleText
                )
                Text(
                    text = entry.startedAt.toErpDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.subtleText
                )
                if (entry.failedSteps.isNotEmpty()) {
                    Text(
                        text = entry.failedSteps.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.subtleText
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncTogglesRow(
    autoSync: Boolean,
    syncOnStartup: Boolean,
    wifiOnly: Boolean,
    useTtl: Boolean,
    ttlHours: Int,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onUseTtlChanged: (Boolean) -> Unit,
    onTtlHoursClick: () -> Unit,
    compact: Boolean
) {
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingToggle(
            label = strings.settings.autoSyncLabel,
            checked = autoSync,
            onCheckedChange = onAutoSyncChanged,
            compact = compact
        )
        SettingToggle(
            label = strings.settings.syncOnStartupLabel,
            checked = syncOnStartup,
            onCheckedChange = onSyncOnStartupChanged,
            compact = compact
        )
        SettingToggle(
            label = strings.settings.wifiOnlyLabel,
            checked = wifiOnly,
            onCheckedChange = onWifiOnlyChanged,
            compact = compact
        )
        SettingToggle(
            label = strings.settings.useTtlLabel,
            checked = useTtl,
            onCheckedChange = onUseTtlChanged,
            supportingText = strings.settings.useTtlHelp,
            compact = compact,
            showDivider = true
        )
        SettingItem(
            label = strings.settings.ttlHoursLabel,
            value = "$ttlHours h",
            onClick = onTtlHoursClick,
            enabled = useTtl,
            compact = compact,
            showDivider = false
        )
    }
}

private data class SummaryField(
    val label: String,
    val value: String
)

@Composable
private fun SummaryGrid(items: List<SummaryField>) {
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryItem(row[0], modifier = Modifier.weight(1f))
                if (row.size > 1) {
                    SummaryItem(row[1], modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(item: SummaryField, modifier: Modifier = Modifier) {
    val tokens = settingsTokens()
    Column(modifier = modifier) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.subtleText
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = tokens.titleColor
        )
    }
}

private data class StatusStyle(
    val container: Color,
    val content: Color
)

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

private data class SettingsTokens(
    val cardContainer: Color,
    val cardBorder: Color,
    val divider: Color,
    val titleColor: Color,
    val valueColor: Color,
    val subtleText: Color,
    val mutedText: Color,
    val accent: Color,
    val iconTint: Color,
    val heroPrimary: Color,
    val heroSecondary: Color,
    val heroBorder: Color,
    val heroText: Color,
    val heroButton: Color,
    val heroButtonText: Color,
)

@Composable
private fun settingsTokens(): SettingsTokens {
    val scheme = MaterialTheme.colorScheme
    return SettingsTokens(
        cardContainer = scheme.surface,
        cardBorder = scheme.outlineVariant.copy(alpha = 0.55f),
        divider = scheme.outlineVariant.copy(alpha = 0.35f),
        titleColor = scheme.onSurface,
        valueColor = scheme.onSurfaceVariant,
        subtleText = scheme.onSurfaceVariant.copy(alpha = 0.9f),
        mutedText = scheme.onSurfaceVariant.copy(alpha = 0.7f),
        accent = scheme.primary,
        iconTint = scheme.onSurfaceVariant,
        heroPrimary = scheme.primaryContainer.copy(alpha = 0.96f),
        heroSecondary = scheme.secondaryContainer.copy(alpha = 0.96f),
        heroBorder = scheme.outlineVariant.copy(alpha = 0.4f),
        heroText = scheme.onPrimaryContainer,
        heroButton = scheme.onPrimaryContainer,
        heroButtonText = scheme.primary
    )
}

@Composable
private fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    compact: Boolean
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = when (currentLanguage) {
                AppLanguage.Spanish -> strings.settings.languageSpanish
                AppLanguage.English -> strings.settings.languageEnglish
            },
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(strings.settings.languageLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = if (compact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyLarge
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.settings.languageSpanish) },
                onClick = {
                    onLanguageSelected(AppLanguage.Spanish)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(strings.settings.languageEnglish) },
                onClick = {
                    onLanguageSelected(AppLanguage.English)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun MissingContextBanner() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Configuración limitada",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "No se encontró un perfil POS activo. Abre una caja o selecciona un perfil para ver datos completos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun ThemeChipSelector(
    currentTheme: AppColorTheme,
    onThemeSelected: (AppColorTheme) -> Unit
) {
    val tokens = settingsTokens()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppColorTheme.entries.forEach { theme ->
            FilterChip(
                selected = theme == currentTheme,
                onClick = { onThemeSelected(theme) },
                label = { Text(theme.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = tokens.cardContainer,
                    labelColor = tokens.titleColor
                ),
            )
        }
    }
}

@Composable
private fun ThemeModeChipSelector(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    val tokens = settingsTokens()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == currentMode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    containerColor = tokens.cardContainer,
                    labelColor = tokens.titleColor,
                ),
                elevation = FilterChipDefaults.elevatedFilterChipElevation(),
            )
        }
    }
}
