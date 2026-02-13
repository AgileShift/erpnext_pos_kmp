package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryReferenceCreateDto
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.toErpDate
import com.erpnext.pos.utils.toErpDateTime
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class PartialReturnInput(
    val invoiceName: String,
    val itemsToReturnByCode: Map<String, Double>,
    val reason: String? = null,
    val refundModeOfPayment: String? = null,
    val refundReferenceNo: String? = null,
    val applyRefund: Boolean = false
)

data class PartialReturnResult(
    val creditNoteName: String?,
    val refundCreated: Boolean = false
)

@OptIn(ExperimentalTime::class)
class PartialReturnUseCase(
    private val repository: SalesInvoiceRepository,
    private val paymentEntryUseCase: CreatePaymentEntryUseCase,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val posProfilePaymentMethodDao: PosProfilePaymentMethodDao,
    private val networkMonitor: NetworkMonitor
) : UseCase<PartialReturnInput, PartialReturnResult>() {
    override suspend fun useCaseFunction(input: PartialReturnInput): PartialReturnResult {
        var invoice = repository.getInvoiceByName(input.invoiceName)
            ?: throw IllegalArgumentException("Factura no encontrada: ${input.invoiceName}")
        if (invoice.items.isEmpty()) {
            repository.refreshInvoiceFromRemote(input.invoiceName)
            invoice = repository.getInvoiceByName(input.invoiceName)
                ?: throw IllegalArgumentException("Factura no encontrada: ${input.invoiceName}")
        }
        if (invoice.items.isEmpty()) {
            throw IllegalStateException("La factura no tiene items cargados para devolución.")
        }
        val originalOutstanding = invoice.invoice.outstandingAmount

        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        if (!isOnline) {
            throw IllegalStateException(
                "Se requiere conexión a internet para registrar un retorno parcial."
            )
        }

        val returnItems = buildReturnItems(invoice, input.itemsToReturnByCode)
        if (returnItems.isEmpty()) {
            throw IllegalArgumentException("Selecciona al menos un artículo para devolver.")
        }

        val creditNoteDto = buildCreditNoteDto(invoice, returnItems, input.reason)
        val created = repository.createRemoteInvoice(creditNoteDto)
        val entity = created.toEntity()
        repository.saveInvoiceLocally(
            invoice = entity.invoice,
            items = entity.items,
            payments = entity.payments
        )

        val returnTotal = kotlin.math.abs(created.grandTotal)
        invoice.invoice.invoiceName?.let { name ->
            repository.refreshInvoiceFromRemote(name)
            val refreshed = repository.getInvoiceByName(name)?.invoice
            val expectedOutstanding = (originalOutstanding - returnTotal).coerceAtLeast(0.0)
            if (refreshed != null && refreshed.outstandingAmount > expectedOutstanding + 0.01) {
                repository.applyLocalReturnAdjustment(name, returnTotal)
            }
        }

        val refundCreated = if (input.applyRefund) {
            input.refundModeOfPayment?.takeIf { it.isNotBlank() }?.let { mode ->
                createRefundPayment(
                    invoice = invoice,
                    creditNote = created,
                    refundModeOfPayment = mode,
                    refundReferenceNo = input.refundReferenceNo
                )
            } ?: false
        } else {
            false
        }

        return PartialReturnResult(
            creditNoteName = created.name,
            refundCreated = refundCreated
        )
    }

    private suspend fun buildReturnItems(
        invoice: SalesInvoiceWithItemsAndPayments,
        requested: Map<String, Double>
    ): List<SalesInvoiceItemDto> {
        val parent = invoice.invoice
        val remainingByItem = runCatching { buildRemainingByItem(invoice) }
            .getOrElse { emptyMap() }
        val requestedQty = requested.mapValues { it.value.coerceAtLeast(0.0) }
        return invoice.items.mapNotNull { item ->
            val remaining = remainingByItem[item.itemCode] ?: kotlin.math.abs(item.qty)
            if (remaining <= 0.0) return@mapNotNull null
            val desired = requestedQty[item.itemCode] ?: 0.0
            if (desired <= 0.0) return@mapNotNull null
            val qtyToReturn = desired.coerceAtMost(remaining)
            if (qtyToReturn <= 0.0) return@mapNotNull null
            createReturnItem(item, parent, qtyToReturn)
        }.filter { it.qty != 0.0 }
    }

    private suspend fun buildRemainingByItem(
        invoice: SalesInvoiceWithItemsAndPayments
    ): Map<String, Double> {
        val originalName = invoice.invoice.invoiceName ?: return emptyMap()
        val returnInvoices = repository.fetchRemoteReturnInvoices(
            returnAgainst = originalName
        )
        if (returnInvoices.isEmpty()) {
            return invoice.items.associate { item ->
                item.itemCode to kotlin.math.abs(item.qty).coerceAtLeast(0.0)
            }
        }
        val returnedByItem = mutableMapOf<String, Double>()
        returnInvoices.forEach { credit ->
            credit.items.forEach { item ->
                val qty = kotlin.math.abs(item.qty)
                if (qty <= 0.0) return@forEach
                returnedByItem[item.itemCode] = (returnedByItem[item.itemCode] ?: 0.0) + qty
            }
        }
        return invoice.items.associate { item ->
            val sold = kotlin.math.abs(item.qty).coerceAtLeast(0.0)
            val returned = returnedByItem[item.itemCode] ?: 0.0
            item.itemCode to (sold - returned).coerceAtLeast(0.0)
        }
    }

    private fun createReturnItem(
        entity: SalesInvoiceItemEntity,
        parent: SalesInvoiceEntity,
        qtyToReturn: Double
    ): SalesInvoiceItemDto {
        val dto = entity.toDto(parent)
        val perUnit =
            if (entity.qty != 0.0) entity.amount / entity.qty else dto.rate.takeIf { it != 0.0 }
                ?: 0.0
        val absAmount = perUnit * qtyToReturn
        return dto.copy(
            qty = -qtyToReturn,
            amount = -absAmount
        )
    }

    private fun buildCreditNoteDto(
        invoice: SalesInvoiceWithItemsAndPayments,
        items: List<SalesInvoiceItemDto>,
        reason: String?
    ): SalesInvoiceDto {
        val parent = invoice.invoice
        val now = Clock.System.now()
        val isPos = parent.isPos
        val totalAmount = items.sumOf { -it.amount }
        return SalesInvoiceDto(
            customer = parent.customer,
            customerName = parent.customerName ?: "",
            customerPhone = parent.customerPhone,
            company = parent.company,
            postingDate = now.toEpochMilliseconds().toErpDateTime(),
            dueDate = now.toEpochMilliseconds().toErpDate(),
            status = "Draft",
            grandTotal = totalAmount,
            outstandingAmount = totalAmount,
            totalTaxesAndCharges = 0.0,
            netTotal = totalAmount,
            paidAmount = 0.0,
            items = items,
            payments = emptyList(),
            remarks = reason ?: parent.remarks,
            updateStock = true,
            posProfile = parent.profileId,
            currency = parent.currency,
            conversionRate = parent.conversionRate,
            partyAccountCurrency = parent.partyAccountCurrency,
            returnAgainst = parent.invoiceName,
            posOpeningEntry = parent.posOpeningEntry,
            debitTo = parent.debitTo,
            docStatus = 0,
            isReturn = 1,
            isPos = isPos,
            doctype = "Sales Invoice"
        )
    }

    private suspend fun createRefundPayment(
        invoice: SalesInvoiceWithItemsAndPayments,
        creditNote: SalesInvoiceDto,
        refundModeOfPayment: String,
        refundReferenceNo: String?
    ): Boolean {
        val company = invoice.invoice.company
        val profileId = invoice.invoice.profileId
        val creditName = creditNote.name ?: return false
        val totalReturn = creditNote.grandTotal
        if (totalReturn <= 0.0) return false

        val refundModes = modeOfPaymentDao.getAllModes(company)
        if (!isRefundModeAllowed(profileId, refundModeOfPayment)) {
            throw IllegalStateException("El modo de reembolso no está habilitado para retornos.")
        }
        val referenceAmount = creditNote.outstandingAmount ?: totalReturn
        val postingDate = Clock.System.now().toEpochMilliseconds().toErpDateTime()

        val entry = PaymentEntryCreateDto(
            company = company,
            postingDate = postingDate,
            paymentType = "Pay",
            partyType = "Customer",
            partyId = invoice.invoice.customer,
            modeOfPayment = refundModeOfPayment,
            paidAmount = totalReturn,
            receivedAmount = totalReturn,
            paidFrom = resolveAccount(refundModeOfPayment, refundModes, invoice.invoice.debitTo),
            paidTo = resolveAccount(refundModeOfPayment, refundModes, invoice.invoice.debitTo),
            paidToAccountCurrency = invoice.invoice.currency,
            referenceNo = refundReferenceNo,
            referenceDate = postingDate,
            references = listOf(
                PaymentEntryReferenceCreateDto(
                    referenceDoctype = "Sales Invoice",
                    referenceName = creditName,
                    totalAmount = totalReturn,
                    outstandingAmount = referenceAmount,
                    allocatedAmount = totalReturn
                )
            ),
            docStatus = 0
        )

        paymentEntryUseCase(CreatePaymentEntryInput(entry))
        return true
    }

    private fun resolveAccount(
        mode: String?,
        cachedModes: List<ModeOfPaymentEntity>,
        fallback: String?
    ): String? {
        if (mode.isNullOrBlank()) return fallback
        return cachedModes.firstOrNull {
            it.modeOfPayment.equals(mode, ignoreCase = true) ||
                    it.name.equals(mode, ignoreCase = true)
        }?.account ?: fallback
    }

    private suspend fun isRefundModeAllowed(
        profileId: String?,
        mode: String?
    ): Boolean {
        if (profileId.isNullOrBlank() || mode.isNullOrBlank()) return false
        val resolved = posProfilePaymentMethodDao.getResolvedMethodsForProfile(profileId)
        val matched = resolved.firstOrNull { it.mopName.equals(mode, ignoreCase = true) }
        return matched?.allowInReturns == true && matched.enabledInProfile
    }
}
