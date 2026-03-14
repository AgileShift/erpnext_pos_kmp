package com.erpnext.pos.domain.printing.ports

import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType

interface PrinterTransports {
  val transportType: TransportType

  suspend fun connect(target: PrinterTarget): Result<Unit>

  suspend fun write(bytes: ByteArray): Result<Unit>

  suspend fun disconnect(): Result<Unit>
}
