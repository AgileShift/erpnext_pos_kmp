@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.domain.usecases

import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.CurrencyDailySalesTotal
import com.erpnext.pos.views.home.DailyMetric
import com.erpnext.pos.views.home.CurrencyHomeMetric
import com.erpnext.pos.views.home.HomeMetrics
import com.erpnext.pos.views.home.TopProductMetric
import com.erpnext.pos.views.home.TopProductMarginMetric
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class HomeMetricInput(
    val days: Int = 7,
    val nowMillis: Long = Clock.System.now().toEpochMilliseconds()
)
class LoadHomeMetricsUseCase(
    private val salesInvoiceDao: SalesInvoiceDao
): UseCase<HomeMetricInput, HomeMetrics>() {

    override suspend fun useCaseFunction(input: HomeMetricInput): HomeMetrics {
        val tz = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(input.nowMillis).toLocalDateTime(tz).date
        val startDate = today.minus(DatePeriod(days = input.days - 1))
        val startDate14 = today.minus(DatePeriod(days = 13))
        val dateStrings = generateDateRange(startDate, today)

        val totalSalesToday = salesInvoiceDao.getTotalSalesForDate(today.toString()) ?: 0.0
        val invoicesToday = salesInvoiceDao.getSalesCountForDate(today.toString())
        val customersToday = salesInvoiceDao.getDistinctCustomersForDate(today.toString())
        val outstanding = salesInvoiceDao.getTotalOutstanding() ?: 0.0
        val avgTicket = if (invoicesToday > 0) totalSalesToday / invoicesToday else 0.0

        val rangeTotals14 = salesInvoiceDao.getDailySalesTotals(
            startDate = startDate14.toString(),
            endDate = today.toString()
        ).associateBy { it.date }

        val weekSeries = dateStrings.map { date ->
            DailyMetric(date = date, total = rangeTotals14[date]?.total ?: 0.0)
        }

        val yesterday = today.minus(DatePeriod(days = 1)).toString()
        val salesYesterday = rangeTotals14[yesterday]?.total ?: 0.0

        val last7Start = today.minus(DatePeriod(days = 6))
        val prev7Start = today.minus(DatePeriod(days = 13))
        val prev7End = today.minus(DatePeriod(days = 7))

        val last7Dates = generateDateRange(last7Start, today)
        val prev7Dates = generateDateRange(prev7Start, prev7End)

        val salesLast7 = last7Dates.sumOf { rangeTotals14[it]?.total ?: 0.0 }
        val salesPrev7 = prev7Dates.sumOf { rangeTotals14[it]?.total ?: 0.0 }

        val compareVsYesterday = percentChange(totalSalesToday, salesYesterday)
        val compareVsLastWeek = percentChange(salesLast7, salesPrev7)

        val currencySummaries = salesInvoiceDao.getSalesSummaryForDateByCurrency(today.toString())
        val outstandingByCurrency = salesInvoiceDao.getOutstandingTotalsByCurrency()
            .associateBy { it.currency }
        val dailyTotalsByCurrency = salesInvoiceDao.getDailySalesTotalsByCurrency(
            startDate = startDate14.toString(),
            endDate = today.toString()
        )
        val marginTodayByCurrency = salesInvoiceDao.getEstimatedMarginTotalByCurrency(
            startDate = today.toString(),
            endDate = today.toString()
        ).associateBy { it.currency }
        val marginLast7ByCurrency = salesInvoiceDao.getEstimatedMarginTotalByCurrency(
            startDate = last7Start.toString(),
            endDate = today.toString()
        ).associateBy { it.currency }
        val costedTodayByCurrency = salesInvoiceDao.countItemsWithCostByCurrency(
            startDate = today.toString(),
            endDate = today.toString()
        ).associateBy { it.currency }
        val totalItemsTodayByCurrency = salesInvoiceDao.countItemsInRangeByCurrency(
            startDate = today.toString(),
            endDate = today.toString()
        ).associateBy { it.currency }

        val availableCurrencies = salesInvoiceDao.getAvailableCurrencies()
        val currencies = buildSet {
            availableCurrencies.forEach { add(it) }
            currencySummaries.forEach { add(it.currency) }
            outstandingByCurrency.keys.forEach { add(it) }
            dailyTotalsByCurrency.forEach { add(it.currency) }
            marginTodayByCurrency.keys.forEach { add(it) }
            marginLast7ByCurrency.keys.forEach { add(it) }
        }.filter { it.isNotBlank() }.sorted()

        val currencyMetrics = currencies.map { currency ->
            val summary = currencySummaries.firstOrNull { it.currency == currency }
            val dailyTotalsForCurrency = dailyTotalsByCurrency
                .filter { it.currency == currency }
                .associateBy(CurrencyDailySalesTotal::date)

            val totalSalesTodayCurrency = summary?.total ?: 0.0
            val invoicesTodayCurrency = summary?.invoices ?: 0
            val customersTodayCurrency = summary?.customers ?: 0
            val avgTicketCurrency = if (invoicesTodayCurrency > 0) {
                totalSalesTodayCurrency / invoicesTodayCurrency
            } else {
                0.0
            }

            val weekSeriesCurrency = dateStrings.map { date ->
                DailyMetric(date = date, total = dailyTotalsForCurrency[date]?.total ?: 0.0)
            }

            val salesYesterdayCurrency = dailyTotalsForCurrency[yesterday]?.total ?: 0.0
            val salesLast7Currency = last7Dates.sumOf { dailyTotalsForCurrency[it]?.total ?: 0.0 }
            val salesPrev7Currency = prev7Dates.sumOf { dailyTotalsForCurrency[it]?.total ?: 0.0 }

            val marginTodayCurrency = marginTodayByCurrency[currency]?.margin
            val marginTodayPercentCurrency = marginTodayCurrency?.takeIf {
                totalSalesTodayCurrency > 0.0
            }?.let { (it / totalSalesTodayCurrency) * 100.0 }

            val marginLast7Currency = marginLast7ByCurrency[currency]?.margin
            val marginLast7PercentCurrency = marginLast7Currency?.takeIf {
                salesLast7Currency > 0.0
            }?.let { (it / salesLast7Currency) * 100.0 }

            val costedCount = costedTodayByCurrency[currency]?.count ?: 0
            val totalItemsCount = totalItemsTodayByCurrency[currency]?.count ?: 0
            val costCoveragePercentCurrency = if (totalItemsCount > 0) {
                (costedCount.toDouble() / totalItemsCount.toDouble()) * 100.0
            } else {
                null
            }

            CurrencyHomeMetric(
                currency = currency,
                totalSalesToday = totalSalesTodayCurrency,
                invoicesToday = invoicesTodayCurrency,
                avgTicket = avgTicketCurrency,
                customersToday = customersTodayCurrency,
                outstandingTotal = outstandingByCurrency[currency]?.total ?: 0.0,
                salesYesterday = salesYesterdayCurrency,
                salesLast7 = salesLast7Currency,
                salesPrev7 = salesPrev7Currency,
                compareVsYesterday = percentChange(totalSalesTodayCurrency, salesYesterdayCurrency),
                compareVsLastWeek = percentChange(salesLast7Currency, salesPrev7Currency),
                marginToday = marginTodayCurrency,
                marginTodayPercent = marginTodayPercentCurrency,
                marginLast7 = marginLast7Currency,
                marginLast7Percent = marginLast7PercentCurrency,
                costCoveragePercent = costCoveragePercentCurrency,
                weekSeries = weekSeriesCurrency
            )
        }

        val topProductsByMargin = salesInvoiceDao.getTopProductsByMargin(
            startDate = startDate.toString(),
            endDate = today.toString(),
            limit = 5
        ).map {
            val percent = if (it.total > 0.0) (it.margin / it.total) * 100.0 else null
            TopProductMarginMetric(
                itemCode = it.itemCode,
                itemName = it.itemName,
                qty = it.qty,
                total = it.total,
                margin = it.margin,
                marginPercent = percent
            )
        }

        val topProducts = salesInvoiceDao.getTopProductsBySales(
            startDate = startDate.toString(),
            endDate = today.toString(),
            limit = 5
        ).map {
            TopProductMetric(
                itemCode = it.itemCode,
                itemName = it.itemName,
                qty = it.qty,
                total = it.total
            )
        }

        val costedItemsToday = salesInvoiceDao.countItemsWithCost(
            startDate = today.toString(),
            endDate = today.toString()
        )
        val totalItemsToday = salesInvoiceDao.countItemsInRange(
            startDate = today.toString(),
            endDate = today.toString()
        )
        val costCoveragePercent = if (totalItemsToday > 0) {
            (costedItemsToday.toDouble() / totalItemsToday.toDouble()) * 100.0
        } else {
            null
        }
        val marginToday = if (costedItemsToday > 0) {
            salesInvoiceDao.getEstimatedMarginTotal(
                startDate = today.toString(),
                endDate = today.toString()
            ) ?: 0.0
        } else {
            null
        }
        val marginTodayPercent = marginToday?.takeIf { totalSalesToday > 0.0 }?.let {
            (it / totalSalesToday) * 100.0
        }

        val costedItemsLast7 = salesInvoiceDao.countItemsWithCost(
            startDate = last7Start.toString(),
            endDate = today.toString()
        )
        val marginLast7 = if (costedItemsLast7 > 0) {
            salesInvoiceDao.getEstimatedMarginTotal(
                startDate = last7Start.toString(),
                endDate = today.toString()
            ) ?: 0.0
        } else {
            null
        }
        val marginLast7Percent = marginLast7?.takeIf { salesLast7 > 0.0 }?.let {
            (it / salesLast7) * 100.0
        }

        return HomeMetrics(
            totalSalesToday = totalSalesToday,
            invoicesToday = invoicesToday,
            avgTicket = avgTicket,
            customersToday = customersToday,
            outstandingTotal = outstanding,
            salesYesterday = salesYesterday,
            salesLast7 = salesLast7,
            salesPrev7 = salesPrev7,
            compareVsYesterday = compareVsYesterday,
            compareVsLastWeek = compareVsLastWeek,
            marginToday = marginToday,
            marginTodayPercent = marginTodayPercent,
            marginLast7 = marginLast7,
            marginLast7Percent = marginLast7Percent,
            costCoveragePercent = costCoveragePercent,
            topProducts = topProducts,
            topProductsByMargin = topProductsByMargin,
            weekSeries = weekSeries,
            currencyMetrics = currencyMetrics
        )
    }

    private fun generateDateRange(start: LocalDate, end: LocalDate): List<String> {
        val dates = mutableListOf<String>()
        var current = start
        while (current <= end) {
            dates.add(current.toString())
            current = current.plus(DatePeriod(days = 1))
        }
        return dates
    }

    private fun percentChange(current: Double, previous: Double): Double? {
        if (previous == 0.0) return null
        return ((current - previous) / previous) * 100.0
    }
}
