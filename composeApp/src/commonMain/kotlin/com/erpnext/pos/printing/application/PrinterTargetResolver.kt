package com.erpnext.pos.printing.application

import com.erpnext.pos.domain.printing.errors.PrintingError
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.platform.PrintingPlatform

data class ResolvedPrinterTarget(
    val transportType: TransportType,
    val target: PrinterTarget,
)

class PrinterTargetResolver {

  fun resolve(profile: PrinterProfile): ResolvedPrinterTarget {
    val platform = PrintingPlatform.capabilities
    val allowed =
        platform.preferredTransportOrder.filter { candidate ->
          candidate in profile.supportedTransports && candidate in platform.supportedTransports
        }

    val ordered = buildList {
      profile.preferredTransport?.takeIf { it in allowed }?.let(::add)
      allowed.filterNot { it == profile.preferredTransport }.forEach(::add)
    }

    ordered.forEach { type ->
      targetFor(type, profile)?.let { return ResolvedPrinterTarget(type, it) }
    }

    throw PrintingError.InvalidProfile(
        "No viable transport for profile=${profile.name} platform=${platform.platform}"
    )
  }

  private fun targetFor(type: TransportType, profile: PrinterProfile): PrinterTarget? {
    return when (type) {
      TransportType.TCP_RAW ->
          profile.host?.takeIf { it.isNotBlank() }?.let { host ->
            PrinterTarget.TcpRaw(host = host, port = profile.port ?: 9100)
          }
      TransportType.BT_SPP ->
          profile.bluetoothMacAddress?.takeIf { it.isNotBlank() }?.let { mac ->
            PrinterTarget.BluetoothSpp(macAddress = mac, deviceName = profile.bluetoothName)
          }
      TransportType.BT_DESKTOP ->
          if (
              !profile.bluetoothMacAddress.isNullOrBlank() || !profile.bluetoothName.isNullOrBlank()
          ) {
            PrinterTarget.DesktopBluetooth(
                macAddress = profile.bluetoothMacAddress,
                deviceName = profile.bluetoothName,
            )
          } else {
            null
          }
    }
  }
}
