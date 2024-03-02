package org.rooftop.netx.engine.logging

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal lateinit var logger: Logger

internal fun info(message: String) = logger.info(message)

internal fun warning(message: String) = logger.warn(message)

internal fun infoOnError(message: String, throwable: Throwable) = logger.info(message, throwable)

internal fun warningOnError(message: String, throwable: Throwable) = logger.warn(message, throwable)

internal fun error(message: String) = logger.error(message)

internal fun <T> Mono<T>.info(message: String): Mono<T> = this.doOnNext {
    logger.info(message)
}

internal fun <T> Mono<T>.infoOnError(message: String): Mono<T> = this.doOnError {
    logger.info(message, it)
}

internal fun <T> Mono<T>.warningOnError(message: String): Mono<T> = this.doOnError {
    logger.warn(message, it)
}


internal fun <T> Flux<T>.info(message: String): Flux<T> = this.doOnNext {
    logger.info(message)
}

internal fun <T> Flux<T>.warningOnError(message: String): Flux<T> = this.doOnError {
    logger.warn(message, it)
}
