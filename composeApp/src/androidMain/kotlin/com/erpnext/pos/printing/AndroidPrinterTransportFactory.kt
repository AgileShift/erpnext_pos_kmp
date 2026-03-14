package com.erpnext.pos.printing

import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransportFactory
import com.erpnext.pos.domain.printing.ports.PrinterTransports
import com.erpnext.pos.printing.transport.ble.AndroidBluetoothSppPrinterTransport
import com.erpnext.pos.printing.transport.tcp.AndroidTcpRawPrinterTransport

class AndroidPrinterTransportFactory : PrinterTransportFactory {

    private val tcp by lazy { AndroidTcpRawPrinterTransport() }
    private val ble by lazy { AndroidBluetoothSppPrinterTransport() }

    override fun getTransport(type: TransportType): PrinterTransports {
        return when (type) {
            TransportType.TCP_RAW -> tcp
            TransportType.BT_SPP -> ble
            TransportType.BT_DESKTOP -> error("Desktop Bluetooth transport is not available on Android.")
        }
    }

    override fun supports(type: TransportType): Boolean =
        type == TransportType.TCP_RAW || type == TransportType.BT_SPP
}
