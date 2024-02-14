package org.rooftop.netx.api

class TransactionRollbackEvent(
    transactionId: String,
    nodeName: String,
    group: String,
    val cause: String?,
    val undo: String,
): TransactionEvent(transactionId, nodeName, group)
