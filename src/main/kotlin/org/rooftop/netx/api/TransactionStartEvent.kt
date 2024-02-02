package org.rooftop.netx.api

data class TransactionStartEvent(
    val transactionId: String,
    val replay: String,
    val nodeName: String,
)
