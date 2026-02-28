package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SalesInvoiceMapperTest {

  @Test
  fun cashSale_mapsAsPosAndPreservesPaymentAmount() {
    val source = fixture(grandTotal = 256.0, paidAmount = 256.0, outstandingAmount = 0.0, isPos = true)
    val dto = source.toDto()

    assertTrue(dto.isPos)
    assertEquals("POS-A", dto.posProfile)
    assertEquals("POS-OPE-1", dto.posOpeningEntry)
    assertEquals(1, dto.payments.size)
    assertEquals(256.0, dto.payments.first().amount)
    assertEquals(256.0, dto.paidAmount)
    assertEquals(0.0, dto.outstandingAmount)
  }

  @Test
  fun partialSale_mapsAsPosAndUsesEnteredPayment() {
    val source = fixture(grandTotal = 381.0, paidAmount = 300.0, outstandingAmount = 81.0, isPos = true)
    val dto = source.toDto()

    assertTrue(dto.isPos)
    assertEquals(1, dto.payments.size)
    assertEquals(300.0, dto.payments.first().amount)
    assertEquals(300.0, dto.paidAmount)
    assertEquals(81.0, dto.outstandingAmount)
  }

  @Test
  fun fullCreditSale_forcesNonPosAndClearsPosReconciliationFields() {
    val source = fixture(grandTotal = 220.0, paidAmount = 0.0, outstandingAmount = 220.0, isPos = true)
    val dto = source.toDto()

    assertFalse(dto.isPos)
    assertTrue(dto.payments.isEmpty())
    assertNull(dto.posProfile)
    assertNull(dto.posOpeningEntry)
    assertEquals(0.0, dto.paidAmount)
    assertEquals(220.0, dto.outstandingAmount)
  }

  private fun fixture(
      grandTotal: Double,
      paidAmount: Double,
      outstandingAmount: Double,
      isPos: Boolean,
  ): SalesInvoiceWithItemsAndPayments {
    val invoiceName = "LOCAL-1"
    return SalesInvoiceWithItemsAndPayments(
        invoice =
            SalesInvoiceEntity(
                invoiceName = invoiceName,
                profileId = "POS-A",
                customer = "CUST-001",
                customerName = "API ACTIVITY TEST 001",
                company = "La Casita del Queso",
                postingDate = "2026-02-28",
                dueDate = "2026-02-28",
                netTotal = grandTotal,
                grandTotal = grandTotal,
                paidAmount = paidAmount,
                outstandingAmount = outstandingAmount,
                status = if (outstandingAmount > 0.0) "Unpaid" else "Paid",
                isPos = isPos,
                posOpeningEntry = "POS-OPE-1",
                currency = "NIO",
                partyAccountCurrency = "NIO",
            ),
        items =
            listOf(
                SalesInvoiceItemEntity(
                    parentInvoice = invoiceName,
                    itemCode = "ITEM-001",
                    itemName = "Test item",
                    qty = 1.0,
                    rate = grandTotal,
                    amount = grandTotal,
                    warehouse = "WH-1",
                )
            ),
        payments =
            listOf(
                POSInvoicePaymentEntity(
                    parentInvoice = invoiceName,
                    modeOfPayment = "Efectivo CORDOBAS",
                    amount = paidAmount,
                    enteredAmount = paidAmount,
                    paymentCurrency = "NIO",
                    paymentReference = "POSPAY-001",
                )
            ),
    )
  }
}
