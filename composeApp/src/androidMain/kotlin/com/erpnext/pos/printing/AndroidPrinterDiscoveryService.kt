package com.erpnext.pos.printing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.RequiresPermission
import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterDiscoveryService
import com.erpnext.pos.printing.discovery.PrinterFingerprintHeuristics
import com.erpnext.pos.utils.AppLogger

class AndroidPrinterDiscoveryService : PrinterDiscoveryService {
  @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
  override suspend fun bondedDevices(): List<DiscoveredPrinterDevice> {
    val adapter =
        BluetoothAdapter.getDefaultAdapter()
            ?: throw IllegalStateException("Bluetooth is not available on this Android device.")
    if (!adapter.isEnabled) {
      throw IllegalStateException("Bluetooth is turned off. Turn it on and try discovery again.")
    }

    return try {
      val bondedDevices = adapter.bondedDevices.orEmpty()
      val printerDevices = bondedDevices.filter(::isLikelyPrinter)
      AppLogger.info(
          "AndroidPrinterDiscoveryService.bondedDevices -> bonded=${bondedDevices.size}, printerCandidates=${printerDevices.size}"
      )
      printerDevices.map { device ->
        val hints = PrinterFingerprintHeuristics.fromDeviceName(device.name)
        DiscoveredPrinterDevice(
            address = device.address.orEmpty(),
            name = hints.displayName,
            transportType = TransportType.BT_SPP,
            brandHint = hints.brandHint,
            modelHint = hints.modelHint,
            familyHint = hints.familyHint,
            languageHint = hints.languageHint,
            paperWidthMmHint = hints.paperWidthMmHint,
            charactersPerLineHint = hints.charactersPerLineHint,
            codePageHint = hints.codePageHint,
            confidenceLabel = hints.confidenceLabel,
        )
      }.sortedBy { it.name }
    } catch (securityException: SecurityException) {
      val permissionName =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
          } else {
            Manifest.permission.BLUETOOTH
          }
      throw IllegalStateException(
          "Bluetooth discovery permission is missing ($permissionName).",
          securityException,
      )
    } catch (error: Throwable) {
      throw IllegalStateException(
          "Unable to read bonded Bluetooth printers on Android.",
          error,
      )
    }
  }

  private fun isLikelyPrinter(device: BluetoothDevice): Boolean {
    val deviceName = device.name.orEmpty()
    val bluetoothClass = device.bluetoothClass
    val classMatches = bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.IMAGING
    val printerNameMatches = PrinterFingerprintHeuristics.looksLikePrinterName(deviceName)
    val excludedByName = PrinterFingerprintHeuristics.looksLikeNonPrinterName(deviceName)

    return !excludedByName && (classMatches || printerNameMatches)
  }
}
