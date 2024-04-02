package org.rooftop.netx.api

import org.rooftop.netx.core.Codec
import kotlin.reflect.KClass

/**
 * Context maintained in each saga.
 *
 * The lifecycle of the Context is per saga request.
 *
 * @see Orchestrator
 * @see ContextOrchestrate
 * @see ContextRollback
 */
data class Context internal constructor(
    private val codec: Codec,
    internal val contexts: MutableMap<String, String>,
) {

    /**
     * Sets the value with the specified key in the Context.
     *
     * Once set, it can be used in subsequent orchestrate and rollback operations.
     *
     * If the key already exists, it will overwrite the value.
     */
    fun <T : Any> set(key: String, value: T) {
        contexts[key] = codec.encode(value)
    }

    /**
     * Decodes the value associated with the key using the provided type reference.
     *
     * Use TypeReference when decoding types with generics.
     *
     * Example.
     *
     *      context.decodeContext("foos", object: TypeReference<List<Foo>>(){})
     *
     * @param typeReference
     * @param T
     */
    fun <T : Any> decodeContext(key: String, typeReference: TypeReference<T>): T =
        contexts[key]?.let {
            codec.decode(it, typeReference)
        } ?: throw NullPointerException("Cannot find context by key \"$key\"")

    /**
     * Decodes the value associated with the key using the provided Class.
     *
     * @param type
     * @param T
     */
    fun <T : Any> decodeContext(key: String, type: Class<T>): T = decodeContext(key, type.kotlin)

    /**
     * @see decodeContext
     */
    fun <T : Any> decodeContext(key: String, type: KClass<T>): T = contexts[key]?.let {
        codec.decode(it, type)
    } ?: throw NullPointerException("Cannot find context by key \"$key\"")
}
