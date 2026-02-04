package com.erpnext.pos.utils.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.erpnext.pos.domain.usecases.InventoryAlertInput
import com.erpnext.pos.domain.usecases.LoadInventoryAlertsUseCase
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.sync.SyncContextProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class InventoryAlertsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val syncContextProvider: SyncContextProvider by inject()
    private val loadInventoryAlertsUseCase: LoadInventoryAlertsUseCase by inject()
    private val generalPreferences: GeneralPreferences by inject()

    override suspend fun doWork(): Result {
        val ctx = syncContextProvider.buildContext() ?: return Result.success()
        if (ctx.warehouseId.isBlank()) return Result.success()
        if (!generalPreferences.getInventoryAlertsEnabled()) return Result.success()

        val alerts = loadInventoryAlertsUseCase(
            InventoryAlertInput(
                instanceId = ctx.instanceId,
                companyId = ctx.companyId,
                warehouseId = ctx.warehouseId,
                limit = 20
            )
        )
        if (alerts.isEmpty()) return Result.success()

        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val lastDate = generalPreferences.getInventoryAlertDate()
        if (lastDate != today) {
            generalPreferences.setInventoryAlertDate(today)
            val message = "Alertas de inventario: ${alerts.size} (${ctx.warehouseId})"
            notifySystem("Inventory Alerts", message)
        }
        return Result.success()
    }
}
