package com.erpnext.pos.printing.policy

class PrintRetryPolicy(
    private val maxAttempts: Int = 3
) {
    fun shouldRetry(attempts: Int): Boolean = attempts < maxAttempts
}