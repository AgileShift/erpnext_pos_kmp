package com.erpnext.pos.domain.printing.model

import kotlinx.serialization.Serializable

enum class PrinterFamily {
  RECEIPT,
  LABEL,
}

enum class PrinterLanguage {
  ESC_POS,
  ZPL,
}

enum class TransportType {
  TCP_RAW,
  BT_SPP,
  BT_DESKTOP,
}

enum class PrintJobStatus {
  PENDING,
  CONNECTING,
  RENDERING,
  PRINTING,
  SUCCESS,
  FAILED,
  RETRYING,
  CANCELLED,
}

enum class PrintAlignment {
  START,
  CENTER,
  END,
}

@Serializable
data class PrinterCapabilities(
    val paperWidthMm: Int = 80,
    val charactersPerLine: Int = 32,
    val codePage: String = "CP437",
    val autoCut: Boolean = true,
    val openDrawer: Boolean = false,
)

data class PrinterProfile(
    val id: String,
    val name: String,
    val brandHint: String? = null,
    val modelHint: String? = null,
    val family: PrinterFamily,
    val language: PrinterLanguage,
    val supportedTransports: Set<TransportType>,
    val preferredTransport: TransportType? = null,
    val host: String? = null,
    val port: Int? = null,
    val bluetoothMacAddress: String? = null,
    val bluetoothName: String? = null,
    val capabilities: PrinterCapabilities = PrinterCapabilities(),
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val notes: String? = null,
) {
  val paperWidthMm: Int get() = capabilities.paperWidthMm
  val charactersPerLine: Int get() = capabilities.charactersPerLine
  val codePage: String get() = capabilities.codePage
  val autoCut: Boolean get() = capabilities.autoCut
  val openDrawer: Boolean get() = capabilities.openDrawer
}

sealed interface PrinterTarget {
  data class TcpRaw(
      val host: String,
      val port: Int = 9100,
  ) : PrinterTarget

  data class BluetoothSpp(
      val macAddress: String,
      val deviceName: String? = null,
  ) : PrinterTarget

  data class DesktopBluetooth(
      val macAddress: String? = null,
      val deviceName: String? = null,
  ) : PrinterTarget
}

sealed interface PrintDocument {
  val documentId: String
  val family: PrinterFamily
}

data class ReceiptLine(
    val left: String,
    val right: String = "",
    val emphasis: Boolean = false,
)

data class ReceiptSection(
    val lines: List<String>,
    val alignment: PrintAlignment = PrintAlignment.CENTER,
)

data class ReceiptTotals(
    val subTotal: String? = null,
    val tax: String? = null,
    val total: String,
)

data class ReceiptDocument(
    override val documentId: String,
    val header: ReceiptSection = ReceiptSection(emptyList(), PrintAlignment.CENTER),
    val bodyLines: List<ReceiptLine>,
    val totals: ReceiptTotals,
    val footer: ReceiptSection = ReceiptSection(emptyList(), PrintAlignment.CENTER),
) : PrintDocument {
  override val family: PrinterFamily = PrinterFamily.RECEIPT
}

data class LabelField(
    val x: Int,
    val y: Int,
    val font: String = "A",
    val text: String,
)

data class LabelDocument(
    override val documentId: String,
    val widthDots: Int = 600,
    val heightDots: Int = 400,
    val fields: List<LabelField>,
) : PrintDocument {
  override val family: PrinterFamily = PrinterFamily.LABEL
}

data class PrintJob(
    val id: String,
    val profileId: String,
    val documentId: String,
    val documentType: String,
    val summary: String,
    val status: PrintJobStatus = PrintJobStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
)

data class PrintExecutionResult(
    val transportType: TransportType,
    val bytesWritten: Int,
    val jobId: String? = null,
)

data class DiscoveredPrinterDevice(
    val address: String,
    val name: String,
    val transportType: TransportType,
    val brandHint: String? = null,
    val modelHint: String? = null,
    val familyHint: PrinterFamily? = null,
    val languageHint: PrinterLanguage? = null,
    val paperWidthMmHint: Int? = null,
    val charactersPerLineHint: Int? = null,
    val codePageHint: String? = null,
    val confidenceLabel: String? = null,
)
