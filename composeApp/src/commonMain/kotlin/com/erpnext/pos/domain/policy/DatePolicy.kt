package com.erpnext.pos.domain.policy

interface DatePolicy {
    /**
     * Fecha mínima (inclusive) desde la cual se cargan invoices
     * Formato: yyyy-MM-dd (ERPNext-Room compatible)
     */
    fun invoicesFromDate(): String
}