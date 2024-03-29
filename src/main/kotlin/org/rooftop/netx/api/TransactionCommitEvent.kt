package org.rooftop.netx.api

class TransactionCommitEvent internal constructor(
    transactionId: String,
    nodeName: String,
    group: String,
    event: String?,
    codec: Codec,
): TransactionEvent(transactionId, nodeName, group, event, codec) {

    override fun copy(): TransactionCommitEvent =
        TransactionCommitEvent(transactionId, nodeName, group, event, codec)
}
