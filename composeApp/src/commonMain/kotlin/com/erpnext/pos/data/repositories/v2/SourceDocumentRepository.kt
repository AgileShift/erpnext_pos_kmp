package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.models.SourceDocumentOption
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteListDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationListDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderListDto
import com.erpnext.pos.remoteSource.sdk.Filter
import com.erpnext.pos.remoteSource.sdk.Operator
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.views.salesflow.SalesFlowSource

class SourceDocumentRepository(
    private val api: APIServiceV2
) {
    suspend fun fetchDocumentsForCustomer(
        customerId: String,
        sourceType: SalesFlowSource
    ): List<SourceDocumentOption> {
        return when (sourceType) {
            SalesFlowSource.Quotation -> {
                val rows = api.list<QuotationListDto>(
                    doctype = ERPDocType.Quotation,
                    fields = listOf("name", "party_name", "customer_name", "transaction_date", "status"),
                    filters = listOf(Filter("party_name", Operator.EQ, customerId)),
                    orderBy = "transaction_date desc",
                    limit = 20
                )
                rows.map {
                    SourceDocumentOption(
                        id = it.name,
                        sourceType = sourceType,
                        customerId = it.customerId,
                        customerName = it.customerName,
                        date = it.transactionDate,
                        status = it.status
                    )
                }
            }

            SalesFlowSource.SalesOrder -> {
                val rows = api.list<SalesOrderListDto>(
                    doctype = ERPDocType.SalesOrder,
                    fields = listOf("name", "customer", "customer_name", "transaction_date", "status"),
                    filters = listOf(Filter("customer", Operator.EQ, customerId)),
                    orderBy = "transaction_date desc",
                    limit = 20
                )
                rows.map {
                    SourceDocumentOption(
                        id = it.name,
                        sourceType = sourceType,
                        customerId = it.customerId,
                        customerName = it.customerName,
                        date = it.transactionDate,
                        status = it.status
                    )
                }
            }

            SalesFlowSource.DeliveryNote -> {
                val rows = api.list<DeliveryNoteListDto>(
                    doctype = ERPDocType.DeliveryNote,
                    fields = listOf("name", "customer", "customer_name", "posting_date", "status"),
                    filters = listOf(Filter("customer", Operator.EQ, customerId)),
                    orderBy = "posting_date desc",
                    limit = 20
                )
                rows.map {
                    SourceDocumentOption(
                        id = it.name,
                        sourceType = sourceType,
                        customerId = it.customerId,
                        customerName = it.customerName,
                        date = it.postingDate,
                        status = it.status
                    )
                }
            }

            SalesFlowSource.Customer -> emptyList()
        }
    }
}
