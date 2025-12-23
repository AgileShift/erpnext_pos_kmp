package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments

class SalesInvoiceRemoteRepository {
    suspend fun submitInvoice(
        invoice: SalesInvoiceWithItemsAndPayments
    ): Result<String> {
        TODO("Not yet implemented")
    }// retorna invoice_name de ERPNext
}