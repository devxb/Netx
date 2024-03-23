package org.rooftop.netx.api

fun interface Rollback<T : Any, V : Any?> {

    fun rollback(request: T): V
}
