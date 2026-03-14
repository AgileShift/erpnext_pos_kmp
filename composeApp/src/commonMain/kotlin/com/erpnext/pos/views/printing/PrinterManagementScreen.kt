@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice
import com.erpnext.pos.domain.printing.model.PrintJobStatus
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.printing.discovery.PrinterCatalog

@Composable
fun PrinterManagementScreen(
    state: PrinterManagementUiState,
    language: AppLanguage,
    onCreateNew: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onUpdateForm: ((PrinterProfileFormState) -> PrinterProfileFormState) -> Unit,
    onUseDiscoveredDevice: (DiscoveredPrinterDevice) -> Unit,
    onRefreshDiscovery: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onPrintTest: () -> Unit,
    onClearMessage: () -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val wideLayout = maxWidth >= 980.dp

    if (wideLayout) {
      Row(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(
            modifier = Modifier.weight(0.95f).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          DiscoveredPrintersPanel(state, language, onRefreshDiscovery, onUseDiscoveredDevice)
          SavedPrintersPanel(state, language, onCreateNew, onSelectProfile)
        }
        EditorPanel(
            modifier = Modifier.weight(1.2f).fillMaxSize(),
            state = state,
            language = language,
            scrollable = true,
            onUpdateForm = onUpdateForm,
            onSave = onSave,
            onDelete = onDelete,
            onSetDefault = onSetDefault,
            onPrintTest = onPrintTest,
            onClearMessage = onClearMessage,
        )
      }
    } else {
      LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item { DiscoveredPrintersPanel(state, language, onRefreshDiscovery, onUseDiscoveredDevice) }
        item { SavedPrintersPanel(state, language, onCreateNew, onSelectProfile) }
        item {
          EditorPanel(
              modifier = Modifier.fillMaxWidth(),
              state = state,
              language = language,
              scrollable = false,
              onUpdateForm = onUpdateForm,
              onSave = onSave,
              onDelete = onDelete,
              onSetDefault = onSetDefault,
              onPrintTest = onPrintTest,
              onClearMessage = onClearMessage,
          )
        }
      }
    }
  }
}

@Composable
private fun DiscoveredPrintersPanel(
    state: PrinterManagementUiState,
    language: AppLanguage,
    onRefreshDiscovery: () -> Unit,
    onUseDiscoveredDevice: (DiscoveredPrinterDevice) -> Unit,
) {
  SectionCard(
      title = tr(language, "Dispositivos Bluetooth detectados", "Detected Bluetooth devices"),
      subtitle =
          tr(
              language,
              "Lista de Android vinculada. Nombre Bluetooth y MAC son reales; marca, modelo, papel, chars y code page son sugeridos.",
              "This is the paired Android list. Bluetooth name and MAC are real. Brand, model, paper, chars, and code page are suggested for faster setup.",
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          tr(language, "Dispositivos vinculados", "Paired devices"),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      AssistChip(
          onClick = onRefreshDiscovery,
          label = { Text(tr(language, "Actualizar", "Refresh")) },
          leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
      )
    }
    Spacer(Modifier.height(12.dp))
    if (state.discoveredDevices.isEmpty()) {
      EmptyState(
          icon = Icons.Outlined.Bluetooth,
          title =
              tr(
                  language,
                  "No se encontraron impresoras Bluetooth vinculadas",
                  "No paired Bluetooth printers found",
              ),
          body =
              tr(
                  language,
                  "Vincula la impresora desde ajustes de Android, mantén Bluetooth encendido y toca Actualizar.",
                  "Pair the printer from Android settings, keep Bluetooth on, and tap Refresh.",
              ),
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.discoveredDevices.forEach { device ->
          DiscoveredPrinterCard(
              device = device,
              language = language,
              onUseDiscoveredDevice = { onUseDiscoveredDevice(device) },
          )
        }
      }
    }
  }
}

