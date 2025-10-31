package com.erpnext.pos.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.CategoryDao
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.dao.ItemDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PaymentModesDao
import com.erpnext.pos.localSource.dao.PendingInvoiceDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.CategoryEntity
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.ItemEntity
import com.erpnext.pos.localSource.entities.POSInvoiceEntity
import com.erpnext.pos.localSource.entities.POSInvoiceItemEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.POSProfileEntity
import com.erpnext.pos.localSource.entities.PaymentModesEntity
import com.erpnext.pos.localSource.entities.PendingSalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ItemEntity::class,
        POSProfileEntity::class,
        PaymentModesEntity::class,
        POSInvoiceEntity::class,
        POSInvoiceItemEntity::class,
        POSInvoicePaymentEntity::class,
        CashboxEntity::class,
        BalanceDetailsEntity::class,
        CustomerEntity::class,
        PendingSalesInvoiceEntity::class,
        CategoryEntity::class,
        SalesInvoiceEntity::class,
        SalesInvoiceItemEntity::class,
    ], version = 31,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun posProfileDao(): POSProfileDao
    abstract fun paymentModesDao(): PaymentModesDao
    abstract fun cashboxDao(): CashboxDao
    abstract fun customerDao(): CustomerDao
    abstract fun pendingInvoiceDao(): PendingInvoiceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleInvoiceDao(): SalesInvoiceDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect class DatabaseBuilder {
    fun build(): AppDatabase
}
