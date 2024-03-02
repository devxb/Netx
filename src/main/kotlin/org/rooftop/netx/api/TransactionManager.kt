package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface TransactionManager {

    fun <T> start(undo: T): Mono<String>

    fun <T> syncStart(undo: T): String

    fun <T> join(transactionId: String, undo: T): Mono<String>

    fun <T> syncJoin(transactionId: String, undo: T): String

    fun exists(transactionId: String): Mono<String>

    fun syncExists(transactionId: String): String

    fun commit(transactionId: String): Mono<String>

    fun syncCommit(transactionId: String): String

    fun rollback(transactionId: String, cause: String): Mono<String>

    fun syncRollback(transactionId: String, cause: String): String

}
