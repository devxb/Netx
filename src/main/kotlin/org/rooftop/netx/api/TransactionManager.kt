package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface TransactionManager {

    fun <T> start(undo: T): Mono<String>

    fun <T, S> start(undo: T, event: S): Mono<String>

    fun <T> syncStart(undo: T): String

    fun <T, S> syncStart(undo: T, event: S): String

    fun <T> join(transactionId: String, undo: T): Mono<String>

    fun <T, S> join(transactionId: String, undo: T, event: S): Mono<String>

    fun <T> syncJoin(transactionId: String, undo: T): String

    fun <T, S> syncJoin(transactionId: String, undo: T, event: S): String

    fun exists(transactionId: String): Mono<String>

    fun syncExists(transactionId: String): String

    fun commit(transactionId: String): Mono<String>

    fun <T> commit(transactionId: String, event: T): Mono<String>

    fun syncCommit(transactionId: String): String

    fun <T> syncCommit(transactionId: String, event: T): String

    fun rollback(transactionId: String, cause: String): Mono<String>

    fun <T> rollback(transactionId: String, cause: String, event: T): Mono<String>

    fun syncRollback(transactionId: String, cause: String): String

    fun <T> syncRollback(transactionId: String, cause: String, event: T): String

}
