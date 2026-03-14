package com.erpnext.pos.views.printing

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine
import com.erpnext.pos.domain.printing.model.ReceiptSection
import com.erpnext.pos.domain.printing.model.ReceiptTotals
import com.erpnext.pos.domain.printing.usecase.PrintReceiptInput
import com.erpnext.pos.domain.printing.usecase.PrintReceiptUseCase
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PrinterSettingsViewModel(
    private val printReceiptUseCase: PrintReceiptUseCase
) : BaseViewModel() {
    fun printTest(profileId: String, preferBluetooth: Boolean) {
        viewModelScope.launch {
            val testDoc = ReceiptDocument(
                documentId = "test-${Clock.System.now().epochSeconds}",
                header = ReceiptSection(listOf("Clothing Center", "TEST DE IMPRESION"), PrintAlignment.CENTER),
                bodyLines = listOf(
                    ReceiptLine("Producto demo", "C$ 100.00"),
                    ReceiptLine("Producto demo 2", "C$ 50.00"),
                    ReceiptLine("Producto demo 3", "C$ 1050.00")
                ),
                totals = ReceiptTotals(
                    subTotal = "C$ 1200.00",
                    tax = "C$ 0.00",
                    total = "C$ 1200.00"
                ),
                footer = ReceiptSection(listOf("Gracias por su compra"), PrintAlignment.CENTER)
            )

            printReceiptUseCase(
                PrintReceiptInput(
                    profileId = profileId,
                    document = testDoc,
                )
            )
        }
    }
}
