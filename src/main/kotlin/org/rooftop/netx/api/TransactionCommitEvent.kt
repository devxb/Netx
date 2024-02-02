package org.rooftop.netx.api

data class TransactionCommitEvent(
    val transactionId: String,
    val nodeName: String,
)
