package com.erpnext.pos.printing

import com.erpnext.pos.domain.printing.ports.PrinterDiscoveryService
import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice

class DesktopPrinterDiscoveryService : PrinterDiscoveryService {
  override suspend fun bondedDevices(): List<DiscoveredPrinterDevice> = emptyList()
}
