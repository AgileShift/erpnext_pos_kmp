package com.erpnext.pos.views.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class SettingsCoordinator(
    val viewModel: SettingsViewModel
) {
    val screenStateFlow = viewModel.uiState

    fun loadSettings() {}

    fun onToggle(s: String, b: Boolean) {}

    fun onSelect(s: String) {}
}

@Composable
fun rememberSettingsCoordinator(): SettingsCoordinator {
    val viewModel: SettingsViewModel = koinViewModel()
    return remember(viewModel) {
        SettingsCoordinator(viewModel)
    }
}