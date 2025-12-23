package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.remoteSource.dto.v2.InvoiceSnapshot

data class LoadInvoiceInput(
    val instanceId: String,
    val companyId: String,
    val territoryId: String,
    val fromDate: String // yyyy-MM-dd
)

class LoadInvoicesForRouteUseCase(
    private val salesInvoiceDao: SalesInvoiceDao
) : UseCase<LoadInvoiceInput, List<InvoiceSnapshot>>() {

    override suspend fun useCaseFunction(input: LoadInvoiceInput): List<InvoiceSnapshot> {
        return salesInvoiceDao
            .getInvoicesForRoute(input.instanceId, input.companyId, input.territoryId, input.fromDate)
            .map { invoiceWithDetails ->

                val paidAmount = invoiceWithDetails.payments.sumOf { it.amount }

                InvoiceSnapshot(
                    invoiceId = invoiceWithDetails.invoice.invoiceId,
                    customerId = invoiceWithDetails.invoice.customerName,
                    postingDate = invoiceWithDetails.invoice.postingDate,
                    dueDate = invoiceWithDetails.invoice.dueDate,
                    status = invoiceWithDetails.invoice.status,
                    grandTotal = invoiceWithDetails.invoice.grandTotal,
                    outstandingAmount = invoiceWithDetails.invoice.outstandingAmount,
                    payments = paidAmount,
                    docStatus = 0,
                    syncStatus = invoiceWithDetails.invoice.syncStatus ?: SyncStatus.PENDING
                )
            }
    }
}
