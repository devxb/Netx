package org.rooftop.netx.api

import kotlin.reflect.KClass

class OrchestrateResult(
    val isSuccess: Boolean,
    private val codec: Codec,
    private val result: String
) {

    fun <T : Any> decodeResult(type: Class<T>): T = decodeResult(type.kotlin)

    fun <T : Any> decodeResult(type: KClass<T>): T = codec.decode(result, type)
}
