package org.rooftop.netx.redis

import org.rooftop.netx.api.Codec
import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.idl.Transaction
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

class NoAckRedisStreamTransactionDispatcher(
    codec: Codec,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
    private val nodeGroup: String,
) : AbstractTransactionDispatcher(codec) {

    override fun <T : Annotation> findHandlers(type: KClass<T>): List<Any> {
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

