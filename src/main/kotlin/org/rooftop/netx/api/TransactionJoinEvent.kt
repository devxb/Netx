package org.rooftop.netx.api

class TransactionJoinEvent internal constructor(
    transactionId: String,
    nodeName: String,
    group: String,
    event: String?,
    codec: Codec,
) : TransactionEvent(transactionId, nodeName, group, event, codec) {

    override fun copy(): TransactionJoinEvent =
        TransactionJoinEvent(transactionId, nodeName, group, event, codec)
}
