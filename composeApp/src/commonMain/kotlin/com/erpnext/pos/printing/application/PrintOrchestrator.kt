package com.erpnext.pos.printing.application

import com.erpnext.pos.domain.printing.model.PrintDocument
import com.erpnext.pos.domain.printing.model.PrintExecutionResult
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransportFactory
import com.erpnext.pos.utils.AppLogger
import kotlinx.coroutines.delay

class PrintOrchestrator(
    private val rendererSelector: RendererSelector,
    private val targetResolver: PrinterTargetResolver,
    private val transportFactory: PrinterTransportFactory,
) {
  suspend fun print(
      profile: PrinterProfile,
      document: PrintDocument,
  ): Result<PrintExecutionResult> {
    return runCatching {
      AppLogger.info(
          "PrintOrchestrator.print -> profile=${profile.name}, document=${document.documentId}, family=${document.family}"
      )
      val renderer = rendererSelector.select(profile, document)
      val payload = renderer.render(profile, document)
      val resolvedTarget = targetResolver.resolve(profile)
      AppLogger.info(
          "PrintOrchestrator.print -> resolved transport=${resolvedTarget.transportType}, target=${resolvedTarget.target.describeForLogs()}"
      )
      require(transportFactory.supports(resolvedTarget.transportType)) {
        "Transport ${resolvedTarget.transportType} not available on this platform."
      }
      val transport = transportFactory.getTransport(resolvedTarget.transportType)
      transport.connect(resolvedTarget.target).getOrThrow()
      AppLogger.info("PrintOrchestrator.print -> transport connected")
      try {
        transport.write(payload).getOrThrow()
        AppLogger.info("PrintOrchestrator.print -> bytes written=${payload.size}")
        if (resolvedTarget.transportType == TransportType.BT_SPP) {
          // Some mobile Bluetooth printers need a short drain window before the RFCOMM
          // socket closes, otherwise the job can be accepted but never physically printed.
          delay(500)
          AppLogger.info("PrintOrchestrator.print -> waited 500ms before Bluetooth disconnect")
        }
      } finally {
        transport.disconnect()
            .onFailure { error ->
              AppLogger.warn("PrintOrchestrator.print -> disconnect failed", error, reportToSentry = false)
            }
      }
      PrintExecutionResult(
          transportType = resolvedTarget.transportType,
          bytesWritten = payload.size,
      )
    }.onFailure { error ->
      AppLogger.warn(
          "PrintOrchestrator.print failed for profile=${profile.name}, document=${document.documentId}",
          error,
          reportToSentry = false,
      )
    }
  }
}

private fun com.erpnext.pos.domain.printing.model.PrinterTarget.describeForLogs(): String =
    when (this) {
      is com.erpnext.pos.domain.printing.model.PrinterTarget.TcpRaw -> "tcp:$host:$port"
      is com.erpnext.pos.domain.printing.model.PrinterTarget.BluetoothSpp -> "bt:${deviceName ?: macAddress}@$macAddress"
      is com.erpnext.pos.domain.printing.model.PrinterTarget.DesktopBluetooth -> "desktop-bt:${deviceName ?: macAddress.orEmpty()}"
    }
