package com.erpnext.pos.views.home

data class HomeMetrics(
    val totalSalesToday: Double = 0.0,
    val invoicesToday: Int = 0,
    val avgTicket: Double = 0.0,
    val customersToday: Int = 0,
    val outstandingTotal: Double = 0.0,
    val salesYesterday: Double = 0.0,
    val salesLast7: Double = 0.0,
    val salesPrev7: Double = 0.0,
    val compareVsYesterday: Double? = null,
    val compareVsLastWeek: Double? = null,
    val marginToday: Double? = null,
    val marginTodayPercent: Double? = null,
    val marginLast7: Double? = null,
    val marginLast7Percent: Double? = null,
    val costCoveragePercent: Double? = null,
    val topProducts: List<TopProductMetric> = emptyList(),
    val topProductsByMargin: List<TopProductMarginMetric> = emptyList(),
    val weekSeries: List<DailyMetric> = emptyList(),
    val currencyMetrics: List<CurrencyHomeMetric> = emptyList(),
    val inventoryAlerts: List<InventoryAlert> = emptyList()
)

data class DailyMetric(
    val date: String,
    val total: Double
)

data class CurrencyHomeMetric(
    val currency: String,
    val totalSalesToday: Double,
    val invoicesToday: Int,
    val avgTicket: Double,
    val customersToday: Int,
    val outstandingTotal: Double,
    val salesYesterday: Double,
    val salesLast7: Double,
    val salesPrev7: Double,
    val compareVsYesterday: Double?,
    val compareVsLastWeek: Double?,
    val marginToday: Double?,
    val marginTodayPercent: Double?,
    val marginLast7: Double?,
    val marginLast7Percent: Double?,
    val costCoveragePercent: Double?,
    val weekSeries: List<DailyMetric>
)

data class TopProductMetric(
    val itemCode: String,
    val itemName: String?,
    val qty: Double,
    val total: Double
)

data class TopProductMarginMetric(
    val itemCode: String,
    val itemName: String?,
    val qty: Double,
    val total: Double,
    val margin: Double,
    val marginPercent: Double?
)

data class InventoryAlert(
    val itemCode: String,
    val itemName: String,
    val qty: Double,
    val status: InventoryAlertStatus,
    val reorderLevel: Double?,
    val reorderQty: Double?
)

enum class InventoryAlertStatus {
    CRITICAL,
    LOW
}
