package com.erpnext.pos.views.printing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import com.erpnext.pos.localization.AppLanguage
import org.koin.compose.koinInject

@Composable
fun PrinterManagementRoute(
    viewModel: PrinterManagementViewModel = koinInject(),
    languagePreferences: LanguagePreferences = koinInject(),
) {
  val state by viewModel.uiState.collectAsState()
  val language by languagePreferences.language.collectAsState(initial = AppLanguage.Spanish)
  PrinterManagementScreen(
      state = state,
      language = language,
      onCreateNew = viewModel::createNew,
      onSelectProfile = viewModel::selectProfile,
      onUpdateForm = viewModel::updateForm,
      onUseDiscoveredDevice = viewModel::useDiscoveredDevice,
      onRefreshDiscovery = viewModel::refreshDiscovery,
      onSave = viewModel::saveProfile,
      onDelete = viewModel::deleteSelected,
      onSetDefault = viewModel::setSelectedAsDefault,
      onPrintTest = viewModel::printTestDocument,
      onClearMessage = viewModel::clearMessage,
  )
}
