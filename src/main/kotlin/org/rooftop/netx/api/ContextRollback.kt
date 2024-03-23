package org.rooftop.netx.api

fun interface ContextRollback<T : Any, V : Any?> {

    fun rollback(context: Context, request: T): V
}
