package org.rooftop.netx.redis

import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class RedisStreamTransactionDispatcher(
    eventPublisher: ApplicationEventPublisher,
    connectionFactory: ReactiveRedisConnectionFactory,
    private val nodeGroup: String,
    private val nodeName: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
) : AbstractTransactionDispatcher(eventPublisher) {

    private val options = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(1.hours.toJavaDuration())
        .build()

    private val receiver = StreamReceiver.create(connectionFactory, options)

    override fun receive(transactionId: String): Flux<Pair<Transaction, String>> {
        return createGroupIfNotExists(transactionId)
            .flatMap {
                receiver.receive(
                    Consumer.from(nodeGroup, nodeName),
                    StreamOffset.create(transactionId, ReadOffset.from(">"))
                ).publishOn(Schedulers.parallel())
                    .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) to it.id.value }
                    .flatMap { (transaction, messageId) ->
                        when (transaction.state) {
                            TransactionState.TRANSACTION_STATE_ROLLBACK ->
                                findOwnTransaction(transaction).map { it to messageId }

                            else -> Mono.just(transaction to messageId)
                        }
                    }
            }
    }

    private fun createGroupIfNotExists(transactionId: String): Flux<String> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .createGroup(transactionId, ReadOffset.from("0"), nodeGroup)
            .flatMapMany { Flux.just(it) }
    }

    override fun findOwnTransaction(transaction: Transaction): Mono<Transaction> {
        return reactiveRedisTemplate.opsForStream<String, String>()
            .read(StreamOffset.create(transaction.id, ReadOffset.from("0")))
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
            .filter { it.group == nodeGroup }
            .filter { hasUndo(it) }
            .next()
    }

    private fun hasUndo(transaction: Transaction): Boolean =
        transaction.state == TransactionState.TRANSACTION_STATE_JOIN
                || transaction.state == TransactionState.TRANSACTION_STATE_START

    override fun Flux<Pair<Transaction, String>>.ack(): Flux<Pair<Transaction, String>> {
        return this.flatMap { (transaction, messageId) ->
            reactiveRedisTemplate.opsForStream<String, ByteArray>()
                .acknowledge(transaction.id, nodeGroup, messageId)
                .flatMapMany { Flux.just(transaction to messageId) }
        }
    }
}
