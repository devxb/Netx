package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.AbstractOrchestrateListener
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.RequestHolder
import org.rooftop.netx.engine.ResultHolder
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

internal class RollbackOrchestrateListener<T : Any, V : Any>(
    codec: Codec,
    private val orchestratorId: String,
    orchestrateSequence: Int,
    private val transactionManager: TransactionManager,
    private val rollbackFunction: RollbackFunction<T, *>,
    requestHolder: RequestHolder,
    resultHolder: ResultHolder,
) : AbstractOrchestrateListener<T, V>(
    orchestratorId,
    orchestrateSequence,
    codec,
    transactionManager,
    requestHolder,
    resultHolder,
) {

    @TransactionRollbackListener(OrchestrateEvent::class)
    fun listenRollbackOrchestrateEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return transactionRollbackEvent.toOrchestrateEvent()
            .filter { it.orchestratorId == orchestratorId && it.orchestrateSequence == orchestrateSequence }
            .getHeldRequest(transactionRollbackEvent)
            .map { request ->
                rollbackFunction.rollback(request)
            }
            .switchIfEmpty(Mono.just("SUCCESS ROLLBACK"))
            .map { }
            .cascadeRollback(transactionRollbackEvent)
    }

    private fun Mono<Unit>.cascadeRollback(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return this.doOnSuccess {
            val orchestrateEvent = transactionRollbackEvent.decodeEvent(OrchestrateEvent::class)
            if (!isFirst && orchestrateEvent.orchestratorId == orchestratorId
                && orchestrateEvent.orchestrateSequence == orchestrateSequence
            ) {
                val nextOrchestrateEvent = OrchestrateEvent(
                    orchestrateEvent.orchestratorId,
                    beforeRollbackOrchestrateSequence,
                    orchestrateEvent.clientEvent
                )
                transactionManager.rollback(
                    transactionRollbackEvent.transactionId,
                    transactionRollbackEvent.cause,
                    nextOrchestrateEvent
                ).subscribeOn(Schedulers.parallel()).subscribe()
            }
        }
    }
}
