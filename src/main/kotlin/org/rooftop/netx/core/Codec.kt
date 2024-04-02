package org.rooftop.netx.core

import org.rooftop.netx.api.TypeReference
import kotlin.reflect.KClass

interface Codec {

    fun <T : Any> encode(data: T): String

    fun <T : Any> decode(data: String, type: KClass<T>): T

    fun <T : Any> decode(data: String, type: TypeReference<T>): T
}
