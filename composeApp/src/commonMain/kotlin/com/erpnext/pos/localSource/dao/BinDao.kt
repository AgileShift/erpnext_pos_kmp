package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface BinDao {
    @Query("""
        UPDATE inventory_bins
        SET actualQty = CASE 
            WHEN (actualQty - :qty) < 0 THEN 0 
            ELSE (actualQty - :qty) 
        END
    """)
    //WHERE warehouse = :warehouse AND itemCode = :itemCode
    suspend fun decrementActualQty(warehouse: String, itemCode: String, qty: Double)
}
