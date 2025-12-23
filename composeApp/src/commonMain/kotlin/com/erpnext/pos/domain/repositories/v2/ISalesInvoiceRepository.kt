package com.erpnext.pos.domain.repositories.v2

import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments

interface ISalesInvoiceRepository {

    suspend fun insertInvoiceWithItemsAndPayments(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<SalesInvoicePaymentEntity>
    )

    suspend fun getPendingInvoices(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceWithItemsAndPayments>

    suspend fun getRelevantInvoices(
        instanceId: String,
        companyId: String,
        territoryId: String,
        fromDate: Long
    ): List<SalesInvoiceEntity>

    suspend fun countPending(instanceId: String, companyId: String): Int
    suspend fun countFailed(instanceId: String, companyId: String): Int
}