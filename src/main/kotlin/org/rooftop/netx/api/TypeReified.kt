package org.rooftop.netx.api

/**
 * Provides a hint about the type by returning a TypeReference.
 */
fun interface TypeReified<T : Any> {

    fun reified(): TypeReference<T>?
}
