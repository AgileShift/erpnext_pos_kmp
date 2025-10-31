package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "tabSalesInvoice",
    foreignKeys = [
        ForeignKey(
            entity = POSProfileEntity::class,
            parentColumns = ["profile_name"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["invoice_name"], unique = true),
        Index(value = ["profile_id"]),
        Index(value = ["customer"]),
        Index(value = ["status"]),
        Index(value = ["posting_date"])
    ]
)
data class SalesInvoiceEntity(

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    // 🔗 Relación con el perfil POS
    @ColumnInfo(name = "profile_id")
    var profileId: String? = null,

    // 🧾 Identificación contable
    @ColumnInfo(name = "invoice_name")
    var invoiceName: String? = null, // Nombre remoto (ej. SINV-00045)
    @ColumnInfo(name = "reference_no")
    var referenceNo: String? = null, // Ej. número de recibo o comprobante externo

    // 👤 Cliente y compañía
    var customer: String,
    @ColumnInfo(name = "customer_name")
    var customerName: String? = null,
    var company: String,
    var branch: String? = null,
    var warehouse: String? = null,
    var currency: String = "USD",

    // 📅 Fechas clave
    @ColumnInfo(name = "posting_date")
    var postingDate: String, // Fecha de emisión
    @ColumnInfo(name = "due_date")
    var dueDate: String? = null, // Fecha límite de pago
    @ColumnInfo(name = "posting_time")
    var postingTime: String? = null, // Hora precisa (opcional)

    // 💰 Montos y cuentas
    @ColumnInfo(name = "net_total")
    var netTotal: Double = 0.0, // Subtotal sin impuestos
    @ColumnInfo(name = "tax_total")
    var taxTotal: Double = 0.0, // Total de impuestos
    @ColumnInfo(name = "discount_amount")
    var discountAmount: Double = 0.0,
    @ColumnInfo(name = "grand_total")
    var grandTotal: Double = 0.0, // Total final con impuestos
    @ColumnInfo(name = "paid_amount")
    var paidAmount: Double = 0.0,
    @ColumnInfo(name = "outstanding_amount")
    var outstandingAmount: Double = 0.0, // Saldo pendiente

    // 📦 Contabilidad
    @ColumnInfo(name = "income_account")
    var incomeAccount: String? = null,
    @ColumnInfo(name = "expense_account")
    var expenseAccount: String? = null,
    @ColumnInfo(name = "cost_center")
    var costCenter: String? = null,
    @ColumnInfo(name = "price_list")
    var priceList: String? = null,

    // ⚙️ Estado y sincronización
    var status: String = "Draft", // Draft, Submitted, Paid, Cancelled
    @ColumnInfo(name = "sync_status")
    var syncStatus: String = "Pending", // Pending, Synced, Failed
    @ColumnInfo(name = "docstatus")
    var docstatus: Int = 0, // 0 = borrador, 1 = enviado, 2 = cancelado
    @ColumnInfo(name = "is_return")
    var isReturn: Boolean = false, // Nota de crédito o devolución

    // 💳 Método de pago (último o principal)
    @ColumnInfo(name = "mode_of_payment")
    var modeOfPayment: String? = null,

    // 🧠 Auditoría y metadatos
    @ColumnInfo(name = "created_by")
    var createdBy: String? = null,
    @ColumnInfo(name = "modified_by")
    var modifiedBy: String? = null,
    @ColumnInfo(name = "created_at")
    var createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "modified_at")
    var modifiedAt: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "remarks")
    var remarks: String? = null
)


data class SalesInvoiceWithItemsAndPayments(

    @Embedded
    var invoice: SalesInvoiceEntity,

    @Relation(
        parentColumn = "invoice_name",
        entityColumn = "parent_invoice"
    )
    var items: List<SalesInvoiceItemEntity> = emptyList(),

    @Relation(
        parentColumn = "invoice_name",
        entityColumn = "parent_invoice"
    )
    var payments: List<POSInvoicePaymentEntity> = emptyList()
)