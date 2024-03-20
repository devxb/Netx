package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import reactor.core.publisher.Mono
import kotlin.time.Duration.Companion.milliseconds

class OrchestratorManager<T : Any, V : Any> internal constructor(
    private val transactionManager: TransactionManager,
    private val codec: Codec,
    private val orchestratorId: String,
    private val orchestrateResultHolder: OrchestrateResultHolder,
    private val orchestrateListener: AbstractOrchestrateListener<T, out Any>,
    private val rollbackOrchestrateListener: AbstractOrchestrateListener<T, out Any>?,
) : Orchestrator<T, V> {

    override fun transactionSync(request: T): OrchestrateResult<V> {
        return transaction(request).block()
            ?: throw TransactionException("Cannot start transaction \"$request\"")
    }

    override fun transactionSync(timeoutMillis: Long, request: T): OrchestrateResult<V> {
        return transaction(timeoutMillis, request).block()
            ?: throw TransactionException("Cannot start transaction \"$request\"")
    }

    override fun transaction(request: T): Mono<OrchestrateResult<V>> {
        return transaction(TEN_SECONDS_TO_TIME_OUT, request)
    }

    override fun transaction(timeoutMillis: Long, request: T): Mono<OrchestrateResult<V>> {
        return Mono.just(request)
            .doOnNext { _ ->
                orchestrateListener.setCastableType(request::class)
                rollbackOrchestrateListener?.setCastableType(request::class)
            }
            .map {
                OrchestrateEvent(
                    orchestratorId = orchestratorId,
                    clientEvent = codec.encode(request),
                )
            }
            .flatMap { transactionManager.start(UNDO, it) }
            .flatMap { orchestrateResultHolder.getResult(timeoutMillis.milliseconds, it) }
    }

    private companion object {
        private const val UNDO = "Orchestrate mode";
        private const val TEN_SECONDS_TO_TIME_OUT = 10000L
    }
}
