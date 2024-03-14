package org.rooftop.netx.api

import kotlin.reflect.KClass

class OrchestrateRequest(
    private val event: String,
    private val codec: Codec,
) {

    fun <T : Any> decodeEvent(type: Class<T>): T = decodeEvent(type.kotlin)

    fun <T : Any> decodeEvent(type: KClass<T>): T = codec.decode(event, type)
}
