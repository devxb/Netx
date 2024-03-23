package org.rooftop.netx.api

import kotlin.reflect.KClass

data class Context internal constructor(
    private val codec: Codec,
    internal val contexts: MutableMap<String, String>,
) {

    fun <T : Any> set(key: String, value: T) {
        contexts[key] = codec.encode(value)
    }

    fun <T : Any> decodeContext(key: String, type: Class<T>): T = decodeContext(key, type.kotlin)

    fun <T : Any> decodeContext(key: String, type: KClass<T>): T = contexts[key]?.let {
        codec.decode(it, type)
    } ?: throw NullPointerException("Cannot find context by key \"$key\"")
}
