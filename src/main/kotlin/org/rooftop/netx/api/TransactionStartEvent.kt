package org.rooftop.netx.api

class TransactionStartEvent internal constructor(
    transactionId: String,
    nodeName: String,
    group: String,
    event: String?,
    codec: Codec,
) : TransactionEvent(transactionId, nodeName, group, event, codec) {

    override fun copy(): TransactionStartEvent =
        TransactionStartEvent(transactionId, nodeName, group, event, codec)
}
