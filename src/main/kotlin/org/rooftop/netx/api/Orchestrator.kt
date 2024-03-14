package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface Orchestrator<T : Any> {

    fun transaction(request: T): Mono<OrchestrateResult>

    fun transaction(timeoutMillis: Long, request: T): Mono<OrchestrateResult>

    fun transactionSync(request: T): OrchestrateResult

    fun transactionSync(timeoutMillis: Long, request: T): OrchestrateResult
}
