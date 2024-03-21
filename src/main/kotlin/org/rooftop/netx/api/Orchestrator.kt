package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface Orchestrator<T : Any, V : Any> {

    fun transaction(request: T): Mono<Result<V>>

    fun transaction(timeoutMillis: Long, request: T): Mono<Result<V>>

    fun transactionSync(request: T): Result<V>

    fun transactionSync(timeoutMillis: Long, request: T): Result<V>
}
