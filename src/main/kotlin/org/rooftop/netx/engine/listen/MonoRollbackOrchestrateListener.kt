package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.AbstractOrchestrateListener
import org.rooftop.netx.engine.OrchestrateEvent
import reactor.core.publisher.Mono

internal class MonoRollbackOrchestrateListener<T : Any, V : Any>(
    private val codec: Codec,
    private val orchestratorId: String,
    private val orchestrateSequence: Int,
    transactionManager: TransactionManager,
    private val rollbackFunction: RollbackFunction<T, Mono<V?>>,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    transactionManager
) {

    @TransactionRollbackListener(OrchestrateEvent::class)
    fun listenRollbackOrchestrateEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return Mono.just(transactionRollbackEvent)
            .map { it.decodeEvent(OrchestrateEvent::class) }
            .filter { it.orchestratorId == orchestratorId && it.orchestrateSequence == orchestrateSequence }
            .flatMap { event ->
                val request = codec.decode(event.clientEvent, getCastableType())
                rollbackFunction.rollback(request)
            }
            .map { }
    }
}
