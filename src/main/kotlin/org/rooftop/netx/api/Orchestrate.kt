package org.rooftop.netx.api

fun interface Orchestrate<T : Any, V : Any> {

    fun orchestrate(request: T): V
}
