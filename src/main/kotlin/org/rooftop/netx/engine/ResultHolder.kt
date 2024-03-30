package org.rooftop.netx.engine

import org.rooftop.netx.api.Result
import reactor.core.publisher.Mono
import kotlin.time.Duration

internal interface ResultHolder {

    fun <T : Any> getResult(timeout: Duration, id: String): Mono<Result<T>>

    fun <T : Any> setSuccessResult(id: String, result: T): Mono<T>

    fun <T : Throwable> setFailResult(id: String, result: T) : Mono<T>
}
