package org.rooftop.netx.api

import reactor.core.publisher.Mono

interface TransactionManager {

    fun start(undo: String): Mono<String>

    fun syncStart(undo: String): String

    fun join(transactionId: String, undo: String): Mono<String>

    fun syncJoin(transactionId: String, undo: String): String

    fun exists(transactionId: String): Mono<String>

    fun syncExists(transactionId: String): String

    fun commit(transactionId: String): Mono<String>

    fun syncCommit(transactionId: String): String

    fun rollback(transactionId: String, cause: String): Mono<String>

    fun syncRollback(transactionId: String, cause: String): String

}
