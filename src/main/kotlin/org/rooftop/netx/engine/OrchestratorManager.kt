package org.rooftop.netx.engine

import org.rooftop.netx.api.*
import reactor.core.publisher.Mono
import kotlin.time.Duration.Companion.milliseconds

class OrchestratorManager<T : Any>(
    private val transactionManager: TransactionManager,
    private val codec: Codec,
    private val orchestrateId: String,
    private val orchestrateResultHolder: OrchestrateResultHolder,
) : Orchestrator<T> {

    override fun transactionSync(request: T): OrchestrateResult {
        return transaction(request).block()
            ?: throw TransactionException("Cannot start transaction \"$request\"")
    }

    override fun transactionSync(timeoutMillis: Long, request: T): OrchestrateResult {
        return transaction(timeoutMillis, request).block()
            ?: throw TransactionException("Cannot start transaction \"$request\"")
    }

    override fun transaction(request: T): Mono<OrchestrateResult> {
        return transaction(ONE_MINUTES_TO_TIME_OUT, request)
    }

    override fun transaction(timeoutMillis: Long, request: T): Mono<OrchestrateResult> {
        return Mono.just(request)
            .map {
                OrchestrateEvent(
                    orchestrateId = orchestrateId,
                    clientEvent = codec.encode(request),
                )
            }
            .flatMap { transactionManager.start(UNDO, it) }
            .flatMap { orchestrateResultHolder.getResult(timeoutMillis.milliseconds, it) }
    }

    private companion object {
        private const val UNDO = "Orchestrate mode";
        private const val ONE_MINUTES_TO_TIME_OUT = 60000L
    }
}
