package org.rooftop.netx.api

fun interface ContextOrchestrate<T : Any, V : Any> : TypeReified<T> {

    fun orchestrate(context: Context, request: T): V

    override fun reified(): TypeReference<T>? = null
}
