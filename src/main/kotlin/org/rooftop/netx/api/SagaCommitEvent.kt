package org.rooftop.netx.api

import org.rooftop.netx.core.Codec

/**
 * @see SagaEvent
 * @see SagaCommitListener
 */
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
