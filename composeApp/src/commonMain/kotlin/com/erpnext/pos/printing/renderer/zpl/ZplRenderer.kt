package com.erpnext.pos.printing.renderer.zpl

import com.erpnext.pos.domain.printing.model.LabelDocument
import com.erpnext.pos.domain.printing.model.PrintDocument
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.ports.PrintRenderer

class ZplRenderer : PrintRenderer {
  override fun supports(profile: PrinterProfile, document: PrintDocument): Boolean {
    return profile.family == PrinterFamily.LABEL &&
        profile.language == PrinterLanguage.ZPL &&
        document is LabelDocument
  }

  override fun render(profile: PrinterProfile, document: PrintDocument): ByteArray {
    require(document is LabelDocument) { "ZplRenderer only supports LabelDocument." }
    val payload =
        buildString {
          append("^XA\n")
          append("^PW${document.widthDots}\n")
          append("^LL${document.heightDots}\n")
          document.fields.forEach { field ->
            append("^FO${field.x},${field.y}^A${field.font},30,30^FD${field.text}^FS\n")
          }
          append("^XZ")
        }
    return payload.encodeToByteArray()
  }
}
