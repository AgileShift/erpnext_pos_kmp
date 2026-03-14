package com.erpnext.pos.printing.transport.tcp

import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransports
import com.erpnext.pos.utils.AppLogger
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class AndroidTcpRawPrinterTransport : PrinterTransports {
    override val transportType: TransportType = TransportType.TCP_RAW

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(target: PrinterTarget): Result<Unit> = runCatching {
        require(target is PrinterTarget.TcpRaw) { "TCP target required." }
        AppLogger.info("AndroidTcpRawPrinterTransport.connect -> host=${target.host}, port=${target.port}")

        val s = Socket()
        s.connect(InetSocketAddress(target.host, target.port), 4000)
        socket = s
        outputStream = s.getOutputStream()
    }.onFailure { error ->
        AppLogger.warn("AndroidTcpRawPrinterTransport.connect failed", error, reportToSentry = false)
    }

    override suspend fun write(bytes: ByteArray): Result<Unit> = runCatching{
        val out = outputStream ?: error("No existe conexion activa TCP")
        AppLogger.info("AndroidTcpRawPrinterTransport.write -> bytes=${bytes.size}")
        out.write(bytes)
        out.flush()
    }.onFailure { error ->
        AppLogger.warn("AndroidTcpRawPrinterTransport.write failed", error, reportToSentry = false)
    }

    override suspend fun disconnect(): Result<Unit>  = runCatching{
        outputStream?.close()
        socket?.close()
        outputStream = null
        socket = null
    }.onFailure { error ->
        AppLogger.warn("AndroidTcpRawPrinterTransport.disconnect failed", error, reportToSentry = false)
    }
}
