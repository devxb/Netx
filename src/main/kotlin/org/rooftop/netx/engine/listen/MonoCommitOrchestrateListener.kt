package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.AbstractOrchestrateListener
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.OrchestrateResultHolder
import org.rooftop.netx.engine.core.TransactionState
import reactor.core.publisher.Mono

internal class MonoCommitOrchestrateListener<T : Any, V : Any> internal constructor(
    private val codec: Codec,
    transactionManager: TransactionManager,
    private val orchestratorId: String,
    private val orchestrateSequence: Int,
    private val orchestrateFunction: OrchestrateFunction<T, Mono<V>>,
    private val orchestrateResultHolder: OrchestrateResultHolder,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    transactionManager,
) {
    @TransactionCommitListener(OrchestrateEvent::class)
    fun listenCommitOrchestrateEvent(transactionCommitEvent: TransactionCommitEvent): Mono<Unit> {
        return Mono.just(transactionCommitEvent)
            .map { it.decodeEvent(OrchestrateEvent::class) }
            .filter { it.orchestrateSequence == orchestrateSequence && it.orchestratorId == orchestratorId }
            .flatMap { event ->
                val request = codec.decode(event.clientEvent, getCastableType())
                orchestrateFunction.orchestrate(request)
            }
            .flatMap {
                orchestrateResultHolder.setResult(
                    transactionCommitEvent.transactionId,
                    TransactionState.COMMIT,
                    it,
                )
            }
            .onErrorRollback(
                transactionCommitEvent.transactionId,
                transactionCommitEvent.decodeEvent(OrchestrateEvent::class)
            )
            .map { }
    }
}
