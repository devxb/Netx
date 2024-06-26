package org.rooftop.netx.api

import org.rooftop.netx.core.Codec

/**
 * @see SagaEvent
 * @see SagaRollbackListener
 */
class SagaRollbackEvent internal constructor(
    id: String,
    nodeName: String,
    group: String,
    event: String?,
    val cause: String,
    codec: Codec,
) : SagaEvent(id, nodeName, group, event, codec) {

    override fun copy(): SagaRollbackEvent =
        SagaRollbackEvent(id, nodeName, group, event, cause, codec)
}
