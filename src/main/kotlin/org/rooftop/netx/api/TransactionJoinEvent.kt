package org.rooftop.netx.api

class TransactionJoinEvent(
    transactionId: String,
    nodeName: String,
    group: String,
    event: String?,
    codec: Codec,
): TransactionEvent(transactionId, nodeName, group, event, codec)
