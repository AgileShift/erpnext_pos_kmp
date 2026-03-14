package com.erpnext.pos.views.printing

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice
import com.erpnext.pos.domain.printing.model.LabelDocument
import com.erpnext.pos.domain.printing.model.LabelField
import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.PrintJob
import com.erpnext.pos.domain.printing.model.PrinterCapabilities
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine
import com.erpnext.pos.domain.printing.model.ReceiptSection
import com.erpnext.pos.domain.printing.model.ReceiptTotals
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterDiscoveryService
import com.erpnext.pos.domain.printing.usecase.DeletePrinterProfileUseCase
import com.erpnext.pos.domain.printing.usecase.PrintDocumentInput
import com.erpnext.pos.domain.printing.usecase.PrintDocumentUseCase
import com.erpnext.pos.domain.printing.usecase.SavePrinterProfileUseCase
import com.erpnext.pos.domain.printing.usecase.SetDefaultPrinterUseCase
import com.erpnext.pos.domain.repositories.printing.IPrintJobRepository
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.domain.printing.platform.PrintingPlatform
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class PrinterProfileFormState(
    val id: String? = null,
    val name: String = "",
    val brandHint: String = "",
    val modelHint: String = "",
    val family: PrinterFamily = PrinterFamily.RECEIPT,
    val language: PrinterLanguage = PrinterLanguage.ESC_POS,
    val supportedTransports: Set<TransportType> = setOf(TransportType.TCP_RAW),
    val preferredTransport: TransportType? = TransportType.TCP_RAW,
    val host: String = "",
    val port: String = "9100",
    val bluetoothMacAddress: String = "",
    val bluetoothName: String = "",
    val paperWidthMm: String = "80",
    val charactersPerLine: String = "32",
    val codePage: String = "CP437",
    val autoCut: Boolean = true,
    val openDrawer: Boolean = false,
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val notes: String = "",
)

data class PrinterManagementUiState(
    val profiles: List<PrinterProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val selectedProfileName: String? = null,
    val defaultProfileName: String? = null,
    val form: PrinterProfileFormState = PrinterProfileFormState(),
    val jobs: List<PrintJob> = emptyList(),
    val discoveredDevices: List<DiscoveredPrinterDevice> = emptyList(),
    val message: String? = null,
    val isBusy: Boolean = false,
    val platformSummary: String = "",
)

