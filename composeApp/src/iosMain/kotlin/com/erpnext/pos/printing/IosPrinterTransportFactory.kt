package com.erpnext.pos.printing

import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransportFactory
import com.erpnext.pos.domain.printing.ports.PrinterTransports
import com.erpnext.pos.printing.transport.tcp.IosTcpRawPrinterTransport

class IosPrinterTransportFactory : PrinterTransportFactory {

    private val tcp by lazy { IosTcpRawPrinterTransport() }

    override fun getTransport(type: TransportType): PrinterTransports {
        return when (type) {
            TransportType.TCP_RAW -> tcp
            TransportType.BT_SPP -> error("Bluetooth SPP is not supported on iOS baseline.")
            TransportType.BT_DESKTOP -> error("Desktop Bluetooth transport is not supported on iOS.")
        }
    }

    override fun supports(type: TransportType): Boolean = type == TransportType.TCP_RAW
}
