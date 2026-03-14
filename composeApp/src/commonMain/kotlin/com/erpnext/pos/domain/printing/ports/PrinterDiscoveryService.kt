package com.erpnext.pos.domain.printing.ports

import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice

interface PrinterDiscoveryService {
  suspend fun bondedDevices(): List<DiscoveredPrinterDevice>
}
