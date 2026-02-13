package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.data.mappers.buildClosingEntryDto
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import com.erpnext.pos.utils.buildPaymentReconciliationSeeds
import com.erpnext.pos.utils.normalizeCurrency

class ClosingEntrySyncRepository(
    private val api: APIService,
    private val cashboxDao: CashboxDao,
    private val openingEntryLinkDao: POSOpeningEntryLinkDao,
    private val openingEntryDao: POSOpeningEntryDao,
    private val closingDao: POSClosingEntryDao,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val posProfileDao: POSProfileDao,
    private val paymentMethodLocalRepository: PosProfilePaymentMethodLocalRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend fun reconcileRemoteClosingsForActiveCashboxes(): Boolean {
        val active = cashboxDao.getActiveCashboxes()
        if (active.isEmpty()) return false
        var hasChanges = false

        active.forEach { wrapper ->
            val cashbox = wrapper.cashbox
            val remoteOpeningName = openingEntryLinkDao.getRemoteOpeningEntryName(cashbox.localId)
                ?: cashbox.openingEntryId?.takeIf {
                    it.isNotBlank() && !it.startsWith("LOCAL-", ignoreCase = true)
                }
            if (remoteOpeningName.isNullOrBlank()) return@forEach

            val remoteClosing = runCatching {
                api.getPOSClosingEntriesForOpening(remoteOpeningName).firstOrNull()
            }.onFailure {
                AppLogger.warn(
                    "ClosingEntrySyncRepository: remote closing lookup failed for opening $remoteOpeningName",
                    it
                )
            }.getOrNull() ?: return@forEach

            if (remoteClosing.docstatus != null && remoteClosing.docstatus != 1) {
                return@forEach
            }

            val resolvedEndDate =
                remoteClosing.periodEndDate
                    ?: remoteClosing.postingDate
                    ?: cashbox.periodEndDate
                    ?: cashbox.periodStartDate

            ensureRemoteOpeningEntry(remoteOpeningName, cashbox.localId)

            val startMillis = parseErpDateTimeToEpochMillis(cashbox.periodStartDate)
                ?: return@forEach
            val endMillis = parseErpDateTimeToEpochMillis(resolvedEndDate) ?: return@forEach
            val shiftInvoices = buildList {
                val openingId = cashbox.openingEntryId?.takeIf { it.isNotBlank() }
                if (!openingId.isNullOrBlank()) {
                    addAll(salesInvoiceDao.getInvoicesForOpeningEntry(openingId))
                }
                if (isEmpty()) {
                    val localOpening = openingEntryLinkDao.getByCashboxId(cashbox.localId)
                        ?.localOpeningEntryName
                    if (!localOpening.isNullOrBlank() && localOpening != openingId) {
                        addAll(salesInvoiceDao.getInvoicesForOpeningEntry(localOpening))
                    }
                }
                if (isEmpty()) {
                    addAll(
                        salesInvoiceDao.getInvoicesForShift(
                            profileId = cashbox.posProfile,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                    )
                }
            }
            shiftInvoices.forEach { invoice ->
                val name = invoice.invoiceName?.trim().orEmpty()
                if (name.isBlank() || name.startsWith("LOCAL-", ignoreCase = true)) return@forEach
                if (invoice.posOpeningEntry != remoteOpeningName ||
                    invoice.profileId != cashbox.posProfile
                ) {
                    salesInvoiceDao.updateInvoiceOpeningAndProfile(
                        invoiceName = name,
                        posOpeningEntry = remoteOpeningName,
                        profileId = cashbox.posProfile
                    )
                    salesInvoiceDao.updatePaymentsOpeningForInvoice(name, remoteOpeningName)
                }
            }
            val paymentRows = buildList {
                val openingId = cashbox.openingEntryId?.takeIf { it.isNotBlank() }
                if (!openingId.isNullOrBlank()) {
                    addAll(salesInvoiceDao.getPaymentsForOpeningEntry(openingId))
                }
                if (isEmpty()) {
                    val localOpening = openingEntryLinkDao.getByCashboxId(cashbox.localId)
                        ?.localOpeningEntryName
                    if (!localOpening.isNullOrBlank() && localOpening != openingId) {
                        addAll(salesInvoiceDao.getPaymentsForOpeningEntry(localOpening))
                    }
                }
                if (isEmpty()) {
                    addAll(
                        salesInvoiceDao.getShiftPayments(
                            profileId = cashbox.posProfile,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                    )
                }
            }
            val profileCurrency = posProfileDao.getPOSProfile(cashbox.posProfile).currency
            val modeCurrency = runCatching {
                paymentMethodLocalRepository.getMethodsForProfile(cashbox.posProfile)
            }.getOrElse { emptyList() }
                .associate { method ->
                    method.mopName to normalizeCurrency(method.currency ?: profileCurrency)
                }
            val paymentReconciliation = buildPaymentReconciliationSeeds(
                balanceDetails = wrapper.details,
                paymentRows = paymentRows,
                invoices = shiftInvoices,
                modeCurrency = modeCurrency,
                posCurrency = normalizeCurrency(profileCurrency),
                rateResolver = { from, to -> exchangeRateRepository.getRate(from, to) }
            )

            val dto = buildClosingEntryDto(
                cashbox = cashbox,
                openingEntryId = remoteOpeningName,
                postingDate = resolvedEndDate,
                periodEndDate = resolvedEndDate,
                paymentReconciliation = paymentReconciliation,
                invoices = shiftInvoices
            )

            cashboxDao.updateStatus(
                cashbox.localId,
                status = false,
                pceId = remoteClosing.name,
                endDate = resolvedEndDate,
                pendingSync = false
            )
            closingDao.insert(dto.toEntity(remoteClosing.name, pendingSync = false))
            cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, remoteClosing.name)

            if (cashbox.closingEntryId?.startsWith("LOCAL-", ignoreCase = true) == true) {
                closingDao.delete(cashbox.closingEntryId!!)
            }

            if (cashbox.openingEntryId != remoteOpeningName) {
                cashboxDao.updateOpeningEntryId(cashbox.localId, remoteOpeningName)
            }

            hasChanges = true
        }

        return hasChanges
    }

    suspend fun pushPending(): Boolean {
        val pending = cashboxDao.getClosedPendingSync()
        if (pending.isEmpty()) return false
        var hasChanges = false

        pending.forEach { wrapper ->
            val cashbox = wrapper.cashbox
            val localClosingName = cashbox.closingEntryId
            val remoteOpeningName = openingEntryLinkDao.getRemoteOpeningEntryName(cashbox.localId)
                ?: cashbox.openingEntryId?.takeIf {
                    it.isNotBlank() && !it.startsWith("LOCAL-", ignoreCase = true)
                }
            if (remoteOpeningName.isNullOrBlank()) {
                AppLogger.warn(
                    "ClosingEntrySyncRepository: missing remote opening for cashbox ${cashbox.localId}"
                )
                return@forEach
            }
            val periodEnd = cashbox.periodEndDate
            if (periodEnd.isNullOrBlank()) return@forEach
            val startMillis = parseErpDateTimeToEpochMillis(cashbox.periodStartDate) ?: return@forEach
            val endMillis = parseErpDateTimeToEpochMillis(periodEnd) ?: return@forEach
            val remoteClosing = runCatching {
                api.getPOSClosingEntriesForOpening(remoteOpeningName).firstOrNull()
            }.onFailure {
                AppLogger.warn(
                    "ClosingEntrySyncRepository: lookup remote closing failed for opening $remoteOpeningName",
                    it
                )
            }.getOrNull()
            val shiftInvoices = buildList {
                val openingId = cashbox.openingEntryId?.takeIf { it.isNotBlank() }
                if (!openingId.isNullOrBlank()) {
                    addAll(salesInvoiceDao.getInvoicesForOpeningEntry(openingId))
                }
                if (isEmpty()) {
                    val localOpening = openingEntryLinkDao.getByCashboxId(cashbox.localId)
                        ?.localOpeningEntryName
                    if (!localOpening.isNullOrBlank() && localOpening != openingId) {
                        addAll(salesInvoiceDao.getInvoicesForOpeningEntry(localOpening))
                    }
                }
                if (isEmpty()) {
                    addAll(
                        salesInvoiceDao.getInvoicesForShift(
                            profileId = cashbox.posProfile,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                    )
                }
            }
            shiftInvoices.forEach { invoice ->
                val name = invoice.invoiceName?.trim().orEmpty()
                if (name.isBlank() || name.startsWith("LOCAL-", ignoreCase = true)) return@forEach
                if (invoice.posOpeningEntry != remoteOpeningName ||
                    invoice.profileId != cashbox.posProfile
                ) {
                    salesInvoiceDao.updateInvoiceOpeningAndProfile(
                        invoiceName = name,
                        posOpeningEntry = remoteOpeningName,
                        profileId = cashbox.posProfile
                    )
                    salesInvoiceDao.updatePaymentsOpeningForInvoice(name, remoteOpeningName)
                }
            }
            val paymentRows = buildList {
                val openingId = cashbox.openingEntryId?.takeIf { it.isNotBlank() }
                if (!openingId.isNullOrBlank()) {
                    addAll(salesInvoiceDao.getPaymentsForOpeningEntry(openingId))
                }
                if (isEmpty()) {
                    val localOpening = openingEntryLinkDao.getByCashboxId(cashbox.localId)
                        ?.localOpeningEntryName
                    if (!localOpening.isNullOrBlank() && localOpening != openingId) {
                        addAll(salesInvoiceDao.getPaymentsForOpeningEntry(localOpening))
                    }
                }
                if (isEmpty()) {
                    addAll(
                        salesInvoiceDao.getShiftPayments(
                            profileId = cashbox.posProfile,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                    )
                }
            }
            val profileCurrency = posProfileDao.getPOSProfile(cashbox.posProfile).currency
            val modeCurrency = runCatching {
                paymentMethodLocalRepository.getMethodsForProfile(cashbox.posProfile)
            }.getOrElse { emptyList() }
                .associate { method ->
                    method.mopName to normalizeCurrency(method.currency ?: profileCurrency)
                }
            val paymentReconciliation = buildPaymentReconciliationSeeds(
                balanceDetails = wrapper.details,
                paymentRows = paymentRows,
                invoices = shiftInvoices,
                modeCurrency = modeCurrency,
                posCurrency = normalizeCurrency(profileCurrency),
                rateResolver = { from, to -> exchangeRateRepository.getRate(from, to) }
            )
            ensureRemoteOpeningEntry(remoteOpeningName, cashbox.localId)
            val dto = buildClosingEntryDto(
                cashbox = cashbox,
                openingEntryId = remoteOpeningName,
                postingDate = periodEnd,
                periodEndDate = periodEnd,
                paymentReconciliation = paymentReconciliation,
                invoices = shiftInvoices
            )

            if (remoteClosing != null) {
                val pendingSync = remoteClosing.docstatus != 1
                if (pendingSync) {
                    AppLogger.warn(
                        "ClosingEntrySyncRepository: remote closing ${remoteClosing.name} en borrador; submit legacy removido."
                    )
                }
                val resolvedEndDate = remoteClosing.periodEndDate ?: periodEnd
                cashboxDao.updateStatus(
                    cashbox.localId,
                    status = false,
                    pceId = remoteClosing.name,
                    endDate = resolvedEndDate,
                    pendingSync = pendingSync
                )
                closingDao.insert(dto.toEntity(remoteClosing.name, pendingSync = pendingSync))
                cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, remoteClosing.name)
                if (localClosingName?.startsWith("LOCAL-", ignoreCase = true) == true) {
                    closingDao.delete(localClosingName)
                }
                hasChanges = true
                return@forEach
            }

            val existingRemote = localClosingName
                ?.takeIf { it.isNotBlank() && !it.startsWith("LOCAL-", ignoreCase = true) }
            val response = if (existingRemote == null) {
                runCatching { api.closeCashbox(dto) }
                    .onFailure { AppLogger.warn("ClosingEntrySyncRepository create failed", it) }
                    .getOrNull()
            } else {
                com.erpnext.pos.remoteSource.dto.POSClosingEntryResponse(existingRemote)
            }
            if (response == null) return@forEach
            val pendingSync = existingRemote != null
            if (pendingSync) {
                AppLogger.warn(
                    "ClosingEntrySyncRepository: closing ${response.name} requiere revision manual; submit legacy removido."
                )
            }
            cashboxDao.updateStatus(
                cashbox.localId,
                status = false,
                pceId = response.name,
                endDate = periodEnd,
                pendingSync = pendingSync
            )
            closingDao.insert(dto.toEntity(response.name, pendingSync = pendingSync))
            cashboxDao.updateBalanceDetailsClosingEntry(cashbox.localId, response.name)
            if (!pendingSync && localClosingName?.startsWith("LOCAL-", ignoreCase = true) == true) {
                closingDao.delete(localClosingName)
            }
            hasChanges = true
        }

        return hasChanges
    }

    private suspend fun ensureRemoteOpeningEntry(remoteName: String, cashboxId: Long) {
        val existing = openingEntryDao.getByName(remoteName)
        if (existing != null) return
        val link = openingEntryLinkDao.getByCashboxId(cashboxId) ?: return
        val local = openingEntryDao.getByName(link.localOpeningEntryName) ?: return
        openingEntryDao.insert(local.copy(name = remoteName, pendingSync = false))
    }
}
