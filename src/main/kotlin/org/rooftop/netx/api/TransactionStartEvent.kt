package org.rooftop.netx.api

class TransactionStartEvent(
    transactionId: String,
    nodeName: String,
    group: String,
) : TransactionEvent(transactionId, nodeName, group)
