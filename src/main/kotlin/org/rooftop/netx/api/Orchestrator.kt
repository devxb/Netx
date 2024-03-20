package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface Orchestrator<T : Any, V : Any> {

    fun transaction(request: T): Mono<OrchestrateResult<V>>

    fun transaction(timeoutMillis: Long, request: T): Mono<OrchestrateResult<V>>

    fun transactionSync(request: T): OrchestrateResult<V>

    fun transactionSync(timeoutMillis: Long, request: T): OrchestrateResult<V>
}
