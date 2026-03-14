package com.erpnext.pos.printing

import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine
import com.erpnext.pos.domain.printing.model.ReceiptSection
import com.erpnext.pos.domain.printing.model.ReceiptTotals
import com.erpnext.pos.printing.formatting.ReceiptFormatter
import kotlin.test.Test
import kotlin.test.assertTrue

class ReceiptFormatterTest {
  private val formatter = ReceiptFormatter()

  @Test
  fun `format wraps long left column without losing totals`() {
    val result =
        formatter.format(
            ReceiptDocument(
                documentId = "receipt-1",
                header = ReceiptSection(listOf("Store"), PrintAlignment.CENTER),
                bodyLines =
                    listOf(
                        ReceiptLine(
                            left = "Very long product description for printing",
                            right = "C$ 100.00",
                        )
                    ),
                totals = ReceiptTotals(subTotal = "C$ 100.00", tax = "C$ 0.00", total = "C$ 100.00"),
            ),
            lineWidth = 16,
        )

    assertTrue(result.any { it.contains("produc") })
    assertTrue(result.last { it.contains("TOTAL") }.contains("C$ 100.00"))
  }
}
