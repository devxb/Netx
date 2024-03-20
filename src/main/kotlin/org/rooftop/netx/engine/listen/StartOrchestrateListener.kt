package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.AbstractOrchestrateListener
import org.rooftop.netx.engine.OrchestrateEvent
import reactor.core.publisher.Mono

internal class StartOrchestrateListener<T : Any, V : Any>(
    private val codec: Codec,
    private val transactionManager: TransactionManager,
    private val orchestratorId: String,
    private val orchestrateSequence: Int,
    private val orchestrateFunction: OrchestrateFunction<T, V>,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    transactionManager
) {

    @TransactionStartListener(OrchestrateEvent::class)
    fun listenStartOrchestrateEvent(transactionStartEvent: TransactionStartEvent): Mono<Unit> {
        return transactionStartEvent.toOrchestrateEvent()
            .filter { it.orchestratorId == orchestratorId && it.orchestrateSequence == orchestrateSequence }
            .map { event ->
                val request = codec.decode(event.clientEvent, getCastableType())
                orchestrateFunction.orchestrate(request)
            }
            .setNextCastableType()
            .onErrorRollback(
                transactionStartEvent.transactionId,
                transactionStartEvent.decodeEvent(OrchestrateEvent::class)
            )
            .map { response ->
                OrchestrateEvent(
                    orchestratorId = orchestratorId,
                    orchestrateSequence = orchestrateSequence + 1,
                    clientEvent = codec.encode(response),
                )
            }
            .flatMap {
                if (isLast) {
                    return@flatMap transactionManager.commit(
                        transactionId = transactionStartEvent.transactionId,
                        event = it,
                    )
                }
                transactionManager.join(
                    transactionId = transactionStartEvent.transactionId,
                    undo = "",
                    event = it,
                )
            }
            .onErrorResume {
                if (it::class == AlreadyCommittedTransactionException::class) {
                    return@onErrorResume Mono.empty()
                }
                throw it
            }
            .map { }
    }
}
