package com.erpnext.pos.printing.transport.bluetooth

import com.erpnext.pos.domain.printing.errors.PrintingError
import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransports

class DesktopBluetoothPrinterTransport : PrinterTransports {
  override val transportType: TransportType = TransportType.BT_DESKTOP

  override suspend fun connect(target: PrinterTarget): Result<Unit> {
    return Result.failure(
        PrintingError.UnsupportedTransport(
            "Desktop Bluetooth is not bundled because generic JVM support is not portable across macOS, Windows, and Linux without extra native dependencies. Use TCP/IP RAW as the baseline transport."
        )
    )
  }

  override suspend fun write(bytes: ByteArray): Result<Unit> = Result.failure(
      PrintingError.UnsupportedTransport("Desktop Bluetooth transport is not connected.")
  )

  override suspend fun disconnect(): Result<Unit> = Result.success(Unit)
}
