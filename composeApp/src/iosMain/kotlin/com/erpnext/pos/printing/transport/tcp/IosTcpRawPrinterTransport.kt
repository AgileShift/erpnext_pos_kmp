package com.erpnext.pos.printing.transport.tcp

import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.ports.PrinterTransports
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.sizeOf
import platform.posix.AF_INET
import platform.posix.IPPROTO_TCP
import platform.posix.SOCK_STREAM
import platform.posix.close
import platform.posix.connect
import platform.posix.errno
import platform.posix.inet_addr
import platform.posix.send
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
class IosTcpRawPrinterTransport : PrinterTransports {
  override val transportType: TransportType = TransportType.TCP_RAW

  private var socketFd: Int = -1

  override suspend fun connect(target: PrinterTarget): Result<Unit> = runCatching {
    require(target is PrinterTarget.TcpRaw) { "TCP target required." }
    memScoped {
      val fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
      check(fd >= 0) { "socket() failed: ${lastError()}" }

      val address = alloc<sockaddr_in>()
      address.sin_family = AF_INET.convert()
      address.sin_port = hostToNetworkShort(target.port)
      address.sin_addr.s_addr = inet_addr(target.host)

      val connectResult =
          connect(fd, address.ptr.reinterpret<sockaddr>(), sizeOf<sockaddr_in>().convert())
      if (connectResult != 0) {
        close(fd)
        error("connect() failed: ${lastError()}")
      }
      socketFd = fd
    }
  }

  override suspend fun write(bytes: ByteArray): Result<Unit> = runCatching {
    check(socketFd >= 0) { "No active iOS TCP printer connection." }
    bytes.usePinned { pinned ->
      var offset = 0
      while (offset < bytes.size) {
        val sent = send(socketFd, pinned.addressOf(offset), (bytes.size - offset).convert(), 0)
        check(sent >= 0) { "send() failed: ${lastError()}" }
        offset += sent.toInt()
      }
    }
  }

  override suspend fun disconnect(): Result<Unit> = runCatching {
    if (socketFd >= 0) {
      close(socketFd)
      socketFd = -1
    }
  }

  private fun hostToNetworkShort(value: Int): UShort {
    val normalized = value and 0xFFFF
    return (((normalized and 0xFF) shl 8) or ((normalized shr 8) and 0xFF)).toUShort()
  }

  private fun lastError(): String = strerror(errno)?.toKString() ?: "errno=$errno"
}
