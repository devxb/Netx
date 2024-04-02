package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.core.Codec
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import reactor.core.publisher.Mono

internal class MonoRollbackOrchestrateListener<T : Any, V : Any>(
    private val codec: Codec,
    private val orchestratorId: String,
    orchestrateSequence: Int,
    private val sagaManager: SagaManager,
    private val monoRollbackCommand: MonoRollbackCommand<T>,
    requestHolder: RequestHolder,
    resultHolder: ResultHolder,
    typeReference: TypeReference<T>?,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    sagaManager,
    requestHolder,
    resultHolder,
    typeReference,
) {

    @SagaRollbackListener(OrchestrateEvent::class)
    fun listenRollbackOrchestrateEvent(sagaRollbackEvent: SagaRollbackEvent): Mono<Unit> {
        return sagaRollbackEvent.startWithOrchestrateEvent()
            .filter { it.orchestratorId == orchestratorId && it.orchestrateSequence == orchestrateSequence }
            .getHeldRequest(sagaRollbackEvent)
            .flatMap { (request, event) ->
                monoRollbackCommand.command(request, event.context)
            }
            .cascadeRollback(sagaRollbackEvent)
    }

    private fun Mono<Pair<Any?, Context>>.cascadeRollback(sagaRollbackEvent: SagaRollbackEvent): Mono<Unit> {
        return this.filter { !isFirst }
            .flatMap { (_, context) ->
                val orchestrateEvent = sagaRollbackEvent.decodeEvent(OrchestrateEvent::class)
                val nextOrchestrateEvent = OrchestrateEvent(
                    orchestrateEvent.orchestratorId,
                    beforeRollbackOrchestrateSequence,
                    orchestrateEvent.clientEvent,
                    codec.encode(context.contexts),
                )
                sagaManager.rollback(
                    sagaRollbackEvent.id,
                    sagaRollbackEvent.cause,
                    nextOrchestrateEvent
                )
            }.map { }
    }
}
