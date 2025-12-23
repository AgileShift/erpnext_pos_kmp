package com.erpnext.pos.domain.models

data class ItemBO(
    var name: String = "",
    var description: String,
    var itemCode: String = "",
    var barcode: String = "",
    var image: String? = "",
    var currency: String? = "",
    var itemGroup: String = "",
    var brand: String? = null,
    var price: Double = 0.0,
    var actualQty: Double = 0.0,
    var discount: Double = 0.0,
    var isService: Boolean = false,
    var isStocked: Boolean = false,
    var uom: String,
    var lastSyncedAt: Long? = null
)