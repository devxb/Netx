package org.rooftop.netx.api

import kotlin.reflect.KClass

data class OrchestrateResult<T : Any>(
    val isSuccess: Boolean,
    private val codec: Codec,
    private val result: String,
) {

    fun decodeResult(type: Class<T>): T = decodeResult(type.kotlin)

    fun decodeResult(type: KClass<T>): T = codec.decode(result, type)
}
