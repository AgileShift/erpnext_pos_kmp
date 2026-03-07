package com.erpnext.pos.utils

import com.erpnext.pos.localSource.dao.ShiftPaymentRow
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PaymentReconciliationBuilderTest {

  @Test
  fun doesNotDoubleCountSinglePaymentWhenBaseAmountIsZero() = runBlocking {
    val opening = 2000.0
    val paymentAmount = 1598.0
    val seeds =
        buildPaymentReconciliationSeeds(
            balanceDetails =
                listOf(
                    BalanceDetailsEntity(
                        cashboxId = 1L,
                        modeOfPayment = "Efectivo CORDOBAS",
                        openingAmount = opening,
                    )
                ),
            paymentRows =
                listOf(
                    ShiftPaymentRow(
                        invoiceName = "ACC-SINV-001",
                        modeOfPayment = "Efectivo CORDOBAS",
                        amount = 0.0,
                        enteredAmount = paymentAmount,
                        paymentCurrency = "NIO",
                        exchangeRate = 1.0,
                        invoiceCurrency = "NIO",
                        partyAccountCurrency = "NIO",
                    )
                ),
            invoices =
                listOf(
                    SalesInvoiceEntity(
                        invoiceName = "ACC-SINV-001",
                        customer = "CUST-001",
                        company = "COMPANY",
                        postingDate = "2026-03-06",
                        currency = "NIO",
                        partyAccountCurrency = "NIO",
                        paidAmount = paymentAmount,
                        outstandingAmount = 0.0,
                    )
                ),
            modeCurrency = mapOf("Efectivo CORDOBAS" to "NIO"),
            posCurrency = "NIO",
            rateResolver = { _, _ -> 1.0 },
        )

    val cashSeed = seeds.first { it.modeOfPayment == "Efectivo CORDOBAS" }
    assertEquals(3598.0, cashSeed.expectedAmount)
  }
}
