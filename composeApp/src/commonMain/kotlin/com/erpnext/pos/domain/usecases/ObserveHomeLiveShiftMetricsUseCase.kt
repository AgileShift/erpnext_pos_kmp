package com.erpnext.pos.domain.usecases

import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ObserveHomeLiveShiftMetricsInput(
    val postingDate: String,
    val openingEntryId: String
)

data class HomeLiveShiftCurrencyMetric(
    val currency: String,
    val totalSalesToday: Double,
    val invoicesToday: Int,
    val customersToday: Int,
    val avgTicket: Double
)

data class HomeLiveShiftMetrics(
    val totalSalesToday: Double,
    val invoicesToday: Int,
    val customersToday: Int,
    val avgTicket: Double,
    val byCurrency: List<HomeLiveShiftCurrencyMetric>
)

class ObserveHomeLiveShiftMetricsUseCase(
    private val salesInvoiceDao: SalesInvoiceDao
) {
    fun observe(input: ObserveHomeLiveShiftMetricsInput): Flow<HomeLiveShiftMetrics> {
        val summaryFlow = salesInvoiceDao.observeShiftTodaySummary(
            date = input.postingDate,
            openingEntryId = input.openingEntryId
        )
        val byCurrencyFlow = salesInvoiceDao.observeShiftTodaySummaryByCurrency(
            date = input.postingDate,
            openingEntryId = input.openingEntryId
        )
        return combine(summaryFlow, byCurrencyFlow) { summary, currencyRows ->
            HomeLiveShiftMetrics(
                totalSalesToday = summary.totalSalesToday,
                invoicesToday = summary.invoicesToday,
                customersToday = summary.customersToday,
                avgTicket = if (summary.invoicesToday > 0) {
                    summary.totalSalesToday / summary.invoicesToday
                } else {
                    0.0
                },
                byCurrency = currencyRows.map { row ->
                    HomeLiveShiftCurrencyMetric(
                        currency = row.currency,
                        totalSalesToday = row.totalSalesToday,
                        invoicesToday = row.invoicesToday,
                        customersToday = row.customersToday,
                        avgTicket = if (row.invoicesToday > 0) {
                            row.totalSalesToday / row.invoicesToday
                        } else {
                            0.0
                        }
                    )
                }
            )
        }
    }
}
