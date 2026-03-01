package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentScheduleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SalesInvoicePaymentEntryPayloadTest {

  @Test
  fun cashFlow_keepsExistingBehaviorWithoutInvalidSchedule() {
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
    assertTrue(sanitized.paymentSchedule.isEmpty())
  }

  @Test
  fun partialFlow_keepsExistingBehaviorWithoutInvalidSchedule() {
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
    assertTrue(sanitized.paymentSchedule.isEmpty())
  }

  @Test
  fun creditFull_withoutPaymentSchedule_isValid() {
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
    assertTrue(sanitized.paymentSchedule.isEmpty())
  }

  @Test
  fun validPaymentSchedule_invoicePortion100_isKept() {
    val source =
        fixture(
            total = 220.0,
            paid = 10.0,
            outstanding = 210.0,
            paymentSchedule =
                listOf(
                    SalesInvoicePaymentScheduleDto(
                        paymentTerm = "Net 30",
                        invoicePortion = 100.0,
                        dueDate = "2026-03-30",
                    )
                ),
            paymentTermsTemplate = "Net 30",
        )

    val sanitized = sanitizeInvoiceForPaymentEntry(source)

    assertEquals(1, sanitized.paymentSchedule.size)
    assertEquals(100.0, sanitized.paymentSchedule.first().invoicePortion)
    assertEquals("Net 30", sanitized.paymentTermsTemplate)
  }

  @Test
  fun invalidPaymentSchedule_rowWithoutPortionOrAmount_failsLocally() {
    val source =
        fixture(
            total = 220.0,
            paid = 10.0,
            outstanding = 210.0,
            paymentSchedule =
                listOf(
                    SalesInvoicePaymentScheduleDto(
                        paymentTerm = "Invalid",
                        invoicePortion = 0.0,
                        dueDate = "2026-03-30",
                        paymentAmount = null,
                    )
                ),
        )

    val error = assertFailsWith<IllegalArgumentException> { sanitizeInvoiceForPaymentEntry(source) }
    assertTrue(error.message.orEmpty().contains("payment_schedule inválido"))
  }

  private fun fixture(
      total: Double,
      paid: Double,
      outstanding: Double,
      paymentSchedule: List<SalesInvoicePaymentScheduleDto> = emptyList(),
      paymentTermsTemplate: String? = null,
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
        paymentSchedule = paymentSchedule,
        paymentTermsTemplate = paymentTermsTemplate,
        isPos = true,
        posProfile = "Test POS Profile",
        posOpeningEntry = "POS-OPE-2026-00017",
        currency = "NIO",
        partyAccountCurrency = "NIO",
    )
  }
}
