package com.erpnext.pos.domain.usecases

import com.erpnext.pos.localSource.dao.CurrencyDailySalesTotal
import com.erpnext.pos.localSource.dao.CurrencySalesSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCurrencyMetricsCalculatorTest {

    @Test
    fun `build normalizes currency keys and keeps weekly totals for selected currency`() {
        val metrics = HomeCurrencyMetricsCalculator.build(
            HomeCurrencyMetricsCalculator.Input(
                dateStrings = listOf("2026-02-26", "2026-02-27"),
                yesterday = "2026-02-26",
                last7Dates = listOf("2026-02-26", "2026-02-27"),
                prev7Dates = listOf("2026-02-24", "2026-02-25"),
                availableCurrencies = listOf(" usd "),
                currencySummaries = listOf(
                    CurrencySalesSummary(
                        currency = "USD",
                        total = 7200.0,
                        invoices = 2,
                        customers = 2
                    )
                ),
                outstandingTotalsByCurrency = emptyList(),
                dailyTotalsByCurrency = listOf(
                    CurrencyDailySalesTotal(date = "2026-02-26", currency = "usd", total = 100.0),
                    CurrencyDailySalesTotal(date = "2026-02-27", currency = "USD", total = 7200.0)
                ),
                marginTodayByCurrency = emptyList(),
                marginLast7ByCurrency = emptyList(),
                costedTodayByCurrency = emptyList(),
                totalItemsTodayByCurrency = emptyList()
            )
        )

        assertEquals(1, metrics.size)
        assertEquals("USD", metrics.first().currency)
        assertEquals(7300.0, metrics.first().salesLast7, 0.0001)
    }

    @Test
    fun `build applies signed daily totals so returns reduce weekly sales`() {
        val metrics = HomeCurrencyMetricsCalculator.build(
            HomeCurrencyMetricsCalculator.Input(
                dateStrings = listOf("2026-02-26", "2026-02-27"),
                yesterday = "2026-02-26",
                last7Dates = listOf("2026-02-26", "2026-02-27"),
                prev7Dates = listOf("2026-02-24", "2026-02-25"),
                availableCurrencies = listOf("NIO"),
                currencySummaries = listOf(
                    CurrencySalesSummary(
                        currency = "NIO",
                        total = 5400.0,
                        invoices = 1,
                        customers = 1
                    )
                ),
                outstandingTotalsByCurrency = emptyList(),
                dailyTotalsByCurrency = listOf(
                    CurrencyDailySalesTotal(date = "2026-02-26", currency = "NIO", total = 7200.0),
                    CurrencyDailySalesTotal(date = "2026-02-27", currency = "nio", total = -1800.0)
                ),
                marginTodayByCurrency = emptyList(),
                marginLast7ByCurrency = emptyList(),
                costedTodayByCurrency = emptyList(),
                totalItemsTodayByCurrency = emptyList()
            )
        )

        assertEquals(1, metrics.size)
        assertEquals(5400.0, metrics.first().salesLast7, 0.0001)
    }

    @Test
    fun `build uses NIO for blank currency`() {
        val metrics = HomeCurrencyMetricsCalculator.build(
            HomeCurrencyMetricsCalculator.Input(
                dateStrings = listOf("2026-02-27"),
                yesterday = "2026-02-26",
                last7Dates = listOf("2026-02-27"),
                prev7Dates = listOf("2026-02-20"),
                availableCurrencies = emptyList(),
                currencySummaries = listOf(
                    CurrencySalesSummary(
                        currency = " ",
                        total = 1500.0,
                        invoices = 1,
                        customers = 1
                    )
                ),
                outstandingTotalsByCurrency = emptyList(),
                dailyTotalsByCurrency = listOf(
                    CurrencyDailySalesTotal(date = "2026-02-27", currency = "", total = 1500.0)
                ),
                marginTodayByCurrency = emptyList(),
                marginLast7ByCurrency = emptyList(),
                costedTodayByCurrency = emptyList(),
                totalItemsTodayByCurrency = emptyList()
            )
        )

        assertEquals(1, metrics.size)
        assertEquals("NIO", metrics.first().currency)
        assertEquals(1500.0, metrics.first().salesLast7, 0.0001)
    }
}
