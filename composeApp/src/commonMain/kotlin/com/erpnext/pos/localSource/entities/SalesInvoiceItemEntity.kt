package com.erpnext.pos.localSource.entities

import androidx.room.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "tabSalesInvoiceItem",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoiceEntity::class,
            parentColumns = ["invoice_name"],
            childColumns = ["parent_invoice"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parent_invoice"]),
        Index(value = ["item_code"])
    ]
)
data class SalesInvoiceItemEntity(

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    // 🔗 Referencia a la factura
    @ColumnInfo(name = "parent_invoice")
    var parentInvoice: String, // invoice_name del padre

    // 🧩 Identificación del ítem
    @ColumnInfo(name = "item_code")
    var itemCode: String,
    @ColumnInfo(name = "item_name")
    var itemName: String? = null,
    @ColumnInfo(name = "description")
    var description: String? = null,
    @ColumnInfo(name = "uom")
    var uom: String? = "Unit",
    @ColumnInfo(name = "qty")
    var qty: Double = 1.0,
    @ColumnInfo(name = "rate")
    var rate: Double = 0.0,
    @ColumnInfo(name = "amount")
    var amount: Double = 0.0,

    // 💰 Cuentas y precios
    @ColumnInfo(name = "price_list_rate")
    var priceListRate: Double = 0.0,
    @ColumnInfo(name = "discount_percentage")
    var discountPercentage: Double = 0.0,
    @ColumnInfo(name = "discount_amount")
    var discountAmount: Double = 0.0,
    @ColumnInfo(name = "net_rate")
    var netRate: Double = 0.0,
    @ColumnInfo(name = "net_amount")
    var netAmount: Double = 0.0,

    // 🧾 Impuestos individuales (si aplica)
    @ColumnInfo(name = "tax_rate")
    var taxRate: Double = 0.0,
    @ColumnInfo(name = "tax_amount")
    var taxAmount: Double = 0.0,

    // 📦 Almacén y trazabilidad
    @ColumnInfo(name = "warehouse")
    var warehouse: String? = null,
    @ColumnInfo(name = "batch_no")
    var batchNo: String? = null,
    @ColumnInfo(name = "serial_no")
    var serialNo: String? = null,
    @ColumnInfo("income_account")
    var incomeAccount: String? = null,
    @ColumnInfo("cost_center")
    var costCenter: String? = null,

    // ⚙️ Estado y auditoría
    @ColumnInfo(name = "is_return")
    var isReturn: Boolean = false,
    @ColumnInfo(name = "created_at")
    var createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "modified_at")
    var modifiedAt: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "last_synced_at")
    override var lastSyncedAt: Long? =  Clock.System.now().toEpochMilliseconds()
): SyncableEntity
