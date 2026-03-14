package com.erpnext.pos.domain.printing.ports

import com.erpnext.pos.domain.printing.model.PrintDocument
import com.erpnext.pos.domain.printing.model.PrinterProfile

interface PrintRenderer {
    fun supports(profile: PrinterProfile, document: PrintDocument): Boolean
    fun render(profile: PrinterProfile, document: PrintDocument): ByteArray
}