@Composable
private fun SavedPrintersPanel(
    state: PrinterManagementUiState,
    language: AppLanguage,
    onCreateNew: () -> Unit,
    onSelectProfile: (String) -> Unit,
) {
  SectionCard(
      title = tr(language, "Perfiles de impresora guardados", "Saved printer profiles"),
      subtitle =
          tr(
              language,
              "Estos perfiles ya están almacenados localmente en el POS.",
              "These are the profiles already stored locally in the POS.",
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          if (language == AppLanguage.Spanish) {
            "${state.profiles.size} perfil${if (state.profiles.size == 1) "" else "es"} guardado${if (state.profiles.size == 1) "" else "s"}"
          } else {
            "${state.profiles.size} saved profile${if (state.profiles.size == 1) "" else "s"}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      AssistChip(
          onClick = onCreateNew,
          label = { Text(tr(language, "Nuevo", "New")) },
          leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
      )
    }
    Spacer(Modifier.height(12.dp))
    if (state.profiles.isEmpty()) {
      EmptyState(
          icon = Icons.Outlined.Print,
          title = tr(language, "Aún no hay perfiles guardados", "No saved profiles yet"),
          body =
              tr(
                  language,
                  "Toca un dispositivo Bluetooth arriba o crea un perfil de red manual.",
                  "Tap a Bluetooth device above or create a manual network profile.",
              ),
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.profiles.forEach { profile ->
          PrinterProfileCard(
              profile = profile,
              language = language,
              selected = profile.id == state.selectedProfileId,
              onClick = { onSelectProfile(profile.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun EditorPanel(
    modifier: Modifier,
    state: PrinterManagementUiState,
    language: AppLanguage,
    scrollable: Boolean,
    onUpdateForm: ((PrinterProfileFormState) -> PrinterProfileFormState) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onPrintTest: () -> Unit,
    onClearMessage: () -> Unit,
) {
  SectionCard(
      modifier = modifier,
      title = tr(language, "Editor de perfil", "Profile editor"),
      subtitle =
          tr(
              language,
              "Revisa las sugerencias detectadas y muestra solo los campos necesarios para la ruta elegida.",
              "Review the detected suggestions and only show the fields needed for the selected route.",
          ),
  ) {
    val contentModifier =
        if (scrollable) {
          Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        } else {
          Modifier.fillMaxWidth()
        }
    Column(
        modifier = contentModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      state.message?.let { MessageBanner(it, onClearMessage) }
      PrinterEditorForm(form = state.form, language = language, onUpdateForm = onUpdateForm)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onSave, enabled = !state.isBusy) {
          Text(tr(language, "Guardar perfil", "Save profile"))
        }
        AssistChip(
            onClick = onSetDefault,
            label = { Text(tr(language, "Definir por defecto", "Set as default")) },
        )
        AssistChip(
            onClick = onPrintTest,
            label = { Text(tr(language, "Prueba de impresión", "Test print")) },
            leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
        )
        AssistChip(
            onClick = onDelete,
            label = { Text(tr(language, "Eliminar", "Delete")) },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        )
      }
      RecentJobsSection(state, language)
    }
  }
}

@Composable
private fun RecentJobsSection(state: PrinterManagementUiState, language: AppLanguage) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        tr(language, "Trabajos recientes de impresión", "Recent print jobs"),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    if (state.jobs.isEmpty()) {
      Text(
          tr(language, "Aún no hay historial local de impresión.", "No local print history yet."),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      state.jobs.forEachIndexed { index, job ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Column(modifier = Modifier.weight(1f)) {
            Text(job.summary, fontWeight = FontWeight.Medium)
            Text(job.documentType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          JobStatusChip(job.status, language)
        }
        if (index < state.jobs.lastIndex) {
          HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
      }
    }
  }
}

@Composable
private fun DiscoveredPrinterCard(
    device: DiscoveredPrinterDevice,
    language: AppLanguage,
    onUseDiscoveredDevice: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
              "MAC ${device.address}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(onClick = onUseDiscoveredDevice) { Text(tr(language, "Usar este dispositivo", "Use this device")) }
      }

      Text(
          tr(language, "Categorías detectadas", "Detected categories"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryBadge(
            tr(language, "Conexión", "Connection"),
            humanTransport(device.transportType, language),
            MaterialTheme.colorScheme.primary,
        )
        device.familyHint?.let {
          CategoryBadge(tr(language, "Tipo", "Type"), humanFamily(it, language), MaterialTheme.colorScheme.secondary)
        }
        device.languageHint?.let {
          CategoryBadge(
              tr(language, "Lenguaje", "Language"),
              humanLanguage(it, language),
              MaterialTheme.colorScheme.tertiary,
          )
        }
      }

      Text(
          tr(language, "Capacidades sugeridas", "Suggested capabilities"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        device.brandHint?.let { CategoryBadge(tr(language, "Marca", "Brand"), it, MaterialTheme.colorScheme.primary) }
        device.modelHint?.let { CategoryBadge(tr(language, "Modelo", "Model"), it, MaterialTheme.colorScheme.secondary) }
        device.paperWidthMmHint?.let {
          CategoryBadge(
              tr(language, "Papel", "Paper"),
              "${it} mm",
              MaterialTheme.colorScheme.tertiary,
          )
        }
        device.charactersPerLineHint?.let {
          CategoryBadge(
              tr(language, "Chars", "Chars"),
              "${it}/${tr(language, "línea", "line")}",
              MaterialTheme.colorScheme.primary,
          )
        }
        device.codePageHint?.let { CategoryBadge(tr(language, "Code page", "Code page"), it, MaterialTheme.colorScheme.secondary) }
      }

      val metadataLine =
          remember(device) { listOfNotNull(device.confidenceLabel).joinToString(" • ") }
      if (metadataLine.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(metadataLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun PrinterProfileCard(
    profile: PrinterProfile,
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      shape = RoundedCornerShape(22.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor =
                  if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                  else MaterialTheme.colorScheme.surfaceContainerLow
          ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
              listOfNotNull(profile.brandHint, profile.modelHint)
                  .joinToString(" ")
                  .ifBlank { tr(language, "Perfil genérico", "Generic profile") },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (profile.isDefault) {
          CategoryBadge(
              tr(language, "Predeterminada", "Default"),
              tr(language, "Sí", "Yes"),
              MaterialTheme.colorScheme.primary,
          )
        }
      }

      Text(
          tr(language, "Categorías de impresora", "Printer categories"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryBadge(tr(language, "Tipo", "Type"), humanFamily(profile.family, language), MaterialTheme.colorScheme.secondary)
        CategoryBadge(
            tr(language, "Lenguaje", "Language"),
            humanLanguage(profile.language, language),
            MaterialTheme.colorScheme.tertiary,
        )
        profile.supportedTransports.forEach { transport ->
          CategoryBadge(tr(language, "Ruta", "Route"), humanTransport(transport, language), MaterialTheme.colorScheme.primary)
        }
      }

      Text(
          tr(language, "Capacidades", "Capabilities"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryBadge(tr(language, "Papel", "Paper"), "${profile.paperWidthMm} mm", MaterialTheme.colorScheme.secondary)
        CategoryBadge(
            tr(language, "Chars", "Chars"),
            "${profile.charactersPerLine}/${tr(language, "línea", "line")}",
            MaterialTheme.colorScheme.tertiary,
        )
        if (!profile.isEnabled) {
          CategoryBadge(
              tr(language, "Estado", "Status"),
              tr(language, "Deshabilitada", "Disabled"),
              MaterialTheme.colorScheme.error,
          )
        }
      }

      val routeSummary =
          buildString {
            if (!profile.bluetoothMacAddress.isNullOrBlank()) {
              append("Bluetooth ${profile.bluetoothName ?: profile.bluetoothMacAddress}")
            }
            if (!profile.host.isNullOrBlank()) {
              if (isNotBlank()) append(" • ")
              append("${tr(language, "Red", "Network")} ${profile.host}:${profile.port ?: 9100}")
            }
          }
      if (routeSummary.isNotBlank()) {
        Text(routeSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun PrinterEditorForm(
    form: PrinterProfileFormState,
    language: AppLanguage,
    onUpdateForm: ((PrinterProfileFormState) -> PrinterProfileFormState) -> Unit,
) {
  val brandOptions = remember(form.brandHint) { PrinterCatalog.brands(form.brandHint) }
  val modelOptions = remember(form.brandHint, form.modelHint) { PrinterCatalog.models(form.brandHint, form.modelHint) }
  val usesTcp = form.preferredTransport == TransportType.TCP_RAW
  val usesBluetooth = form.preferredTransport == TransportType.BT_SPP || form.preferredTransport == TransportType.BT_DESKTOP

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    FormSection(
        title = tr(language, "Identidad", "Identity"),
        caption =
            tr(
                language,
                "Usa nombres fáciles de reconocer para la persona en caja.",
                "Use human names the cashier can recognize immediately.",
            ),
    ) {
      OutlinedTextField(
          value = form.name,
          onValueChange = { value -> onUpdateForm { current -> current.copy(name = value) } },
          label = { Text(tr(language, "Nombre de impresora", "Printer name")) },
          modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectionDropdown(
            value = form.brandHint,
            label = tr(language, "Marca", "Brand"),
            options = brandOptions,
            language = language,
            modifier = Modifier.weight(1f),
            onSelected = { selectedBrand ->
              onUpdateForm { current ->
                current.copy(
                    brandHint = selectedBrand,
                    modelHint =
                        if (current.brandHint == selectedBrand && current.modelHint.isNotBlank()) {
                          current.modelHint
                        } else {
                          PrinterCatalog.models(selectedBrand, "").firstOrNull().orEmpty()
                        },
                )
              }
            },
        )
        SelectionDropdown(
            value = form.modelHint,
            label = tr(language, "Modelo", "Model"),
            options = modelOptions,
            language = language,
            modifier = Modifier.weight(1f),
            onSelected = { selectedModel -> onUpdateForm { current -> current.copy(modelHint = selectedModel) } },
        )
      }
    }

    FormSection(
        title = tr(language, "Tipo de impresora", "Printer type"),
        caption =
            tr(
                language,
                "Estos campos controlan renderer, formato y comportamiento esperado.",
                "These fields control renderer, formatting, and expectations.",
            ),
    ) {
      Text(
          tr(language, "Familia de impresora", "Printer family"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(8.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrinterFamily.entries.forEach { family ->
          FilterChip(
              selected = form.family == family,
              onClick = { onUpdateForm { current -> current.copy(family = family) } },
              label = { Text(humanFamily(family, language)) },
          )
        }
      }
      Spacer(Modifier.height(12.dp))
      Text(
          tr(language, "Lenguaje de impresora", "Printer language"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(8.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrinterLanguage.entries.forEach { printerLanguage ->
          FilterChip(
              selected = form.language == printerLanguage,
              onClick = { onUpdateForm { current -> current.copy(language = printerLanguage) } },
              label = { Text(humanLanguage(printerLanguage, language)) },
          )
        }
      }
    }

    FormSection(
        title = tr(language, "Conexión", "Connection"),
        caption =
            tr(
                language,
                "Elige primero la ruta preferida. El editor muestra solo los campos necesarios para esa ruta.",
                "Choose the preferred route first. The editor only shows the fields required for that route.",
            ),
    ) {
      Text(
          tr(language, "Transportes disponibles", "Available transports"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(8.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TransportType.entries.forEach { type ->
          FilterChip(
              selected = type in form.supportedTransports,
              onClick = {
                onUpdateForm { current ->
                  val transports =
                      if (type in current.supportedTransports) current.supportedTransports - type
                      else current.supportedTransports + type
                  current.copy(
                      supportedTransports = transports,
                      preferredTransport = when {
                        current.preferredTransport == null -> type
                        current.preferredTransport !in transports -> transports.firstOrNull()
                        else -> current.preferredTransport
                      },
                  )
                }
              },
              label = { Text(humanTransport(type, language)) },
          )
        }
      }
      Spacer(Modifier.height(12.dp))
      Text(
          tr(language, "Ruta preferida", "Preferred route"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(8.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        form.supportedTransports.forEach { type ->
          AssistChip(
              onClick = { onUpdateForm { current -> current.copy(preferredTransport = type) } },
              label = {
                Text(
                    if (form.preferredTransport == type) {
                      "${tr(language, "Usando", "Using")} ${humanTransport(type, language)}"
                    } else {
                      humanTransport(type, language)
                    }
                )
              },
              leadingIcon = {
                Icon(
                    imageVector = if (type == TransportType.TCP_RAW) Icons.Outlined.Router else Icons.Outlined.Bluetooth,
                    contentDescription = null,
                )
              },
          )
        }
      }
      Spacer(Modifier.height(12.dp))
      if (usesBluetooth) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
              value = form.bluetoothName,
              onValueChange = { onUpdateForm { current -> current.copy(bluetoothName = it) } },
              label = { Text(tr(language, "Nombre Bluetooth", "Bluetooth name")) },
              modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
              value = form.bluetoothMacAddress,
              onValueChange = { onUpdateForm { current -> current.copy(bluetoothMacAddress = it) } },
              label = { Text(tr(language, "MAC Bluetooth", "Bluetooth MAC")) },
              modifier = Modifier.weight(1f),
          )
        }
      }
      if (usesTcp) {
        if (usesBluetooth) {
          Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
              value = form.host,
              onValueChange = { onUpdateForm { current -> current.copy(host = it) } },
              label = { Text(tr(language, "Host", "Host")) },
              modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
              value = form.port,
              onValueChange = { onUpdateForm { current -> current.copy(port = it.filter(Char::isDigit)) } },
              label = { Text(tr(language, "Puerto", "Port")) },
              modifier = Modifier.widthIn(min = 120.dp).weight(0.45f),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )
        }
      }
    }

    FormSection(
        title = tr(language, "Capacidades", "Capabilities"),
        caption =
            tr(
                language,
                "Estos valores son editables porque el descubrimiento Bluetooth genérico no puede garantizarlos.",
                "These values are editable because generic Bluetooth discovery cannot guarantee them.",
            ),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = form.paperWidthMm,
            onValueChange = { onUpdateForm { current -> current.copy(paperWidthMm = it.filter(Char::isDigit)) } },
            label = { Text(tr(language, "Ancho de papel (mm)", "Paper width (mm)")) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = form.charactersPerLine,
            onValueChange = { onUpdateForm { current -> current.copy(charactersPerLine = it.filter(Char::isDigit)) } },
            label = { Text(tr(language, "Chars por línea", "Chars per line")) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
      }
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
          value = form.codePage,
          onValueChange = { onUpdateForm { current -> current.copy(codePage = it) } },
          label = { Text(tr(language, "Code page", "Code page")) },
          modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(10.dp))
      ToggleRow(tr(language, "Corte automático", "Auto cut"), form.autoCut) { checked ->
        onUpdateForm { current -> current.copy(autoCut = checked) }
      }
      ToggleRow(tr(language, "Abrir gaveta de caja", "Open cash drawer"), form.openDrawer) { checked ->
        onUpdateForm { current -> current.copy(openDrawer = checked) }
      }
    }

    FormSection(
        title = tr(language, "Estado", "Status"),
        caption = tr(language, "Estado local y notas para el equipo.", "Local status and notes for the team."),
    ) {
      ToggleRow(tr(language, "Impresora por defecto", "Default printer"), form.isDefault) { checked ->
        onUpdateForm { current -> current.copy(isDefault = checked) }
      }
      ToggleRow(tr(language, "Habilitada", "Enabled"), form.isEnabled) { checked ->
        onUpdateForm { current -> current.copy(isEnabled = checked) }
      }
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
          value = form.notes,
          onValueChange = { onUpdateForm { current -> current.copy(notes = it) } },
          label = { Text(tr(language, "Notas", "Notes")) },
          modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun SelectionDropdown(
    value: String,
    label: String,
    options: List<String>,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
      modifier = modifier,
  ) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("${tr(language, "Selecciona", "Select")} $label") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
            text = { Text(option) },
            onClick = {
              onSelected(option)
              expanded = false
            },
        )
      }
    }
  }
}

@Composable
private fun FormSection(
    title: String,
    caption: String,
    content: @Composable () -> Unit,
) {
  ElevatedCard(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(14.dp))
      content()
    }
  }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
  ElevatedCard(
      modifier = modifier,
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      content()
    }
  }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape)
                .padding(12.dp)
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
    Text(title, fontWeight = FontWeight.SemiBold)
    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun MessageBanner(
    message: String,
    onClearMessage: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClearMessage),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Outlined.Info, contentDescription = null)
      Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun JobStatusChip(status: PrintJobStatus, language: AppLanguage) {
  val color =
      when (status) {
        PrintJobStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        PrintJobStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
      }
  Box(
      modifier =
          Modifier.background(color.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(humanJobStatus(status, language), color = color, style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
private fun HumanBadge(text: String, accent: Color) {
  Box(
      modifier =
          Modifier.background(accent.copy(alpha = 0.12f), CircleShape)
              .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = accent)
  }
}

@Composable
private fun CategoryBadge(label: String, value: String, accent: Color) {
  HumanBadge(text = "$label: $value", accent = accent)
}

private fun humanTransport(type: TransportType, language: AppLanguage): String =
    when (type) {
      TransportType.TCP_RAW -> tr(language, "Red (TCP)", "Network (TCP)")
      TransportType.BT_SPP -> tr(language, "Bluetooth Android", "Bluetooth Android")
      TransportType.BT_DESKTOP -> tr(language, "Bluetooth Desktop", "Bluetooth Desktop")
    }

private fun humanFamily(family: PrinterFamily, language: AppLanguage): String =
    when (family) {
      PrinterFamily.RECEIPT -> tr(language, "Ticket", "Receipt")
      PrinterFamily.LABEL -> tr(language, "Etiqueta", "Label")
    }

private fun humanLanguage(printerLanguage: PrinterLanguage, language: AppLanguage): String =
    when (printerLanguage) {
      PrinterLanguage.ESC_POS -> "ESC/POS"
      PrinterLanguage.ZPL -> "ZPL"
    }

private fun humanJobStatus(status: PrintJobStatus, language: AppLanguage): String =
    when (status) {
      PrintJobStatus.PENDING -> tr(language, "Pendiente", "Pending")
      PrintJobStatus.CONNECTING -> tr(language, "Conectando", "Connecting")
      PrintJobStatus.RENDERING -> tr(language, "Renderizando", "Rendering")
      PrintJobStatus.PRINTING -> tr(language, "Imprimiendo", "Printing")
      PrintJobStatus.SUCCESS -> tr(language, "Completado", "Success")
      PrintJobStatus.FAILED -> tr(language, "Fallido", "Failed")
      PrintJobStatus.RETRYING -> tr(language, "Reintentando", "Retrying")
      PrintJobStatus.CANCELLED -> tr(language, "Cancelado", "Cancelled")
    }

private fun tr(language: AppLanguage, spanish: String, english: String): String =
    if (language == AppLanguage.Spanish) {
      spanish
    } else {
      english
    }
