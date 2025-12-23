package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.domain.policy.DatePolicy
import com.erpnext.pos.performance.SnapshotMetric
import com.erpnext.pos.remoteSource.dto.v2.POSOperationalSnapshot
import com.erpnext.pos.remoteSource.dto.v2.POSConfigurationSnapshot
import io.ktor.client.request.forms.formData

class BuildPosOperationalSnapshotUseCase(
    private val loadContext: LoadPosContextUseCase,
    private val loadCatalog: LoadCatalogUseCase,
    private val loadCustomers: LoadCustomersForRouteUseCase,
    private val loadInvoices: LoadInvoicesForRouteUseCase,
    private val getSyncStatus: GetSyncStatusUseCase,
    private val metrics: SnapshotMetric,
    private val datePolicy: DatePolicy
) {

    suspend operator fun invoke(
        params: BuildPOSOperationalSnapshotInput
    ): POSOperationalSnapshot {

        val context = loadContext(
            params.instanceId,
            params.companyId,
            params.userId,
            params.posProfileId
        )
        metrics.mark("Context")

        val input = CatalogInput(
            params.instanceId,
            params.companyId,
            context.posProfile.profile.priceList,
            context.posProfile.profile.warehouseId
        )
        val catalog = loadCatalog.invoke(input)
        metrics.mark("Catalog")

        val loadCustomersForRouteInput = LoadCustomersForRouteInput(
            params.instanceId,
            params.companyId,
            context.territory.territoryId
        )
        val customers = loadCustomers.invoke(loadCustomersForRouteInput)
        metrics.mark("Customers")

        val fromDate = datePolicy.invoicesFromDate()
        val invoices = loadInvoices.invoke(
            LoadInvoiceInput(
                params.instanceId,
                params.companyId,
                context.territory.territoryId,
                fromDate
            )
        )
        metrics.mark("Invoices")

        val sync =
            getSyncStatus(SyncStatusInput(params.instanceId, params.companyId))
        metrics.mark("Sync")

        metrics.finish()

        return POSOperationalSnapshot(
            context = context,
            configuration = POSConfigurationSnapshot(
                currency = context.posProfile.profile.currency,
                priceList = context.posProfile.profile.priceList,
                warehouseId = context.posProfile.profile.warehouseId,
                paymentMethods = context.posProfile.paymentMethods,
                taxTemplateId = context.posProfile.profile.taxTemplateId
            ),
            catalog = catalog,
            customers = customers,
            invoices = invoices,
            sync = sync
        )
    }
}
