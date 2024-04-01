package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface SagaManager {

    fun start(): Mono<String>

    fun <T : Any> start(event: T): Mono<String>

    fun startSync(): String

    fun <T : Any> startSync(event: T): String

    fun join(id: String): Mono<String>

    fun <T : Any> join(id: String, event: T): Mono<String>

    fun joinSync(id: String): String

    fun <T : Any> joinSync(id: String, event: T): String

    fun exists(id: String): Mono<String>

    fun existsSync(id: String): String

    fun commit(id: String): Mono<String>

    fun <T : Any> commit(id: String, event: T): Mono<String>

    fun commitSync(id: String): String

    fun <T : Any> commitSync(id: String, event: T): String

    fun rollback(id: String, cause: String): Mono<String>

    fun <T : Any> rollback(id: String, cause: String, event: T): Mono<String>

    fun rollbackSync(id: String, cause: String): String

    fun <T : Any> rollbackSync(id: String, cause: String, event: T): String

}
