package org.rooftop.netx.engine

import org.rooftop.netx.api.SagaRollbackEvent
import org.rooftop.netx.api.SagaRollbackListener
import org.rooftop.netx.api.SagaStartEvent
import org.rooftop.netx.api.SagaStartListener
import org.rooftop.netx.meta.SagaHandler

@SagaHandler
class DeadLetterAnnotationClass {

    lateinit var errorInjector: ErrorInjector
    lateinit var relayResultHolder: RelayResultHolder


    @SagaStartListener(event = RelayEvent::class)
    fun sagaStartListener(sagaStartEvent: SagaStartEvent) {
        val relayEvent = sagaStartEvent.decodeEvent(RelayEvent::class)
        sagaStartEvent.setNextEvent(relayEvent)

        throw IllegalArgumentException("Rollback on start")
    }

    @SagaRollbackListener(event = RelayEvent::class)
    fun sagaRollbackListener(sagaRollbackEvent: SagaRollbackEvent) {
        val relayEvent = sagaRollbackEvent.decodeEvent(RelayEvent::class)
        if (errorInjector.doError) {
            throw IllegalStateException("Rollback on start")
        }

        relayResultHolder.hold("DeadLetterAnnotationClass", relayEvent)
    }
}
