package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.AbstractOrchestrateListener
import org.rooftop.netx.engine.OrchestrateEvent
import reactor.core.publisher.Mono

internal class JoinOrchestrateListener<T : Any, V : Any>(
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

    @TransactionJoinListener(OrchestrateEvent::class)
    fun listenJoinOrchestrateEvent(transactionJoinEvent: TransactionJoinEvent): Mono<Unit> {
        return transactionJoinEvent.toOrchestrateEvent()
            .filter {
                it.orchestrateSequence == orchestrateSequence
                        && it.orchestratorId == orchestratorId
            }
            .map { event ->
                val request = codec.decode(event.clientEvent, getCastableType())
                orchestrateFunction.orchestrate(request)
            }
            .setNextCastableType()
            .onErrorRollback(
                transactionJoinEvent.transactionId,
                transactionJoinEvent.decodeEvent(OrchestrateEvent::class)
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
                        transactionId = transactionJoinEvent.transactionId,
                        event = it,
                    )
                }
                transactionManager.join(
                    transactionId = transactionJoinEvent.transactionId,
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
