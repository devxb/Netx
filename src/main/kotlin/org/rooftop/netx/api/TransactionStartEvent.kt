package org.rooftop.netx.api

data class TransactionStartEvent(
    val transactionId: String,
    val nodeName: String,
)
