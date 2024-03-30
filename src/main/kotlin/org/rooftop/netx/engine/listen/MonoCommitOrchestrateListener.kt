package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import reactor.core.publisher.Mono

internal class MonoCommitOrchestrateListener<T : Any, V : Any> internal constructor(
    codec: Codec,
    sagaManager: SagaManager,
    private val orchestratorId: String,
    orchestrateSequence: Int,
    private val monoOrchestrateCommand: MonoOrchestrateCommand<T, V>,
    requestHolder: RequestHolder,
    private val resultHolder: ResultHolder,
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
    @SagaCommitListener(OrchestrateEvent::class)
    fun listenCommitOrchestrateEvent(sagaCommitEvent: SagaCommitEvent): Mono<V> {
        return sagaCommitEvent.startWithOrchestrateEvent()
            .filter { it.orchestrateSequence == orchestrateSequence && it.orchestratorId == orchestratorId }
            .mapReifiedRequest()
            .flatMap { (request, event) ->
                holdRequestIfRollbackable(request, sagaCommitEvent.id)
                    .map { it to event }
            }
            .flatMap { (request, event) ->
                monoOrchestrateCommand.command(request, event.context)
            }
            .doOnError {
                rollback(
                    sagaCommitEvent.id,
                    it,
                    sagaCommitEvent.decodeEvent(OrchestrateEvent::class)
                )
            }
            .flatMap { (response, _) ->
                resultHolder.setSuccessResult(sagaCommitEvent.id, response)
            }
    }
}
