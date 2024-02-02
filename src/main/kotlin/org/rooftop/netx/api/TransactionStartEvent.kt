package org.rooftop.netx.api

data class TransactionStartEvent(
    private val transactionId: String,
    private val replay: String,
)
