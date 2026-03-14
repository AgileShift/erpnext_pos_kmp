package com.erpnext.pos.localSource.printing

import com.erpnext.pos.domain.printing.model.PrintJob
import com.erpnext.pos.domain.printing.model.PrintJobStatus
import com.erpnext.pos.domain.printing.model.PrinterCapabilities
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.localSource.entities.PrintJobEntity
import com.erpnext.pos.localSource.entities.PrinterProfileEntity

internal fun PrinterProfileEntity.toDomain(): PrinterProfile =
    PrinterProfile(
        id = id,
        name = name,
        brandHint = brandHint,
        modelHint = modelHint,
        family = PrinterFamily.valueOf(family),
        language = PrinterLanguage.valueOf(language),
        supportedTransports =
            supportedTransports.split(",").mapNotNull { value ->
              value.takeIf { it.isNotBlank() }?.let(TransportType::valueOf)
            }.toSet(),
        preferredTransport = preferredTransport?.let(TransportType::valueOf),
        host = host,
        port = port,
        bluetoothMacAddress = bluetoothMacAddress,
        bluetoothName = bluetoothName,
        capabilities =
            PrinterCapabilities(
                paperWidthMm = paperWidthMm,
                charactersPerLine = charactersPerLine,
                codePage = codePage,
                autoCut = autoCut,
                openDrawer = openDrawer,
            ),
        isDefault = isDefault,
        isEnabled = isEnabled,
        notes = notes,
    )

internal fun PrinterProfile.toEntity(): PrinterProfileEntity =
    PrinterProfileEntity(
        id = id,
        name = name,
        brandHint = brandHint,
        modelHint = modelHint,
        family = family.name,
        language = language.name,
        supportedTransports = supportedTransports.joinToString(",") { it.name },
        preferredTransport = preferredTransport?.name,
        host = host,
        port = port,
        bluetoothMacAddress = bluetoothMacAddress,
        bluetoothName = bluetoothName,
        paperWidthMm = paperWidthMm,
        charactersPerLine = charactersPerLine,
        codePage = codePage,
        autoCut = autoCut,
        openDrawer = openDrawer,
        isDefault = isDefault,
        isEnabled = isEnabled,
        notes = notes,
    )

internal fun PrintJobEntity.toDomain(): PrintJob =
    PrintJob(
        id = id,
        profileId = profileId,
        documentId = documentId,
        documentType = documentType,
        summary = summary,
        status = PrintJobStatus.valueOf(status),
        attempts = attempts,
        lastError = lastError,
        createdAtEpochMs = createdAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
    )

internal fun PrintJob.toEntity(): PrintJobEntity =
    PrintJobEntity(
        id = id,
        profileId = profileId,
        documentId = documentId,
        documentType = documentType,
        summary = summary,
        status = status.name,
        attempts = attempts,
        lastError = lastError,
        createdAtEpochMs = createdAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
    )
