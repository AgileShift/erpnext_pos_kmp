package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "printer_profiles")
data class PrinterProfileEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "brand_hint") val brandHint: String?,
    @ColumnInfo(name = "model_hint") val modelHint: String?,
    @ColumnInfo(name = "family") val family: String,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "supported_transports") val supportedTransports: String,
    @ColumnInfo(name = "preferred_transport") val preferredTransport: String?,
    @ColumnInfo(name = "host") val host: String?,
    @ColumnInfo(name = "port") val port: Int?,
    @ColumnInfo(name = "bluetooth_mac_address") val bluetoothMacAddress: String?,
    @ColumnInfo(name = "bluetooth_name") val bluetoothName: String?,
    @ColumnInfo(name = "paper_width_mm") val paperWidthMm: Int,
    @ColumnInfo(name = "characters_per_line") val charactersPerLine: Int,
    @ColumnInfo(name = "code_page") val codePage: String,
    @ColumnInfo(name = "auto_cut") val autoCut: Boolean,
    @ColumnInfo(name = "open_drawer") val openDrawer: Boolean,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "notes") val notes: String?,
)
