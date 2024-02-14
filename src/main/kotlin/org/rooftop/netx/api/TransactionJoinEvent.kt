package org.rooftop.netx.api

class TransactionJoinEvent(
    transactionId: String,
    nodeName: String,
    group: String,
): TransactionEvent(transactionId, nodeName, group)
