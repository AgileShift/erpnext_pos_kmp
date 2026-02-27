package com.erpnext.pos.domain.usecases

import com.erpnext.pos.localSource.dao.CurrencyDailySalesTotal
import com.erpnext.pos.localSource.dao.CurrencyItemCount
import com.erpnext.pos.localSource.dao.CurrencyMarginTotal
import com.erpnext.pos.localSource.dao.CurrencyOutstandingTotal
import com.erpnext.pos.localSource.dao.CurrencySalesSummary
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.views.home.CurrencyHomeMetric
import com.erpnext.pos.views.home.DailyMetric

internal object HomeCurrencyMetricsCalculator {

    data class Input(
        val dateStrings: List<String>,
        val yesterday: String,
        val last7Dates: List<String>,
        val prev7Dates: List<String>,
        val availableCurrencies: List<String>,
        val currencySummaries: List<CurrencySalesSummary>,
        val outstandingTotalsByCurrency: List<CurrencyOutstandingTotal>,
        val dailyTotalsByCurrency: List<CurrencyDailySalesTotal>,
        val marginTodayByCurrency: List<CurrencyMarginTotal>,
        val marginLast7ByCurrency: List<CurrencyMarginTotal>,
        val costedTodayByCurrency: List<CurrencyItemCount>,
        val totalItemsTodayByCurrency: List<CurrencyItemCount>
    )

    fun build(input: Input): List<CurrencyHomeMetric> {
        val summaries = aggregateSummariesByCurrency(input.currencySummaries)
        val outstanding = aggregateDoubleByCurrency(input.outstandingTotalsByCurrency) { it.total }
        val dailyTotals = aggregateDailyTotalsByCurrency(input.dailyTotalsByCurrency)
        val marginToday = aggregateDoubleByCurrency(input.marginTodayByCurrency) { it.margin }
        val marginLast7 = aggregateDoubleByCurrency(input.marginLast7ByCurrency) { it.margin }
        val costedToday = aggregateIntByCurrency(input.costedTodayByCurrency) { it.count }
        val totalItemsToday = aggregateIntByCurrency(input.totalItemsTodayByCurrency) { it.count }

        val currencies = buildSet {
            input.availableCurrencies.forEach { add(normalizeCurrency(it)) }
            summaries.keys.forEach { add(it) }
            outstanding.keys.forEach { add(it) }
            dailyTotals.keys.forEach { add(it) }
            marginToday.keys.forEach { add(it) }
            marginLast7.keys.forEach { add(it) }
        }.filter { it.isNotBlank() }.sorted()

        return currencies.map { currency ->
            val summary = summaries[currency]
            val dailyTotalsForCurrency = dailyTotals[currency].orEmpty()
            val totalSalesToday = summary?.total ?: 0.0
            val invoicesToday = summary?.invoices ?: 0
            val customersToday = summary?.customers ?: 0
            val avgTicket = if (invoicesToday > 0) totalSalesToday / invoicesToday else 0.0

            val weekSeries = input.dateStrings.map { date ->
                DailyMetric(date = date, total = dailyTotalsForCurrency[date] ?: 0.0)
            }

            val salesYesterday = dailyTotalsForCurrency[input.yesterday] ?: 0.0
            val salesLast7 = input.last7Dates.sumOf { dailyTotalsForCurrency[it] ?: 0.0 }
            val salesPrev7 = input.prev7Dates.sumOf { dailyTotalsForCurrency[it] ?: 0.0 }

            val marginTodayValue = marginToday[currency]
            val marginLast7Value = marginLast7[currency]
            val marginTodayPercent = marginTodayValue?.takeIf { totalSalesToday > 0.0 }
                ?.let { (it / totalSalesToday) * 100.0 }
            val marginLast7Percent = marginLast7Value?.takeIf { salesLast7 > 0.0 }
                ?.let { (it / salesLast7) * 100.0 }

            val costedCount = costedToday[currency] ?: 0
            val totalItemsCount = totalItemsToday[currency] ?: 0
            val costCoveragePercent = if (totalItemsCount > 0) {
                (costedCount.toDouble() / totalItemsCount.toDouble()) * 100.0
            } else {
                null
            }

            CurrencyHomeMetric(
                currency = currency,
                totalSalesToday = totalSalesToday,
                invoicesToday = invoicesToday,
                avgTicket = avgTicket,
                customersToday = customersToday,
                outstandingTotal = outstanding[currency] ?: 0.0,
                salesYesterday = salesYesterday,
                salesLast7 = salesLast7,
                salesPrev7 = salesPrev7,
                compareVsYesterday = percentChange(totalSalesToday, salesYesterday),
                compareVsLastWeek = percentChange(salesLast7, salesPrev7),
                marginToday = marginTodayValue,
                marginTodayPercent = marginTodayPercent,
                marginLast7 = marginLast7Value,
                marginLast7Percent = marginLast7Percent,
                costCoveragePercent = costCoveragePercent,
                weekSeries = weekSeries
            )
        }
    }

    private fun aggregateSummariesByCurrency(
        summaries: List<CurrencySalesSummary>
    ): Map<String, CurrencySummaryAccumulator> {
        return summaries.fold(mutableMapOf()) { acc, row ->
            val key = normalizeCurrency(row.currency)
            val current = acc[key]
            if (current == null) {
                acc[key] = CurrencySummaryAccumulator(
                    total = row.total,
                    invoices = row.invoices,
                    customers = row.customers
                )
            } else {
                acc[key] = current.copy(
                    total = current.total + row.total,
                    invoices = current.invoices + row.invoices,
                    customers = current.customers + row.customers
                )
            }
            acc
        }
    }

    private fun aggregateDailyTotalsByCurrency(
        rows: List<CurrencyDailySalesTotal>
    ): Map<String, Map<String, Double>> {
        return rows.fold(mutableMapOf<String, MutableMap<String, Double>>()) { acc, row ->
            val currency = normalizeCurrency(row.currency)
            val byDate = acc.getOrPut(currency) { mutableMapOf() }
            byDate[row.date] = (byDate[row.date] ?: 0.0) + row.total
            acc
        }
    }

    private fun <T> aggregateDoubleByCurrency(
        rows: List<T>,
        valueSelector: (T) -> Double
    ): Map<String, Double> where T : Any {
        return rows.fold(mutableMapOf<String, Double>()) { acc, row ->
            val currency = when (row) {
                is CurrencyOutstandingTotal -> normalizeCurrency(row.currency)
                is CurrencyMarginTotal -> normalizeCurrency(row.currency)
                else -> return@fold acc
            }
            acc[currency] = (acc[currency] ?: 0.0) + valueSelector(row)
            acc
        }
    }

    private fun aggregateIntByCurrency(
        rows: List<CurrencyItemCount>,
        valueSelector: (CurrencyItemCount) -> Int
    ): Map<String, Int> {
        return rows.fold(mutableMapOf()) { acc, row ->
            val currency = normalizeCurrency(row.currency)
            acc[currency] = (acc[currency] ?: 0) + valueSelector(row)
            acc
        }
    }

    private fun percentChange(current: Double, previous: Double): Double? {
        if (previous == 0.0) return null
        return ((current - previous) / previous) * 100.0
    }

    private data class CurrencySummaryAccumulator(
        val total: Double,
        val invoices: Int,
        val customers: Int
    )
}
