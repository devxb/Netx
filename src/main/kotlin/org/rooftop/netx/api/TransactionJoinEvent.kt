package org.rooftop.netx.api

data class TransactionJoinEvent(
    val transactionId: String,
    val replay: String,
    val nodeName: String,
)