@OptIn(ExperimentalTime::class)
class PrinterManagementViewModel(
    private val profileRepository: IPrinterProfileRepository,
    private val printJobRepository: IPrintJobRepository,
    private val discoveryService: PrinterDiscoveryService,
    private val printDocumentUseCase: PrintDocumentUseCase,
    private val savePrinterProfileUseCase: SavePrinterProfileUseCase,
    private val deletePrinterProfileUseCase: DeletePrinterProfileUseCase,
    private val setDefaultPrinterUseCase: SetDefaultPrinterUseCase,
    private val languagePreferences: LanguagePreferences,
) : BaseViewModel() {
  private val selection = MutableStateFlow<String?>(null)
  private val draft = MutableStateFlow(PrinterProfileFormState())
  private val devices = MutableStateFlow<List<DiscoveredPrinterDevice>>(emptyList())
  private val message = MutableStateFlow<String?>(null)
  private val busy = MutableStateFlow(false)

  private val _uiState = MutableStateFlow(PrinterManagementUiState())
  val uiState: StateFlow<PrinterManagementUiState> = _uiState.asStateFlow()
  private var currentLanguage: AppLanguage = AppLanguage.Spanish

  init {
    viewModelScope.launch {
      combine(
              profileRepository.observeProfiles(),
              printJobRepository.observeJobs(),
              selection,
              draft,
              devices,
              message,
              busy,
          ) { args: Array<Any?> ->
            val profiles = args[0] as List<PrinterProfile>
            val jobs = args[1] as List<PrintJob>
            val selectedId = args[2] as String?
            val form = args[3] as PrinterProfileFormState
            val discovered = args[4] as List<DiscoveredPrinterDevice>
            val toast = args[5] as String?
            val isBusy = args[6] as Boolean
            val activeProfile = profiles.firstOrNull { it.id == selectedId }
            val defaultProfile = profiles.firstOrNull { it.isDefault }
            PrinterManagementUiState(
                profiles = profiles,
                selectedProfileId = selectedId,
                selectedProfileName = activeProfile?.name,
                defaultProfileName = defaultProfile?.name,
                form =
                    if (selectedId == form.id || activeProfile == null) {
                      form
                    } else {
                      activeProfile.toFormState()
                    },
                jobs = jobs.take(10),
                discoveredDevices = discovered,
                message = toast,
                isBusy = isBusy,
                platformSummary =
                    "Platform ${PrintingPlatform.capabilities.platform} supports ${PrintingPlatform.capabilities.supportedTransports.joinToString()}",
            )
          }
          .collect { state -> _uiState.value = state }
    }
    viewModelScope.launch {
      languagePreferences.language.collect { language -> currentLanguage = language }
    }

    viewModelScope.launch {
      val defaultProfile = profileRepository.getDefaultProfile()
      if (defaultProfile != null) {
        selection.value = defaultProfile.id
        draft.value = defaultProfile.toFormState()
      }
    }
    refreshDiscovery()
  }

  fun createNew() {
    selection.value = null
    draft.value = PrinterProfileFormState()
    message.value = tr("Creando un nuevo perfil de impresora.", "Creating a new printer profile.")
  }

  fun selectProfile(id: String) {
    viewModelScope.launch {
      val profile = profileRepository.getById(id) ?: return@launch
      selection.value = id
      draft.value = profile.toFormState()
    }
  }

  fun updateForm(transform: (PrinterProfileFormState) -> PrinterProfileFormState) {
    draft.update(transform)
  }

  fun useDiscoveredDevice(device: DiscoveredPrinterDevice) {
    draft.update { current ->
      current.copy(
          name =
              current.name.takeIf { it.isNotBlank() }
                  ?: listOfNotNull(device.brandHint, device.modelHint, device.name)
                      .joinToString(" ")
                      .trim()
                      .ifBlank { device.name },
          brandHint = device.brandHint ?: current.brandHint,
          modelHint = device.modelHint ?: current.modelHint,
          family = device.familyHint ?: current.family,
          language = device.languageHint ?: current.language,
          bluetoothMacAddress = device.address,
          bluetoothName = device.name,
          paperWidthMm = (device.paperWidthMmHint ?: current.paperWidthMm.toIntOrNull() ?: 80).toString(),
          charactersPerLine =
              (device.charactersPerLineHint ?: current.charactersPerLine.toIntOrNull() ?: 32)
                  .toString(),
          codePage = device.codePageHint ?: current.codePage,
          supportedTransports = current.supportedTransports + device.transportType,
          preferredTransport = device.transportType,
          notes =
              buildString {
                val existingNotes = current.notes.trim()
                if (existingNotes.isNotEmpty()) {
                  append(existingNotes)
                  append("\n")
                }
                append("Detection source: ${device.name} (${device.address})")
                device.confidenceLabel?.let {
                  append("\n")
                  append("Hints: ")
                  append(it)
                }
              },
      )
    }
    message.value =
        tr(
            "Impresora Bluetooth cargada en el formulario. Revisa paper width, chars por línea y code page antes de guardar.",
            "Bluetooth printer loaded into the form. Review paper width, chars per line, and code page before saving.",
        )
  }

  fun refreshDiscovery() {
    viewModelScope.launch {
      runCatching { discoveryService.bondedDevices() }
          .onSuccess { bondedDevices ->
            devices.value = bondedDevices
            AppLogger.info(
                "PrinterManagementViewModel.refreshDiscovery -> devices=${bondedDevices.size}"
            )
            if (bondedDevices.isEmpty()) {
              message.value =
                  tr(
                      "No se encontraron impresoras Bluetooth vinculadas. Vincula la impresora en ajustes de Android y vuelve a intentar.",
                      "No paired Bluetooth printers were found. Pair the printer in Android settings and try again.",
                  )
            }
          }
          .onFailure { error ->
            devices.value = emptyList()
            AppLogger.warn(
                "PrinterManagementViewModel.refreshDiscovery failed",
                error,
                reportToSentry = false,
            )
            message.value =
                error.message
                    ?: tr(
                        "Falló el descubrimiento Bluetooth. Revisa permisos, estado Bluetooth y dispositivos vinculados.",
                        "Bluetooth discovery failed. Check permissions, Bluetooth state, and paired devices.",
                    )
          }
    }
  }

  fun saveProfile() {
    viewModelScope.launch {
      busy.value = true
      runCatching {
            val profile = draft.value.toProfile()
            validateProfileDraft(profile)
            savePrinterProfileUseCase(profile)
            val savedProfile = profileRepository.getById(profile.id) ?: profile
            selection.value = profile.id
            draft.value = savedProfile.toFormState()
            AppLogger.info(
                "PrinterManagementViewModel.saveProfile -> saved profile=${savedProfile.name}, default=${savedProfile.isDefault}, preferred=${savedProfile.preferredTransport}"
            )
            message.value =
                buildString {
                  append(tr("Guardado '${savedProfile.name}'. ", "Saved '${savedProfile.name}'. "))
                  append(
                      tr(
                          "Ruta preferida: ${savedProfile.preferredTransport ?: savedProfile.supportedTransports.firstOrNull()}. ",
                          "Preferred route: ${savedProfile.preferredTransport ?: savedProfile.supportedTransports.firstOrNull()}. ",
                      )
                  )
                  append(
                      if (savedProfile.isDefault) {
                        tr("Esta impresora ahora es la predeterminada.", "This printer is now the default.")
                      } else {
                        tr("Esta impresora no es la predeterminada.", "This printer is not the default.")
                      }
                  )
                }
          }
          .onFailure {
            AppLogger.warn("PrinterManagementViewModel.saveProfile failed", it, reportToSentry = false)
            message.value = it.message ?: tr("No se pudo guardar el perfil de impresora.", "Unable to save printer profile.")
          }
      busy.value = false
    }
  }

  fun deleteSelected() {
    val selectedId = selection.value ?: return
    viewModelScope.launch {
      busy.value = true
      runCatching {
            deletePrinterProfileUseCase(selectedId)
            AppLogger.info("PrinterManagementViewModel.deleteSelected -> deleted profileId=$selectedId")
            createNew()
            message.value = tr("Perfil de impresora eliminado.", "Printer profile deleted.")
          }
          .onFailure {
            AppLogger.warn("PrinterManagementViewModel.deleteSelected failed", it, reportToSentry = false)
            message.value = it.message ?: tr("No se pudo eliminar el perfil de impresora.", "Unable to delete printer profile.")
          }
      busy.value = false
    }
  }

  fun setSelectedAsDefault() {
    val selectedId = selection.value ?: return
    viewModelScope.launch {
      runCatching {
            setDefaultPrinterUseCase(selectedId)
            val profile = profileRepository.getById(selectedId)
            if (profile != null) {
              draft.value = profile.copy(isDefault = true).toFormState()
              AppLogger.info(
                  "PrinterManagementViewModel.setSelectedAsDefault -> default profile=${profile.name}"
              )
              message.value = "'${profile.name}' is now the default printer."
            } else {
              message.value = tr("Impresora predeterminada actualizada.", "Default printer updated.")
            }
          }
          .onFailure {
            AppLogger.warn("PrinterManagementViewModel.setSelectedAsDefault failed", it, reportToSentry = false)
            message.value = it.message ?: tr("No se pudo establecer la impresora predeterminada.", "Unable to set default printer.")
          }
    }
  }

  fun printTestDocument() {
    val selectedId =
        selection.value ?: draft.value.id ?: run {
          message.value = tr("Selecciona o guarda un perfil de impresora antes de imprimir prueba.", "Select or save a printer profile before sending a test print.")
          AppLogger.warn(
              "PrinterManagementViewModel.printTestDocument blocked: no selected profile",
              reportToSentry = false,
          )
          return
        }
    viewModelScope.launch {
      busy.value = true
      val jobId = "job-${Clock.System.now().toEpochMilliseconds()}"
      val form = draft.value
      val draftProfile = form.toProfile()
      val document =
          if (form.family == PrinterFamily.RECEIPT) {
            ReceiptDocument(
                documentId = "test-${Clock.System.now().toEpochMilliseconds()}",
                header =
                    ReceiptSection(
                        lines = listOf("ERPNext POS", "Printer Test"),
                        alignment = PrintAlignment.CENTER,
                    ),
                bodyLines =
                    listOf(
                        ReceiptLine("Coffee beans", "C$ 120.00"),
                        ReceiptLine("Notebook", "C$ 35.00"),
                    ),
                totals = ReceiptTotals(subTotal = "C$ 155.00", tax = "C$ 0.00", total = "C$ 155.00"),
                footer =
                    ReceiptSection(
                        lines = listOf("Offline-first printing baseline", "Thank you"),
                        alignment = PrintAlignment.CENTER,
                    ),
            )
          } else {
            LabelDocument(
                documentId = "label-${Clock.System.now().toEpochMilliseconds()}",
                fields =
                    listOf(
                        LabelField(30, 30, text = "ERPNext POS"),
                        LabelField(30, 80, text = "LABEL TEST"),
                        LabelField(30, 130, text = draft.value.name.ifBlank { "Printer" }),
                    ),
            )
          }
      printJobRepository.enqueue(
          PrintJob(
              id = jobId,
              profileId = selectedId,
              documentId = document.documentId,
              documentType = document::class.simpleName.orEmpty(),
              summary = "Test print for ${form.name.ifBlank { "printer" }}",
              createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
          )
      )
      runCatching {
            validateProfileReadyToPrint(draftProfile)
            AppLogger.info(
                "PrinterManagementViewModel.printTestDocument -> profile=${draftProfile.name}, preferred=${draftProfile.preferredTransport}, jobId=$jobId"
            )
            val result = printDocumentUseCase(PrintDocumentInput(selectedId, document)).getOrThrow()
            printJobRepository.update(
                PrintJob(
                    id = jobId,
                    profileId = selectedId,
                    documentId = document.documentId,
                    documentType = document::class.simpleName.orEmpty(),
                    summary = "Test print for ${form.name.ifBlank { "printer" }}",
                    status = com.erpnext.pos.domain.printing.model.PrintJobStatus.SUCCESS,
                    attempts = 1,
                    createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                    completedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                )
            )
            message.value =
                tr(
                    "Prueba enviada a '${form.name.ifBlank { draftProfile.name }}' vía ${result.transportType}. Bytes: ${result.bytesWritten}.",
                    "Test print sent to '${form.name.ifBlank { draftProfile.name }}' via ${result.transportType}. Bytes: ${result.bytesWritten}.",
                )
          }
          .onFailure {
            AppLogger.warn("PrinterManagementViewModel.printTestDocument failed", it, reportToSentry = false)
            printJobRepository.update(
                PrintJob(
                    id = jobId,
                    profileId = selectedId,
                    documentId = document.documentId,
                    documentType = document::class.simpleName.orEmpty(),
                    summary = "Test print for ${form.name.ifBlank { "printer" }}",
                    status = com.erpnext.pos.domain.printing.model.PrintJobStatus.FAILED,
                    attempts = 1,
                    lastError = it.message,
                    createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                    completedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                )
            )
            message.value =
                tr(
                    "Falló la impresión de prueba: ${it.message ?: "No se pudo imprimir el documento de prueba."}",
                    "Test print failed: ${it.message ?: "Unable to print test document."}",
                )
          }
      busy.value = false
    }
  }

  fun clearMessage() {
    message.value = null
  }

  private fun PrinterProfile.toFormState(): PrinterProfileFormState =
      PrinterProfileFormState(
          id = id,
          name = name,
          brandHint = brandHint.orEmpty(),
          modelHint = modelHint.orEmpty(),
          family = family,
          language = language,
          supportedTransports = supportedTransports,
          preferredTransport = preferredTransport,
          host = host.orEmpty(),
          port = (port ?: 9100).toString(),
          bluetoothMacAddress = bluetoothMacAddress.orEmpty(),
          bluetoothName = bluetoothName.orEmpty(),
          paperWidthMm = paperWidthMm.toString(),
          charactersPerLine = charactersPerLine.toString(),
          codePage = codePage,
          autoCut = autoCut,
          openDrawer = openDrawer,
          isDefault = isDefault,
          isEnabled = isEnabled,
          notes = notes.orEmpty(),
      )

  private fun PrinterProfileFormState.toProfile(): PrinterProfile {
    val id = id ?: "printer-${Clock.System.now().toEpochMilliseconds()}"
    return PrinterProfile(
        id = id,
        name = name.trim(),
        brandHint = brandHint.trim().takeIf { it.isNotBlank() },
        modelHint = modelHint.trim().takeIf { it.isNotBlank() },
        family = family,
        language = language,
        supportedTransports = supportedTransports.ifEmpty { setOf(TransportType.TCP_RAW) },
        preferredTransport = preferredTransport,
        host = host.trim().takeIf { it.isNotBlank() },
        port = port.toIntOrNull() ?: 9100,
        bluetoothMacAddress = bluetoothMacAddress.trim().takeIf { it.isNotBlank() },
        bluetoothName = bluetoothName.trim().takeIf { it.isNotBlank() },
        capabilities =
            PrinterCapabilities(
                paperWidthMm = paperWidthMm.toIntOrNull() ?: 80,
                charactersPerLine = charactersPerLine.toIntOrNull() ?: 32,
                codePage = codePage.trim().ifBlank { "CP437" },
                autoCut = autoCut,
                openDrawer = openDrawer,
            ),
        isDefault = isDefault,
        isEnabled = isEnabled,
        notes = notes.trim().takeIf { it.isNotBlank() },
    )
  }

  private fun validateProfileDraft(profile: PrinterProfile) {
    require(profile.name.isNotBlank()) { tr("El nombre de impresora es obligatorio.", "Printer name is required.") }
    require(profile.supportedTransports.isNotEmpty()) { tr("Selecciona al menos un transporte.", "Select at least one transport.") }
    val preferred = profile.preferredTransport ?: profile.supportedTransports.firstOrNull()
    require(preferred != null) { tr("Selecciona una ruta preferida.", "Choose a preferred transport.") }
    when (preferred) {
      TransportType.TCP_RAW -> {
        require(!profile.host.isNullOrBlank()) { tr("Host es obligatorio para impresión TCP.", "Host is required for TCP printing.") }
        require((profile.port ?: 0) > 0) { tr("El puerto debe ser mayor que 0 para TCP.", "Port must be greater than 0 for TCP printing.") }
      }
      TransportType.BT_SPP, TransportType.BT_DESKTOP -> {
        require(!profile.bluetoothMacAddress.isNullOrBlank()) {
          tr("La MAC Bluetooth es obligatoria para impresión Bluetooth.", "Bluetooth MAC is required for Bluetooth printing.")
        }
      }
    }
  }

  private fun validateProfileReadyToPrint(profile: PrinterProfile) {
    validateProfileDraft(profile)
    require(profile.isEnabled) { tr("Activa este perfil de impresora antes de imprimir.", "Enable this printer profile before printing.") }
  }

  private fun tr(spanish: String, english: String): String =
      if (currentLanguage == AppLanguage.Spanish) {
        spanish
      } else {
        english
      }
}
