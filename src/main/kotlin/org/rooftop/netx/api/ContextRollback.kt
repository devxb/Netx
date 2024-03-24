package org.rooftop.netx.api

fun interface ContextRollback<T : Any, V : Any?> : TypeReified<T> {

    fun rollback(context: Context, request: T): V

    override fun reified(): TypeReference<T>? = null
}
