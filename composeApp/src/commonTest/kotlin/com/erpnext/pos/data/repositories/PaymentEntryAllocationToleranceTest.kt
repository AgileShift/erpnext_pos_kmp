package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.utils.buildCurrencySpecs
import com.erpnext.pos.utils.buildPaymentEntryDtoWithRateResolver
import com.erpnext.pos.utils.buildPaymentModeDetailMap
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.billing.PaymentLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PaymentEntryAllocationToleranceTest {

  @Test
  fun allocates_full_outstanding_when_fx_difference_is_within_payment_granularity() = runBlocking {
    val dto =
        buildPaymentEntryDtoWithRateResolver(
            rateResolver = { _, _ -> null },
            line =
                PaymentLine(
                    modeOfPayment = "Efectivo DOLARES",
                    enteredAmount = 13.34,
                    currency = "USD",
                    exchangeRate = 36.72,
                    baseAmount = 489.84,
                ),
            context = testContext(),
            customer = testCustomer(),
            postingDate = "2026-02-28",
            invoiceId = "SINV-0001",
            invoiceTotalRc = 490.0,
            outstandingRc = 490.0,
            paidFromAccount = "Debtors - LCDQ",
            partyAccountCurrency = "NIO",
            invoiceCurrency = "NIO",
            invoiceToReceivableRate = 1.0,
            exchangeRateByCurrency = emptyMap(),
            currencySpecs = buildCurrencySpecs(),
            paymentModeDetails = testPaymentModeDetails(),
        )

    assertEquals(490.0, dto.paidAmount)
    assertEquals(490.0, dto.references.first().allocatedAmount)
  }

  @Test
  fun keeps_partial_when_difference_exceeds_fx_tolerance() = runBlocking {
    val dto =
        buildPaymentEntryDtoWithRateResolver(
            rateResolver = { _, _ -> null },
            line =
                PaymentLine(
                    modeOfPayment = "Efectivo DOLARES",
                    enteredAmount = 13.33,
                    currency = "USD",
                    exchangeRate = 36.72,
                    baseAmount = 489.48,
                ),
            context = testContext(),
            customer = testCustomer(),
            postingDate = "2026-02-28",
            invoiceId = "SINV-0002",
            invoiceTotalRc = 490.0,
            outstandingRc = 490.0,
            paidFromAccount = "Debtors - LCDQ",
            partyAccountCurrency = "NIO",
            invoiceCurrency = "NIO",
            invoiceToReceivableRate = 1.0,
            exchangeRateByCurrency = emptyMap(),
            currencySpecs = buildCurrencySpecs(),
            paymentModeDetails = testPaymentModeDetails(),
        )

    assertTrue(dto.paidAmount < 490.0)
    assertTrue(490.0 - dto.paidAmount > 0.18)
    assertEquals(dto.paidAmount, dto.references.first().allocatedAmount)
  }

  private fun testCustomer(): CustomerBO =
      CustomerBO(
          name = "CUST-0001",
          customerName = "API ACTIVITY TEST 001",
          customerType = "Individual",
          partyAccountCurrency = "NIO",
          receivableAccount = "Debtors - LCDQ",
      )

  private fun testPaymentModeDetails(): Map<String, ModeOfPaymentEntity> =
      buildPaymentModeDetailMap(
          listOf(
              ModeOfPaymentEntity(
                  name = "Efectivo DOLARES",
                  modeOfPayment = "Efectivo DOLARES",
                  company = "La Casita del Queso",
                  type = "Cash",
                  currency = "USD",
                  account = "Caja USD - LCDQ",
              )
          )
      )

  private fun testContext(): POSContext =
      POSContext(
          cashier = UserBO(name = "test-cashier", username = "test-cashier"),
          username = "test-cashier",
          profileName = "Test POS Profile",
          company = "La Casita del Queso",
          companyCurrency = "NIO",
          allowNegativeStock = false,
          warehouse = null,
          route = null,
          territory = null,
          priceList = null,
          isCashBoxOpen = true,
          cashboxId = 1L,
          incomeAccount = null,
          expenseAccount = null,
          branch = null,
          applyDiscountOn = "Grand Total",
          currency = "NIO",
          partyAccountCurrency = "NIO",
          monthlySalesTarget = null,
          defaultReceivableAccount = "Debtors - LCDQ",
          defaultReceivableAccountCurrency = "NIO",
          exchangeRate = 36.72,
          allowedCurrencies = emptyList(),
          paymentModes = emptyList(),
          allowPartialPayment = true,
      )
}
