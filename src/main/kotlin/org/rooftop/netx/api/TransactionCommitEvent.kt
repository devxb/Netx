package org.rooftop.netx.api

class TransactionCommitEvent(
    transactionId: String,
    nodeName: String,
    group: String,
): TransactionEvent(transactionId, nodeName, group)
