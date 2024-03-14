package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.OrchestrateEvent
import org.rooftop.netx.engine.OrchestrateResultHolder
import org.rooftop.netx.engine.core.TransactionState
import org.rooftop.netx.engine.logging.info
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass

class MonoCommitOrchestrateListener(
    private val codec: Codec,
    private val transactionManager: TransactionManager,
    private val orchestrateId: String,
    private val orchestrateFunction: OrchestrateFunction<Mono<Any>>,
    private val orchestrateResultHolder: OrchestrateResultHolder,
    private val noRollbackFor: Array<out KClass<out Throwable>>,
) {

    @TransactionCommitListener(OrchestrateEvent::class)
    fun listenCommitOrchestrateEvent(transactionCommitEvent: TransactionCommitEvent): Mono<Unit> {
        return Mono.just(transactionCommitEvent)
            .map { it.decodeEvent(OrchestrateEvent::class) }
            .filter { it.orchestrateId == orchestrateId }
            .map { OrchestrateRequest(it.clientEvent, codec) }
            .flatMap { orchestrateFunction.invoke(it) }
            .flatMap {
                orchestrateResultHolder.setResult(
                    transactionCommitEvent.transactionId,
                    TransactionState.COMMIT,
                    it,
                )
            }
            .onErrorResume {
                if (isNoRollbackFor(it)) {
                    throw it
                }
                rollback(it, transactionCommitEvent)
                Mono.empty()
            }
            .map { }
    }

    private fun rollback(
        it: Throwable,
        transactionCommitEvent: TransactionCommitEvent
    ) {
        val orchestrateEvent =
            OrchestrateEvent(
                orchestrateId = orchestrateId,
                clientEvent = it.message ?: it.localizedMessage
            )
        transactionManager.rollback(
            transactionId = transactionCommitEvent.transactionId,
            cause = it.message ?: it.localizedMessage,
            event = orchestrateEvent
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()
    }

    private fun isNoRollbackFor(throwable: Throwable) =
        noRollbackFor.isNotEmpty() && noRollbackFor.contains(throwable::class)
}
