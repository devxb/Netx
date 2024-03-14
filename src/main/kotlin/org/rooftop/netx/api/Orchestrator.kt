package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface Orchestrator<T : Any> {

    fun transaction(request: T): Mono<OrchestrateResult>

    fun transactionSync(request: T): OrchestrateResult
}
