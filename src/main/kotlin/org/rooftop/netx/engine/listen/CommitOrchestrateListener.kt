package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import reactor.core.publisher.Mono

internal class CommitOrchestrateListener<T : Any, V : Any> internal constructor(
    codec: Codec,
    sagaManager: SagaManager,
    private val orchestratorId: String,
    orchestrateSequence: Int,
    private val orchestrateCommand: OrchestrateCommand<T, V>,
    private val resultHolder: ResultHolder,
    requestHolder: RequestHolder,
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
            .map { (request, event) ->
                orchestrateCommand.command(request, event.context)
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
