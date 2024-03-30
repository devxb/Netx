package org.rooftop.netx.api

class SagaCommitEvent internal constructor(
    id: String,
    nodeName: String,
    group: String,
    event: String?,
    codec: Codec,
) : SagaEvent(id, nodeName, group, event, codec) {

    override fun copy(): SagaCommitEvent =
        SagaCommitEvent(id, nodeName, group, event, codec)
}
