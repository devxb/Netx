package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.OrchestrateEvent
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass

class StartOrchestrateListener(
    private val isLast: Boolean = false,
    private val codec: Codec,
    private val transactionManager: TransactionManager,
    private val orchestrateId: String,
    private val orchestrateSequence: Int,
    private val orchestrateFunction: OrchestrateFunction<Any>,
    private val noRollbackFor: Array<out KClass<out Throwable>>,
) {

    @TransactionStartListener(OrchestrateEvent::class)
    fun listenStartOrchestrateEvent(transactionStartEvent: TransactionStartEvent): Mono<Unit> {
        return Mono.just(transactionStartEvent)
            .map { it.decodeEvent(OrchestrateEvent::class) }
            .filter {
                it.orchestrateSequence == orchestrateSequence
                        && it.orchestrateId == orchestrateId
            }
            .map { OrchestrateRequest(it.clientEvent, codec) to it }
            .map { (request, event) ->
                orchestrateFunction.invoke(request) to event
            }
            .onErrorResume {
                if (isNoRollbackFor(it)) {
                    throw it
                }
                rollback(it, transactionStartEvent)
                Mono.empty()
            }
            .map { (response, event) ->
                if (isLast) {
                    return@map OrchestrateEvent(
                        orchestrateId = orchestrateId,
                        clientEvent = codec.encode(response)
                    )
                }
                OrchestrateEvent(
                    orchestrateId = orchestrateId,
                    orchestrateSequence = event.orchestrateSequence + 1,
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
            .map { }
    }

    private fun rollback(
        it: Throwable,
        transactionStartEvent: TransactionStartEvent
    ) {
        val orchestrateEvent =
            OrchestrateEvent(
                orchestrateId = orchestrateId,
                clientEvent = it.message ?: it.localizedMessage
            )
        transactionManager.rollback(
            transactionId = transactionStartEvent.transactionId,
            cause = it.message ?: it.localizedMessage,
            event = orchestrateEvent
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()
    }

    private fun isNoRollbackFor(throwable: Throwable) =
        noRollbackFor.isNotEmpty() && noRollbackFor.contains(throwable::class)
}
