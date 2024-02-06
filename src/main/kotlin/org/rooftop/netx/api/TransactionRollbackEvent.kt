package org.rooftop.netx.api

data class TransactionRollbackEvent(
    val transactionId: String,
    val nodeName: String,
    val cause: String?,
    val undoState: String,
)
