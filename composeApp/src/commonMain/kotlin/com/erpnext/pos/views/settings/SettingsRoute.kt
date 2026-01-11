package com.erpnext.pos.views.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun SettingsRoute(
    coordinator: SettingsCoordinator = rememberSettingsCoordinator()
) {
    val uiState by coordinator.screenStateFlow.collectAsState(POSSettingState.Loading)
    val actions = rememberSettingsActions(coordinator)

    PosSettingsScreen(uiState, actions)
}

@Composable
fun rememberSettingsActions(coordinator: SettingsCoordinator): POSSettingAction {
    return remember(coordinator) {
        POSSettingAction(
            loadSettings = coordinator::loadSettings,
            onToggle = coordinator::onToggle,
            onSelect = coordinator::onSelect,
            onSyncNow = coordinator::onSyncNow,
            onAutoSyncChanged = coordinator::onAutoSyncChanged,
            onSyncOnStartupChanged = coordinator::onSyncOnStartupChanged,
            onWifiOnlyChanged = coordinator::onWifiOnlyChanged,
            onLanguageSelected = coordinator::onLanguageSelected,
            onThemeSelected = coordinator::onThemeSelected,
            onThemeModeSelected = coordinator::onThemeModeSelected
        )
    }
}
