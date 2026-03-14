package com.erpnext.pos.domain.printing.platform

import com.erpnext.pos.domain.printing.model.TransportType

actual object PrintingPlatform {
  actual val capabilities: PrintingPlatformCapabilities =
      PrintingPlatformCapabilities(
          platform = PlatformKind.ANDROID,
          supportedTransports = setOf(TransportType.TCP_RAW, TransportType.BT_SPP),
          preferredTransportOrder =
              listOf(TransportType.TCP_RAW, TransportType.BT_SPP),
      )
}
