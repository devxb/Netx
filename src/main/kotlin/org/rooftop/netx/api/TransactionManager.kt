package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface TransactionManager {

    fun <T : Any> start(undo: T): Mono<String>

    fun <T : Any, S : Any> start(undo: T, event: S): Mono<String>

    fun <T : Any> syncStart(undo: T): String

    fun <T : Any, S : Any> syncStart(undo: T, event: S): String

    fun <T : Any> join(transactionId: String, undo: T): Mono<String>

    fun <T : Any, S : Any> join(transactionId: String, undo: T, event: S): Mono<String>

    fun <T : Any> syncJoin(transactionId: String, undo: T): String

    fun <T : Any, S : Any> syncJoin(transactionId: String, undo: T, event: S): String

    fun exists(transactionId: String): Mono<String>

    fun syncExists(transactionId: String): String

    fun commit(transactionId: String): Mono<String>

    fun <T : Any> commit(transactionId: String, event: T): Mono<String>

    fun syncCommit(transactionId: String): String

    fun <T : Any> syncCommit(transactionId: String, event: T): String

    fun rollback(transactionId: String, cause: String): Mono<String>

    fun <T : Any> rollback(transactionId: String, cause: String, event: T): Mono<String>

    fun syncRollback(transactionId: String, cause: String): String

    fun <T : Any> syncRollback(transactionId: String, cause: String, event: T): String

}
