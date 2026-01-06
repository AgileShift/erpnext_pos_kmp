package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.api.v2.DocCreateResponse
import com.erpnext.pos.remoteSource.mapper.v2.toCreateDto
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.utils.RepoTrace

class SalesInvoiceRemoteRepository(
    private val api: APIServiceV2,
    private val customerRepository: CustomerRepository
) {
    suspend fun submitInvoice(
        invoice: SalesInvoiceWithItemsAndPayments
    ): Result<DocCreateResponse> {
        RepoTrace.breadcrumb("SalesInvoiceRemoteRepositoryV2", "submitInvoice")
        return runCatching {
            val remoteCustomerId = customerRepository.resolveRemoteCustomerId(
                invoice.invoice.instanceId,
                invoice.invoice.companyId,
                invoice.invoice.customerId
            )
            val payload = invoice.toCreateDto(remoteCustomerId)
            api.createDoc(ERPDocType.SalesInvoice, payload)
        }
    }// retorna invoice_name de ERPNext
}
