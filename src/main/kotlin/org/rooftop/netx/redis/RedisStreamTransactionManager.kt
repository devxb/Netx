package org.rooftop.netx.redis

import org.rooftop.netx.engine.AbstractTransactionManager
import org.rooftop.netx.engine.UndoManager
import org.rooftop.netx.idl.Transaction
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisStreamTransactionManager(
    nodeId: Int,
    nodeName: String,
    applicationEventPublisher: ApplicationEventPublisher,
    undoManager: UndoManager,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
) : AbstractTransactionManager(
    nodeId,
    nodeName,
    SpringEventPublisher(applicationEventPublisher),
    undoManager = undoManager
) {

    override fun findAnyTransaction(transactionId: String): Mono<Transaction> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .range(transactionId, Range.open("-", "+"))
            .map { Transaction.parseFrom(it.value[DATA].toString().toByteArray()) }
            .next()
    }

    override fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .add(
                Record.of<String?, String?, ByteArray?>(mapOf(DATA to transaction.toByteArray()))
                    .withStreamKey(transactionId)
            )
            .mapTransactionId()
    }

    private companion object {
        private const val DATA = "data"
    }
}
