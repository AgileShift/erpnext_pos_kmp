package com.erpnext.pos.domain.printing.errors

sealed class PrintingError(message: String) : Exception(message) {
  class UnsupportedTransport(details: String) :
      PrintingError("Transport is not supported: $details")

  class UnsupportedRenderer(details: String) :
      PrintingError("No renderer matches this printer profile: $details")

  class ConnectionFailed(details: String) :
      PrintingError("Unable to connect to printer: $details")

  class WriteFailed(details: String) :
      PrintingError("Unable to send data to printer: $details")

  class InvalidProfile(details: String) :
      PrintingError("Invalid printer profile: $details")
}
