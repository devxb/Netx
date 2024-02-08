package org.rooftop.netx.redis

import org.rooftop.netx.engine.AbstractTransactionManager
import org.rooftop.netx.idl.Transaction
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisStreamTransactionManager(
    nodeId: Int,
    nodeName: String,
    nodeGroup: String,
    transactionDispatcher: RedisStreamTransactionDispatcher,
    transactionRetrySupporter: RedisTransactionRetrySupporter,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
) : AbstractTransactionManager(
    nodeId = nodeId,
    nodeName = nodeName,
    nodeGroup = nodeGroup,
    transactionDispatcher = transactionDispatcher,
    transactionRetrySupporter = transactionRetrySupporter,
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
