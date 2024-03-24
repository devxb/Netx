package org.rooftop.netx.api

fun interface TypeReified<T : Any> {

    fun reified(): TypeReference<T>?
}
