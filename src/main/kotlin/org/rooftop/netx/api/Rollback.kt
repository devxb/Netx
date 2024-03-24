package org.rooftop.netx.api

fun interface Rollback<T : Any, V : Any?>: TypeReified<T> {

    fun rollback(request: T): V

    override fun reified(): TypeReference<T>? = null
}
