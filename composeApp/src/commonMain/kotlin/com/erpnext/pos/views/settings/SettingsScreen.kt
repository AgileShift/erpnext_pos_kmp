package com.erpnext.pos.views.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
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
            )
        ), POSSettingAction()
    )
}

@Composable
fun PosSettingsScreen(
    state: POSSettingState,
    action: POSSettingAction
) {
    val snackbar = koinInject<SnackbarController>()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when (state) {
            is POSSettingState.Success -> {
                Text(
                    text = "Configuración POS",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PosSettingsCard(
                    title = "General",
                    items = {
                        SettingsRow(
                            "Empresa",
                            state.settings.company
                        ) { action.onSelect("company") }
                        SettingsRow(
                            "Perfil POS",
                            state.settings.posProfile
                        ) { action.onSelect("profile") }
                        SettingsRow(
                            "Almacén",
                            state.settings.warehouse
                        ) { action.onSelect("warehouse") }
                        SettingsRow(
                            "Lista de precios",
                            state.settings.priceList
                        ) { action.onSelect("price_list") }
                    }
                )

                PosSettingsCard(
                    title = "Operación",
                    items = {
                        SwitchRow(
                            label = "Impuestos incluidos",
                            checked = state.settings.taxesIncluded
                        ) { action.onToggle("taxes", it) }

                        SwitchRow(
                            label = "Modo offline",
                            checked = state.settings.offlineMode
                        ) { action.onToggle("offline", it) }
                    }
                )

                PosSettingsCard(
                    title = "Hardware",
                    items = {
                        SwitchRow(
                            label = "Impresora habilitada",
                            checked = state.settings.printerEnabled
                        ) { action.onToggle("printer", it) }

                        SwitchRow(
                            label = "Cajón de dinero",
                            checked = state.settings.cashDrawerEnabled
                        ) { action.onToggle("cash_drawer", it) }
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

