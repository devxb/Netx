package org.rooftop.netx.engine

import reactor.core.publisher.Mono

interface UndoManager {

    fun save(transactionId: String, undo: String): Mono<String>

    fun find(transactionId: String): Mono<String>

    fun delete(transactionId: String): Mono<Boolean>

}
