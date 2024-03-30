package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface Orchestrator<T : Any, V : Any> {

    fun saga(request: T): Mono<Result<V>>

    fun saga(timeoutMillis: Long, request: T): Mono<Result<V>>

    fun saga(request: T, context: MutableMap<String, Any>): Mono<Result<V>>

    fun saga(timeoutMillis: Long, request: T, context: MutableMap<String, Any>): Mono<Result<V>>

    fun sagaSync(request: T): Result<V>

    fun sagaSync(timeoutMillis: Long, request: T): Result<V>

    fun sagaSync(request: T, context: MutableMap<String, Any>): Result<V>

    fun sagaSync(timeoutMillis: Long, request: T, context: MutableMap<String, Any>): Result<V>
}
