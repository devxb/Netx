package org.rooftop.netx.api

abstract class TransactionEvent(
    val transactionId: String,
    val nodeName: String,
    val group: String,
)
