package com.erpnext.pos.domain.printing.ports

import com.erpnext.pos.domain.printing.model.TransportType

interface PrinterTransportFactory {
  fun getTransport(type: TransportType): PrinterTransports

  fun supports(type: TransportType): Boolean
}
