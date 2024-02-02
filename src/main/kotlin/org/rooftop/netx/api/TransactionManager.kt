package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface TransactionManager {

    fun start(replay: String): Mono<String>

    fun exists(transactionId: String): Mono<String>

    fun join(transactionId: String, replay: String): Mono<String>

    fun commit(transactionId: String): Mono<String>

    fun rollback(transactionId: String, cause: String): Mono<String>

}
