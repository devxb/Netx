package org.rooftop.netx.redis

import org.rooftop.netx.api.Codec
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.core.Transaction
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class NoAckRedisStreamTransactionDispatcher(
    codec: Codec,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Transaction>,
    private val nodeGroup: String,
) : AbstractTransactionDispatcher(codec) {

    override fun findHandlers(): List<Any> {
        return listOf()
    }

    override fun findOwnUndo(transaction: Transaction): Mono<String> {
        return reactiveRedisTemplate.opsForHash<String, String>()[transaction.id, nodeGroup]
            .switchIfEmpty(
                Mono.error {
                    error("Cannot find undo state in transaction hashes key \"${transaction.id}\"")
                }
            )
    }

    override fun ack(transaction: Transaction, messageId: String): Mono<Pair<Transaction, String>> =
        Mono.error {
            error("Cannot ack transaction")
        }
}

