package org.rooftop.netx.engine.listen

import org.rooftop.netx.api.*
import org.rooftop.netx.engine.OrchestrateEvent
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.reflect.KClass

class MonoJoinOrchestrateListener(
    private val isLast: Boolean = false,
    private val codec: Codec,
    private val transactionManager: TransactionManager,
    private val orchestrateId: String,
    private val orchestrateSequence: Int,
    private val orchestrateFunction: OrchestrateFunction<Mono<Any>>,
    private val noRollbackFor: Array<out KClass<out Throwable>>,
) {

    @TransactionJoinListener(OrchestrateEvent::class)
    fun listenJoinOrchestrateEvent(transactionJoinEvent: TransactionJoinEvent): Mono<Unit> {
        return Mono.just(transactionJoinEvent)
            .map { it.decodeEvent(OrchestrateEvent::class) }
            .filter {
                it.orchestrateSequence == orchestrateSequence
                        && it.orchestrateId == orchestrateId
            }
            .map { OrchestrateRequest(it.clientEvent, codec) to it }
            .flatMap { (request, event) ->
                orchestrateFunction.orchestrate(request).map {
                    it to event
                }
            }
            .onErrorResume {
                if(it == AlreadyCommittedTransactionException::class) {
                    return@onErrorResume Mono.empty()
                }
                if (isNoRollbackFor(it)) {
                    throw it
                }
                rollback(it, transactionJoinEvent)
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

    private fun rollback(
        it: Throwable,
        transactionJoinEvent: TransactionJoinEvent
    ) {
        val orchestrateEvent =
            OrchestrateEvent(
                orchestrateId = orchestrateId,
                clientEvent = it.message ?: it.localizedMessage
            )
        transactionManager.rollback(
            transactionId = transactionJoinEvent.transactionId,
            cause = it.message ?: it.localizedMessage,
            event = orchestrateEvent
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()
    }

    private fun isNoRollbackFor(throwable: Throwable) =
        noRollbackFor.isNotEmpty() && noRollbackFor.contains(throwable::class)
}
