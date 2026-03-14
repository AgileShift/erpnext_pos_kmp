package com.erpnext.pos.printing.renderer.escpos

import com.erpnext.pos.domain.printing.model.PrintDocument
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.ports.PrintRenderer
import com.erpnext.pos.printing.formatting.ReceiptFormatter

class EscPosRenderer(
    private val formatter: ReceiptFormatter,
) : PrintRenderer {
  override fun supports(profile: PrinterProfile, document: PrintDocument): Boolean {
    return profile.family == PrinterFamily.RECEIPT &&
        profile.language == PrinterLanguage.ESC_POS &&
        document is ReceiptDocument
  }

  override fun render(profile: PrinterProfile, document: PrintDocument): ByteArray {
    require(document is ReceiptDocument) { "EscPosRenderer only supports ReceiptDocument." }

    val lines = formatter.format(document, profile.charactersPerLine)
    val buffer = mutableListOf<Byte>()
    buffer += EscPosCommands.INIT.toList()

    lines.forEach { line ->
      val isHeaderCentered = document.header.lines.any { header -> line.trim() == header.trim() }
      if (isHeaderCentered) {
        buffer += EscPosCommands.ALIGN_CENTER.toList()
      } else {
        buffer += EscPosCommands.ALIGN_LEFT.toList()
      }
      if (line.trimStart().startsWith("TOTAL")) {
        buffer += EscPosCommands.BOLD_ON.toList()
      }
      buffer += line.encodeToByteArray().toList()
      buffer += EscPosCommands.LF.toList()
      if (line.trimStart().startsWith("TOTAL")) {
        buffer += EscPosCommands.BOLD_OFF.toList()
      }
    }

    buffer += EscPosCommands.FEED_3.toList()
    if (profile.openDrawer) buffer += EscPosCommands.OPEN_DRAWER.toList()
    if (profile.autoCut) buffer += EscPosCommands.CUT.toList()
    return buffer.toByteArray()
  }
}
