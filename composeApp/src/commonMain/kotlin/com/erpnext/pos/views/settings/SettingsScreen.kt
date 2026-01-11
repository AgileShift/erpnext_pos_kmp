@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.settings

import AppColorTheme
import AppThemeMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localization.LocalAppStrings
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
                POSSettingBO(
                    "Clothing Center",
                    "Main",
                    "Main",
                    "Standar Price List",
                    taxesIncluded = false,
                    offlineMode = true,
                    printerEnabled = true,
                    cashDrawerEnabled = true
                ),
                syncSettings = com.erpnext.pos.localSource.preferences.SyncSettings(
                    autoSync = true,
                    syncOnStartup = true,
                    wifiOnly = false,
                    lastSyncAt = null
                ),
                syncState = com.erpnext.pos.sync.SyncState.IDLE,
                language = AppLanguage.Spanish,
                theme = AppColorTheme.Noir,
                themeMode = AppThemeMode.System
            ), POSSettingAction()
        )
}

@Composable
fun PosSettingsScreen(
    state: POSSettingState,
    action: POSSettingAction
) {
    val snackbar = koinInject<SnackbarController>()
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when (state) {
            is POSSettingState.Success -> {
                Text(
                    text = strings.settings.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PosSettingsCard(
                    title = strings.settings.generalTitle,
                    items = {
                        SettingsRow(
                            strings.settings.companyLabel,
                            state.settings.company
                        ) { action.onSelect("company") }
                        SettingsRow(
                            strings.settings.posProfileLabel,
                            state.settings.posProfile
                        ) { action.onSelect("profile") }
                        SettingsRow(
                            strings.settings.warehouseLabel,
                            state.settings.warehouse
                        ) { action.onSelect("warehouse") }
                        SettingsRow(
                            strings.settings.priceListLabel,
                            state.settings.priceList
                        ) { action.onSelect("price_list") }
                    }
                )

                SyncSettingsCard(
                    syncSettings = state.syncSettings,
                    syncState = state.syncState,
                    onSyncNow = action.onSyncNow,
                    onAutoSyncChanged = action.onAutoSyncChanged,
                    onSyncOnStartupChanged = action.onSyncOnStartupChanged,
                    onWifiOnlyChanged = action.onWifiOnlyChanged
                )

                PosSettingsCard(
                    title = strings.settings.operationTitle,
                    items = {
                        SwitchRow(
                            label = strings.settings.taxesIncludedLabel,
                            checked = state.settings.taxesIncluded
                        ) { action.onToggle("taxes", it) }

                        SwitchRow(
                            label = strings.settings.offlineModeLabel,
                            checked = state.settings.offlineMode
                        ) { action.onToggle("offline", it) }
                    }
                )

                PosSettingsCard(
                    title = strings.settings.hardwareTitle,
                    items = {
                        SwitchRow(
                            label = strings.settings.printerEnabledLabel,
                            checked = state.settings.printerEnabled
                        ) { action.onToggle("printer", it) }

                        SwitchRow(
                            label = strings.settings.cashDrawerEnabledLabel,
                            checked = state.settings.cashDrawerEnabled
                        ) { action.onToggle("cash_drawer", it) }
                    }
                )

                PosSettingsCard(
                    title = strings.settings.languageTitle,
                    items = {
                        LanguageSelector(
                            currentLanguage = state.language,
                            onLanguageSelected = action.onLanguageSelected
                        )
                        ThemeSelector(
                            currentTheme = state.theme,
                            onThemeSelected = action.onThemeSelected
                        )
                        Spacer(Modifier.height(12.dp))
                        ThemeModeSelector(
                            currentMode = state.themeMode,
                            onModeSelected = action.onThemeModeSelected
                        )
                        Text(
                            text = strings.settings.languageInstantHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                )
            }

            is POSSettingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }

            is POSSettingState.Error -> {
                snackbar.show(state.message, SnackbarType.Error, SnackbarPosition.Top)
            }
        }
    }
}

@Composable
fun PosSettingsCard(
    title: String,
    items: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        items()
    }
}

@Composable
fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SyncSettingsCard(
    syncSettings: com.erpnext.pos.localSource.preferences.SyncSettings,
    syncState: com.erpnext.pos.sync.SyncState,
    onSyncNow: () -> Unit,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    val lastSyncLabel = syncSettings.lastSyncAt?.toErpDateTime() ?: strings.settings.lastSyncNever
    val statusLabel = when (syncState) {
        com.erpnext.pos.sync.SyncState.IDLE -> strings.settings.syncStatusIdle
        com.erpnext.pos.sync.SyncState.SUCCESS -> strings.settings.syncStatusSuccess
        is com.erpnext.pos.sync.SyncState.ERROR -> strings.settings.syncStatusError
        is com.erpnext.pos.sync.SyncState.SYNCING -> strings.settings.syncStatusSyncing
    }

    PosSettingsCard(
        title = strings.settings.syncTitle,
        items = {
            SettingsRow(strings.settings.syncStatusLabel, statusLabel) {}
            SettingsRow(strings.settings.syncLastLabel, lastSyncLabel) {}
            Button(
                onClick = onSyncNow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(strings.settings.syncNowButton)
            }
            SwitchRow(
                label = strings.settings.autoSyncLabel,
                checked = syncSettings.autoSync,
                onCheckedChange = onAutoSyncChanged
            )
            SwitchRow(
                label = strings.settings.syncOnStartupLabel,
                checked = syncSettings.syncOnStartup,
                onCheckedChange = onSyncOnStartupChanged
            )
            SwitchRow(
                label = strings.settings.wifiOnlyLabel,
                checked = syncSettings.wifiOnly,
                onCheckedChange = onWifiOnlyChanged
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }

    val currentLabel = when (currentLanguage) {
        AppLanguage.Spanish -> strings.settings.languageSpanish
        AppLanguage.English -> strings.settings.languageEnglish
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
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
private fun ThemeSelector(
    currentTheme: AppColorTheme,
    onThemeSelected: (AppColorTheme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentTheme.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tema") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppColorTheme.values().forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.label) },
                    onClick = {
                        onThemeSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentMode.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Modo de tema") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppThemeMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}
