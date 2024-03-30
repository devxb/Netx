package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface SagaManager {

    fun start(): Mono<String>

    fun <T : Any> start(event: T): Mono<String>

    fun syncStart(): String

    fun <T : Any> syncStart(event: T): String

    fun join(id: String): Mono<String>

    fun <T : Any> join(id: String, event: T): Mono<String>

    fun syncJoin(id: String): String

    fun <T : Any> syncJoin(id: String, event: T): String

    fun exists(id: String): Mono<String>

    fun syncExists(id: String): String

    fun commit(id: String): Mono<String>

    fun <T : Any> commit(id: String, event: T): Mono<String>

    fun syncCommit(id: String): String

    fun <T : Any> syncCommit(id: String, event: T): String

    fun rollback(id: String, cause: String): Mono<String>

    fun <T : Any> rollback(id: String, cause: String, event: T): Mono<String>

    fun syncRollback(id: String, cause: String): String

    fun <T : Any> syncRollback(id: String, cause: String, event: T): String

}
