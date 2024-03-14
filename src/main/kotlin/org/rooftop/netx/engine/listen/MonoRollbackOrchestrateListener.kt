package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.OrchestrateResultHolder
import org.rooftop.netx.engine.core.TransactionState
import reactor.core.publisher.Mono

class MonoRollbackOrchestrateListener(
    private val codec: Codec,
    private val orchestrateId: String,
    private val orchestrateFunction: OrchestrateFunction<Mono<Any>>,
    private val orchestrateResultHolder: OrchestrateResultHolder,
) {

    @TransactionRollbackListener(OrchestrateEvent::class)
    fun listenRollbackOrchestrateEvent(transactionRollbackEvent: TransactionRollbackEvent): Mono<Unit> {
        return Mono.just(transactionRollbackEvent)
            .map { it.decodeEvent(OrchestrateEvent::class) }
            .filter { it.orchestrateId == orchestrateId }
            .map { OrchestrateRequest(it.clientEvent, codec) }
            .flatMap { orchestrateFunction.invoke(it) }
            .flatMap {
                orchestrateResultHolder.setResult(
                    transactionRollbackEvent.transactionId,
                    TransactionState.ROLLBACK,
                    it,
                )
            }
            .map { }
    }
}
