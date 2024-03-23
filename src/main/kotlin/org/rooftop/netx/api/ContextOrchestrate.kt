package org.rooftop.netx.api

fun interface ContextOrchestrate<T : Any, V : Any> {

    fun orchestrate(context: Context, request: T): V
}
