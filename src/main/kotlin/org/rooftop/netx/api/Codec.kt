package org.rooftop.netx.api

import kotlin.reflect.KClass

interface Codec {

    fun <T : Any> encode(data: T): String

    fun <T : Any> decode(data: String, type: KClass<T>): T
}
