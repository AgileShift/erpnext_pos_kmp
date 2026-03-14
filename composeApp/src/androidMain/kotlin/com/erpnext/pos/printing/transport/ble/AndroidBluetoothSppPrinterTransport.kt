package com.erpnext.pos.printing.transport.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransports
import com.erpnext.pos.utils.AppLogger
import java.util.UUID

class AndroidBluetoothSppPrinterTransport : PrinterTransports {
    override val transportType: TransportType = TransportType.BT_SPP

    companion object {
        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ]
    )
    override suspend fun connect(target: PrinterTarget): Result<Unit> = runCatching {
        require(target is PrinterTarget.BluetoothSpp) { "Bluetooth SPP target required." }
        AppLogger.info("AndroidBluetoothSppPrinterTransport.connect -> target=${target.deviceName ?: target.macAddress}")

        val btAdapter = adapter ?: error("Bluetooth no disponible en dispositivo")
        val device = btAdapter.bondedDevices.firstOrNull { it.address == target.macAddress }
            ?: error("No se encontró dispositivo emparejado con MAC ${target.macAddress}")

        btAdapter.cancelDiscovery()
        val btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        btSocket.connect()
        socket = btSocket
    }.onFailure { error ->
        AppLogger.warn("AndroidBluetoothSppPrinterTransport.connect failed", error, reportToSentry = false)
    }

    override suspend fun write(bytes: ByteArray): Result<Unit> = runCatching {
        val out = socket?.outputStream ?: error("No existe conexión Bluetooth activa.")
        AppLogger.info("AndroidBluetoothSppPrinterTransport.write -> bytes=${bytes.size}")
        out.write(bytes)
        out.flush()
    }.onFailure { error ->
        AppLogger.warn("AndroidBluetoothSppPrinterTransport.write failed", error, reportToSentry = false)
    }

    override suspend fun disconnect(): Result<Unit> = runCatching {
        socket?.close()
        socket = null
    }.onFailure { error ->
        AppLogger.warn("AndroidBluetoothSppPrinterTransport.disconnect failed", error, reportToSentry = false)
    }
}
