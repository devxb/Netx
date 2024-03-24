package org.rooftop.netx.engine

import org.rooftop.netx.api.TypeReference
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

internal interface RequestHolder {


    fun <T : Any> getRequest(key: String, type: KClass<T>): Mono<T>

    fun <T : Any> getRequest(key: String, typeReference: TypeReference<T>): Mono<T>

    fun <T : Any> setRequest(key: String, request: T): Mono<T>

}
