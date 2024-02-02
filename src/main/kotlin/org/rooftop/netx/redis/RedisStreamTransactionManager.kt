package org.rooftop.netx.redis

import org.rooftop.netx.engine.AbstractTransactionManager
import org.rooftop.netx.idl.Transaction
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

class RedisStreamTransactionManager(
    nodeName: String,
    applicationEventPublisher: ApplicationEventPublisher,
    private val transactionServer: ReactiveRedisTemplate<String, ByteArray>,
) : AbstractTransactionManager(nodeName, SpringEventPublisher(applicationEventPublisher)) {

    override fun exists(transactionId: String): Mono<String> {
        return transactionServer.opsForStream<String, ByteArray>()
            .range(transactionId, Range.open("-", "+"))
            .map { Transaction.parseFrom(it.value[DATA].toString().toByteArray()) }
            .next()
            .switchIfEmpty(
                Mono.error {
                    IllegalStateException("Cannot find exists transaction id \"$transactionId\"")
                }
            )
            .transformTransactionId()
    }

    private fun Mono<*>.transformTransactionId(): Mono<String> {
        return this.flatMap {
            Mono.deferContextual { Mono.just(it["transactionId"]) }
        }
    }

    override fun publishTransaction(transactionId: String, transaction: Transaction): Mono<String> {
        return transactionServer.opsForStream<String, ByteArray>()
            .add(
                Record.of<String?, String?, ByteArray?>(mapOf(DATA to transaction.toByteArray()))
                    .withStreamKey(transactionId)
            )
            .transformTransactionId()
    }

    private companion object {
        private const val DATA = "data"
    }
}
