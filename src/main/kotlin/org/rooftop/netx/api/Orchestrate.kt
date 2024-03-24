package org.rooftop.netx.api

fun interface Orchestrate<T : Any, V : Any> : TypeReified<T> {

    fun orchestrate(request: T): V
    
    override fun reified(): TypeReference<T>? = null
}
