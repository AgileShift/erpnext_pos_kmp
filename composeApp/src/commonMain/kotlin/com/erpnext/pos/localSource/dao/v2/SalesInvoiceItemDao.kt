package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity

@Dao
interface SalesInvoiceItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SalesInvoiceItemEntity>)
}
