package com.erpnext.pos.domain.policy

interface BackoffPolicy {
    /**
     * Retorna el delay en milisegundos antes del próximo intento
     *
     * @param attempt número de intento (0-based)
     * @param randomFactor valor entre 0.0 y 1.0 para jitter
     */
    fun nextDelayMs(attempt: Int, randomFactor: Double): Long
}