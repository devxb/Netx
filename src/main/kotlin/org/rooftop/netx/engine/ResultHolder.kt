package org.rooftop.netx.engine

import org.rooftop.netx.api.Result
import reactor.core.publisher.Mono
import kotlin.time.Duration

internal interface ResultHolder {

    fun <T : Any> getResult(timeout: Duration, transactionId: String): Mono<Result<T>>

    fun <T : Any> setSuccessResult(transactionId: String, result: T): Mono<T>

    fun <T : Throwable> setFailResult(transactionId: String, result: T) : Mono<T>
}
