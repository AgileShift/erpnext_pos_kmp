package com.erpnext.pos.printing

import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice
import com.erpnext.pos.domain.printing.ports.PrinterDiscoveryService

class IosPrinterDiscoveryService : PrinterDiscoveryService {
  override suspend fun bondedDevices(): List<DiscoveredPrinterDevice> = emptyList()
}
