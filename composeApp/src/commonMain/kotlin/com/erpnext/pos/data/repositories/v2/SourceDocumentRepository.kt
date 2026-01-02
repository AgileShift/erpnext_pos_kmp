package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.models.SourceDocumentItem
import com.erpnext.pos.domain.models.SourceDocumentOption
import com.erpnext.pos.domain.models.SourceDocumentTax
import com.erpnext.pos.domain.models.SourceDocumentTotals
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteListDto
import com.erpnext.pos.remoteSource.dto.v2.DeliveryNoteDetailDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationListDto
import com.erpnext.pos.remoteSource.dto.v2.QuotationDetailDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderListDto
import com.erpnext.pos.remoteSource.dto.v2.SalesOrderDetailDto
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
                    fields = listOf(
                        "name",
                        "party_name",
                        "customer_name",
                        "transaction_date",
                        "status",
                        "net_total",
                        "grand_total",
                        "total_taxes_and_charges",
                        "price_list_currency",
                        "currency"
                    ),
                    filters = listOf(Filter("party_name", Operator.EQ, customerId)),
                    orderBy = "transaction_date desc",
                    limit = 20
                )
                val details = fetchQuotationDetails(rows.map { it.name })
                val detailsById = details.associateBy { it.quotationId }
                rows.map {
                    val detail = detailsById[it.name]
                    val taxes = detail?.taxes?.map { tax ->
                        SourceDocumentTax(
                            chargeType = tax.chargeType,
                            accountHead = tax.accountHead,
                            rate = tax.rate,
                            taxAmount = tax.taxAmount
                        )
                    }.orEmpty()
                    SourceDocumentOption(
                        id = it.name,
                        sourceType = sourceType,
                        customerId = it.customerId,
                        customerName = it.customerName,
                        date = it.transactionDate,
                        status = it.status,
                        items = detail?.items?.map { item ->
                            SourceDocumentItem(
                                itemCode = item.itemCode,
                                itemName = item.itemName,
                                qty = item.qty,
                                uom = item.uom,
                                rate = item.rate,
                                amount = item.amount,
                                warehouse = item.warehouse
                            )
                        }.orEmpty(),
                        taxes = taxes,
                        totals = SourceDocumentTotals(
                            netTotal = it.netTotal,
                            grandTotal = it.grandTotal,
                            taxTotal = it.totalTaxesAndCharges ?: taxes.sumOf { tax -> tax.taxAmount },
                            currency = it.priceListCurrency ?: it.currency
                        )
                    )
                }
            }

            SalesFlowSource.SalesOrder -> {
                val rows = api.list<SalesOrderListDto>(
                    doctype = ERPDocType.SalesOrder,
                    fields = listOf(
                        "name",
                        "customer",
                        "customer_name",
                        "transaction_date",
                        "status",
                        "net_total",
                        "grand_total",
                        "total_taxes_and_charges",
                        "price_list_currency",
                        "currency"
                    ),
                    filters = listOf(Filter("customer", Operator.EQ, customerId)),
                    orderBy = "transaction_date desc",
                    limit = 20
                )
                val details = fetchSalesOrderDetails(rows.map { it.name })
                val detailsById = details.associateBy { it.salesOrderId }
                rows.map {
                    val detail = detailsById[it.name]
                    val taxes = detail?.taxes?.map { tax ->
                        SourceDocumentTax(
                            chargeType = tax.chargeType,
                            accountHead = tax.accountHead,
                            rate = tax.rate,
                            taxAmount = tax.taxAmount
                        )
                    }.orEmpty()
                    SourceDocumentOption(
                        id = it.name,
                        sourceType = sourceType,
                        customerId = it.customerId,
                        customerName = it.customerName,
                        date = it.transactionDate,
                        status = it.status,
                        items = detail?.items?.map { item ->
                            SourceDocumentItem(
                                itemCode = item.itemCode,
                                itemName = item.itemName,
                                qty = item.qty,
                                uom = item.uom,
                                rate = item.rate,
                                amount = item.amount,
                                warehouse = item.warehouse
                            )
                        }.orEmpty(),
                        taxes = taxes,
                        totals = SourceDocumentTotals(
                            netTotal = it.netTotal,
                            grandTotal = it.grandTotal,
                            taxTotal = it.totalTaxesAndCharges ?: taxes.sumOf { tax -> tax.taxAmount },
                            currency = it.priceListCurrency ?: it.currency
                        )
                    )
                }
            }

            SalesFlowSource.DeliveryNote -> {
                val rows = api.list<DeliveryNoteListDto>(
                    doctype = ERPDocType.DeliveryNote,
                    fields = listOf(
                        "name",
                        "customer",
                        "customer_name",
                        "posting_date",
                        "status",
                        "net_total",
                        "grand_total",
                        "total_taxes_and_charges",
                        "currency"
                    ),
                    filters = listOf(Filter("customer", Operator.EQ, customerId)),
                    orderBy = "posting_date desc",
                    limit = 20
                )
                val details = fetchDeliveryNoteDetails(rows.map { it.name })
                val detailsById = details.associateBy { it.deliveryNoteId }
                rows.map {
                    val detail = detailsById[it.name]
                    val taxes = detail?.taxes?.map { tax ->
                        SourceDocumentTax(
                            chargeType = tax.chargeType,
                            accountHead = tax.accountHead,
                            rate = tax.rate,
                            taxAmount = tax.taxAmount
                        )
                    }.orEmpty()
                    SourceDocumentOption(
                        id = it.name,
                        sourceType = sourceType,
                        customerId = it.customerId,
                        customerName = it.customerName,
                        date = it.postingDate,
                        status = it.status,
                        items = detail?.items?.map { item ->
                            SourceDocumentItem(
                                itemCode = item.itemCode,
                                itemName = item.itemName,
                                qty = item.qty,
                                uom = item.uom,
                                rate = item.rate,
                                amount = item.amount,
                                warehouse = item.warehouse
                            )
                        }.orEmpty(),
                        taxes = taxes,
                        totals = SourceDocumentTotals(
                            netTotal = it.netTotal,
                            grandTotal = it.grandTotal,
                            taxTotal = it.totalTaxesAndCharges ?: taxes.sumOf { tax -> tax.taxAmount },
                            currency = it.currency
                        )
                    )
                }
            }

            SalesFlowSource.Customer -> emptyList()
        }
    }

    private suspend fun fetchQuotationDetails(ids: List<String>): List<QuotationDetailDto> {
        if (ids.isEmpty()) return emptyList()
        return api.getDocsInBatches(ERPDocType.Quotation, ids)
    }

    private suspend fun fetchSalesOrderDetails(ids: List<String>): List<SalesOrderDetailDto> {
        if (ids.isEmpty()) return emptyList()
        return api.getDocsInBatches(ERPDocType.SalesOrder, ids)
    }

    private suspend fun fetchDeliveryNoteDetails(ids: List<String>): List<DeliveryNoteDetailDto> {
        if (ids.isEmpty()) return emptyList()
        return api.getDocsInBatches(ERPDocType.DeliveryNote, ids)
    }
}
