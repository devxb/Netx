package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionCommitListener
import org.rooftop.netx.api.TransactionManager
import org.rooftop.netx.engine.AbstractOrchestrateListener
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import reactor.core.publisher.Mono

internal class MonoCommitOrchestrateListener<T : Any, V : Any> internal constructor(
    private val codec: Codec,
    transactionManager: TransactionManager,
    private val orchestratorId: String,
    orchestrateSequence: Int,
    private val monoOrchestrateCommand: MonoOrchestrateCommand<T, V>,
    requestHolder: RequestHolder,
    private val resultHolder: ResultHolder,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    transactionManager,
    requestHolder,
    resultHolder,
) {
    @TransactionCommitListener(OrchestrateEvent::class)
    fun listenCommitOrchestrateEvent(transactionCommitEvent: TransactionCommitEvent): Mono<Unit> {
        return Mono.just(transactionCommitEvent)
            .map { it.decodeEvent(OrchestrateEvent::class) }
            .filter { it.orchestrateSequence == orchestrateSequence && it.orchestratorId == orchestratorId }
            .map { event ->
                codec.decode(event.clientEvent, getCastableType()) to event
            }
            .flatMap { (request, event) ->
                holdRequestIfRollbackable(request, transactionCommitEvent.transactionId)
                    .map{ it to event }
            }
            .flatMap { (request, event) ->
                monoOrchestrateCommand.command(request, event.context)
            }
            .flatMap { (response, _) ->
                resultHolder.setSuccessResult(transactionCommitEvent.transactionId, response)
            }
            .onErrorRollback(
                transactionCommitEvent.transactionId,
                transactionCommitEvent.decodeEvent(OrchestrateEvent::class)
            )
            .map { }
    }
}
