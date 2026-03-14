package com.erpnext.pos.domain.printing.platform

import com.erpnext.pos.domain.printing.model.TransportType

enum class PlatformKind {
  ANDROID,
  DESKTOP,
  IOS,
}

data class PrintingPlatformCapabilities(
    val platform: PlatformKind,
    val supportedTransports: Set<TransportType>,
    val preferredTransportOrder: List<TransportType>,
)

expect object PrintingPlatform {
  val capabilities: PrintingPlatformCapabilities
}
