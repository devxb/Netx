package org.rooftop.netx.api

fun interface OrchestrateFunction<T : Any, V : Any> {

    fun orchestrate(request: T): V
}
