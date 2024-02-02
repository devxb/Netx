package org.rooftop.netx.api

data class TransactionRollbackEvent(
    val transactionId: String,
    val replay: String,
    val nodeName: String,
    val cause: String?,
)
