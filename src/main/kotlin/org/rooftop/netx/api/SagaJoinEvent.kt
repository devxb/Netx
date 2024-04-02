package org.rooftop.netx.api

import org.rooftop.netx.core.Codec

/**
 * @see SagaEvent
 * @see SagaJoinListener
 */
class SagaJoinEvent internal constructor(
    id: String,
    nodeName: String,
    group: String,
    event: String?,
    codec: Codec,
) : SagaEvent(id, nodeName, group, event, codec) {

    override fun copy(): SagaJoinEvent =
        SagaJoinEvent(id, nodeName, group, event, codec)
}
