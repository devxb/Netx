package org.rooftop.netx.api

fun interface RollbackFunction<T : Any, V : Any?> {

    fun rollback(request: T): V
}
