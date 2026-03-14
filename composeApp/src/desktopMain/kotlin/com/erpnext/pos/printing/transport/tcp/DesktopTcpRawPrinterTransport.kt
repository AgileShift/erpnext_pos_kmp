package com.erpnext.pos.printing.transport.tcp

import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransports
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class DesktopTcpRawPrinterTransport : PrinterTransports {
    override val transportType: TransportType = TransportType.TCP_RAW

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(target: PrinterTarget): Result<Unit> = runCatching {
        require(target is PrinterTarget.TcpRaw) { "TCP target required." }
        val socket = Socket()
        socket.connect(InetSocketAddress(target.host, target.port), 4000)
        this.socket = socket
        outputStream = socket.getOutputStream()
    }

    override suspend fun write(bytes: ByteArray): Result<Unit> = runCatching {
        val out = outputStream ?: error("No active desktop TCP connection.")
        out.write(bytes)
        out.flush()
    }

    override suspend fun disconnect(): Result<Unit> = runCatching {
        outputStream?.close()
        socket?.close()
        outputStream = null
        socket = null
    }
}
