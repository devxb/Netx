package org.rooftop.netx.engine

import org.rooftop.netx.api.Codec
import org.rooftop.netx.api.Orchestrator
import org.rooftop.netx.api.OrchestrateResult
import org.rooftop.netx.api.TransactionException
import org.rooftop.netx.api.TransactionManager
import reactor.core.publisher.Mono

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

    override fun transaction(request: T): Mono<OrchestrateResult> {
        return Mono.just(request)
            .map {
                OrchestrateEvent(
                    orchestrateId = orchestrateId,
                    clientEvent = codec.encode(request),
                )
            }
            .flatMap { transactionManager.start(UNDO, it) }
            .flatMap { orchestrateResultHolder.getResult(it) }
    }

    private companion object {
        private const val UNDO = "Orchestrate mode";
    }
}
