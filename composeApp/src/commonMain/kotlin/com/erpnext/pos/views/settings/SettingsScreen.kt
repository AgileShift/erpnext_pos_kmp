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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun SettingsScreenPreview() {
    PosSettingsScreen(
        POSSettingState.Success(
            settings = POSSettingBO(
                company = "Clothing Center",
                posProfile = "Main",
                warehouse = "Almacén Principal",
                priceList = "Standard Price List",
                taxesIncluded = false,
                offlineMode = true,
                printerEnabled = true,
                cashDrawerEnabled = true,
                allowNegativeStock = false
            ),
            syncSettings = SyncSettings(
                autoSync = true,
                syncOnStartup = true,
                wifiOnly = false,
                lastSyncAt = null,
                useTtl = false
            ),
            syncState = SyncState.IDLE,
            language = AppLanguage.Spanish,
            theme = AppColorTheme.Noir,
            themeMode = AppThemeMode.System,
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
            salesTargetConversionStale = false
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
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            when (state) {
                is POSSettingState.Success -> {
                    var showAlertTimeDialog by remember { mutableStateOf(false) }
                    var showSalesTargetDialog by remember { mutableStateOf(false) }

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

                    Text(
                        text = strings.settings.title,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SettingsOverviewRow(
                        settings = state.settings,
                        syncSettings = state.syncSettings,
                        syncState = state.syncState,
                        onSyncNow = action.onSyncNow,
                    )

                    SettingSection(title = strings.settings.generalTitle) {
                        SettingItem(
                            label = strings.settings.companyLabel,
                            value = state.settings.company,
                            onClick = { action.onSelect("company") }
                        )
                        SettingItem(
                            label = strings.settings.posProfileLabel,
                            value = state.settings.posProfile,
                            onClick = { action.onSelect("profile") }
                        )
                        SettingItem(
                            label = strings.settings.warehouseLabel,
                            value = state.settings.warehouse,
                            onClick = { action.onSelect("warehouse") }
                        )
                        SettingItem(
                            label = strings.settings.priceListLabel,
                            value = state.settings.priceList,
                            onClick = { action.onSelect("price_list") }
                        )
                    }

                    SyncSection(
                        syncSettings = state.syncSettings,
                        syncState = state.syncState,
                        onAutoSyncChanged = action.onAutoSyncChanged,
                        onSyncOnStartupChanged = action.onSyncOnStartupChanged,
                        onWifiOnlyChanged = action.onWifiOnlyChanged,
                        onUseTtlChanged = action.onUseTtlChanged,
                        onSyncNow = action.onSyncNow,
                    )

                    SettingSection(title = strings.settings.operationTitle) {
                        SettingToggle(
                            label = strings.settings.taxesIncludedLabel,
                            checked = state.settings.taxesIncluded,
                            onCheckedChange = action.onTaxesIncludedChanged
                        )
                        SettingToggle(
                            label = strings.settings.offlineModeLabel,
                            checked = state.settings.offlineMode,
                            onCheckedChange = action.onOfflineModeChanged
                        )
                        Column {
                            SettingToggle(
                                label = "Permitir venta con stock negativo",
                                checked = state.settings.allowNegativeStock,
                                onCheckedChange = {},
                                enabled = false
                            )
                            Text(
                                text = "Controlado por ERPNext (Desk).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SettingSection(
                        title = strings.settings.inventoryAlertsTitle,
                        description = strings.settings.inventoryAlertsTimeHint
                    ) {
                        SettingToggle(
                            label = strings.settings.inventoryAlertsEnabledLabel,
                            checked = state.inventoryAlertsEnabled,
                            onCheckedChange = action.onInventoryAlertsEnabledChanged
                        )
                        SettingItem(
                            label = strings.settings.inventoryAlertsTimeLabel,
                            value = formatTime(state.inventoryAlertHour, state.inventoryAlertMinute),
                            onClick = { showAlertTimeDialog = true },
                            enabled = state.inventoryAlertsEnabled
                        )
                    }

                    SettingSection(
                        title = strings.settings.salesTargetTitle,
                        description = strings.settings.salesTargetHint
                    ) {
                        SettingItem(
                            label = strings.settings.salesTargetEditLabel,
                            value = formatCurrency(
                                state.salesTargetBaseCurrency,
                                state.salesTargetMonthly
                            ),
                            onClick = { showSalesTargetDialog = true }
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
                            )
                        ) {
                            Text(strings.settings.salesTargetSyncLabel)
                        }
                    }

                    SettingSection(title = strings.settings.hardwareTitle) {
                        SettingToggle(
                            label = strings.settings.printerEnabledLabel,
                            checked = state.settings.printerEnabled,
                            onCheckedChange = action.onPrinterEnabledChanged
                        )
                        SettingToggle(
                            label = strings.settings.cashDrawerEnabledLabel,
                            checked = state.settings.cashDrawerEnabled,
                            onCheckedChange = action.onCashDrawerEnabledChanged
                        )
                    }

                    SettingSection(
                        title = strings.settings.languageTitle,
                        description = strings.settings.languageInstantHint
                    ) {
                        LanguageSelector(
                            currentLanguage = state.language,
                            onLanguageSelected = action.onLanguageSelected
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
) {
    BoxWithConstraints {
        val isWide = maxWidth > 780.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsInfoCard(settings = settings, modifier = Modifier.weight(0.35f))
                SettingsHeroCard(
                    syncSettings = syncSettings,
                    syncState = syncState,
                    onSyncNow = onSyncNow,
                    modifier = Modifier.weight(0.65f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsInfoCard(settings = settings)
                SettingsHeroCard(
                    syncSettings = syncSettings,
                    syncState = syncState,
                    onSyncNow = onSyncNow
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(
    settings: POSSettingBO,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.settings.generalTitle,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(14.dp))
            InfoLine(label = strings.settings.companyLabel, value = settings.company)
            InfoLine(label = strings.settings.posProfileLabel, value = settings.posProfile)
            InfoLine(label = strings.settings.warehouseLabel, value = settings.warehouse)
            InfoLine(label = strings.settings.priceListLabel, value = settings.priceList)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsHeroCard(
    syncSettings: SyncSettings,
    syncState: SyncState,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )
    val strings = LocalAppStrings.current
    val statusLabel = when (syncState) {
        SyncState.IDLE -> strings.settings.syncStatusIdle
        SyncState.SUCCESS -> strings.settings.syncStatusSuccess
        is SyncState.ERROR -> strings.settings.syncStatusError
        is SyncState.SYNCING -> strings.settings.syncStatusSyncing
    }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .graphicsLayer { clip = true }
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = strings.settings.syncTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.settings.syncStatusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = syncSettings.lastSyncAt?.toErpDateTime()
                        ?: strings.settings.lastSyncNever,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSyncNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.settings.syncNowButton)
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface
            )
        )
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
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = formatCurrency(baseCurrency, baseAmount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        if (secondaryCurrency != null && secondaryAmount != null) {
            Text(
                text = formatCurrency(secondaryCurrency, secondaryAmount),
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
private fun SyncSection(
    syncSettings: SyncSettings,
    syncState: SyncState,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onUseTtlChanged: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
) {
    val strings = LocalAppStrings.current
    SettingSection(title = strings.settings.syncTitle) {
        SyncTogglesRow(
            autoSync = syncSettings.autoSync,
            syncOnStartup = syncSettings.syncOnStartup,
            wifiOnly = syncSettings.wifiOnly,
            useTtl = syncSettings.useTtl,
            onAutoSyncChanged = onAutoSyncChanged,
            onSyncOnStartupChanged = onSyncOnStartupChanged,
            onWifiOnlyChanged = onWifiOnlyChanged,
            onUseTtlChanged = onUseTtlChanged,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (syncState) {
                SyncState.IDLE -> strings.settings.syncStatusIdle
                SyncState.SUCCESS -> strings.settings.syncStatusSuccess
                is SyncState.ERROR -> strings.settings.syncStatusError
                is SyncState.SYNCING -> strings.settings.syncStatusSyncing
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = strings.settings.syncLastLabel + ": " +
                    (syncSettings.lastSyncAt?.toErpDateTime() ?: strings.settings.lastSyncNever),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSyncNow,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(strings.settings.syncNowButton)
        }
    }
}

@Composable
private fun SyncTogglesRow(
    autoSync: Boolean,
    syncOnStartup: Boolean,
    wifiOnly: Boolean,
    useTtl: Boolean,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onUseTtlChanged: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingToggle(
            label = strings.settings.autoSyncLabel,
            checked = autoSync,
            onCheckedChange = onAutoSyncChanged
        )
        SettingToggle(
            label = strings.settings.syncOnStartupLabel,
            checked = syncOnStartup,
            onCheckedChange = onSyncOnStartupChanged
        )
        SettingToggle(
            label = strings.settings.wifiOnlyLabel,
            checked = wifiOnly,
            onCheckedChange = onWifiOnlyChanged
        )
        SettingToggle(
            label = strings.settings.useTtlLabel,
            checked = useTtl,
            onCheckedChange = onUseTtlChanged
        )
    }
}

@Composable
private fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { }
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
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
private fun ThemeChipSelector(
    currentTheme: AppColorTheme,
    onThemeSelected: (AppColorTheme) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppColorTheme.entries.forEach { theme ->
            FilterChip(
                selected = theme == currentTheme,
                onClick = { onThemeSelected(theme) },
                label = { Text(theme.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == currentMode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
                elevation = FilterChipDefaults.elevatedFilterChipElevation(),
            )
        }
    }
}
