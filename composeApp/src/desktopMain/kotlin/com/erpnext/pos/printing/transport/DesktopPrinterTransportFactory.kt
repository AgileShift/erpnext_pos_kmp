package com.erpnext.pos.printing.transport

import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransportFactory
import com.erpnext.pos.domain.printing.ports.PrinterTransports
import com.erpnext.pos.printing.transport.bluetooth.DesktopBluetoothPrinterTransport
import com.erpnext.pos.printing.transport.tcp.DesktopTcpRawPrinterTransport

class DesktopPrinterTransportFactory : PrinterTransportFactory {

    private val tcp by lazy { DesktopTcpRawPrinterTransport() }
    private val bluetooth by lazy { DesktopBluetoothPrinterTransport() }

    override fun getTransport(type: TransportType): PrinterTransports {
        return when (type) {
            TransportType.TCP_RAW -> tcp
            TransportType.BT_SPP -> error("Android Bluetooth SPP is not available on desktop.")
            TransportType.BT_DESKTOP -> bluetooth
        }
    }

    override fun supports(type: TransportType): Boolean =
        type == TransportType.TCP_RAW || type == TransportType.BT_DESKTOP
}
