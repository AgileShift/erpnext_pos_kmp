package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SalesInvoicePaymentEntryPayloadTest {

  @Test
  fun stripsEmbeddedPaymentsForCashFlow() {
    val source = fixture(total = 381.0, paid = 381.0, outstanding = 0.0)
    val sanitized = sanitizeInvoiceForPaymentEntry(source)

    assertTrue(sanitized.payments.isEmpty())
    assertEquals(0.0, sanitized.paidAmount)
    assertEquals(381.0, sanitized.outstandingAmount)
    assertEquals("Unpaid", sanitized.status)
    assertNull(sanitized.changeAmount)
    assertFalse(sanitized.isPos)
    assertEquals(false, sanitized.isCreatedUsingPos)
    assertNull(sanitized.posProfile)
    assertNull(sanitized.posOpeningEntry)
  }

  @Test
  fun stripsEmbeddedPaymentsForPartialFlow() {
    val source = fixture(total = 381.0, paid = 300.0, outstanding = 81.0)
    val sanitized = sanitizeInvoiceForPaymentEntry(source)

    assertTrue(sanitized.payments.isEmpty())
    assertEquals(0.0, sanitized.paidAmount)
    assertEquals(381.0, sanitized.outstandingAmount)
    assertEquals("Unpaid", sanitized.status)
    assertFalse(sanitized.isPos)
    assertEquals(false, sanitized.isCreatedUsingPos)
    assertNull(sanitized.posProfile)
    assertNull(sanitized.posOpeningEntry)
  }

  @Test
  fun keepsCreditFullAsUnpaidWithoutPayments() {
    val source = fixture(total = 220.0, paid = 0.0, outstanding = 220.0, payments = emptyList())
    val sanitized = sanitizeInvoiceForPaymentEntry(source)

    assertTrue(sanitized.payments.isEmpty())
    assertEquals(0.0, sanitized.paidAmount)
    assertEquals(220.0, sanitized.outstandingAmount)
    assertEquals("Unpaid", sanitized.status)
    assertFalse(sanitized.isPos)
    assertEquals(false, sanitized.isCreatedUsingPos)
    assertNull(sanitized.posProfile)
    assertNull(sanitized.posOpeningEntry)
  }

  private fun fixture(
      total: Double,
      paid: Double,
      outstanding: Double,
      payments: List<SalesInvoicePaymentDto> =
          listOf(
              SalesInvoicePaymentDto(
                  modeOfPayment = "Efectivo CORDOBAS",
                  amount = paid,
                  account = "Caja Chica Moneda Extrangera - LCDQ",
                  paymentReference = "POSPAY-1",
              )
          ),
  ): SalesInvoiceDto {
    return SalesInvoiceDto(
        customer = "API ACTIVITY TEST 001",
        customerName = "API ACTIVITY TEST 001",
        company = "La Casita del Queso",
        postingDate = "2026-02-28",
        dueDate = "2026-02-28",
        status = "Partly Paid",
        grandTotal = total,
        outstandingAmount = outstanding,
        netTotal = total,
        paidAmount = paid,
        payments = payments,
        isPos = true,
        posProfile = "Test POS Profile",
        posOpeningEntry = "POS-OPE-2026-00017",
        currency = "NIO",
        partyAccountCurrency = "NIO",
    )
  }
}
