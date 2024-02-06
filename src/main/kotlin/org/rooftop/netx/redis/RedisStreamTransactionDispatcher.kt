package org.rooftop.netx.redis

import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.SubscribeTransactionEvent
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
    private val streamGroup: String,
    private val nodeName: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, ByteArray>,
) : AbstractTransactionDispatcher(SpringEventPublisher(eventPublisher)) {

    private val options = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(1.hours.toJavaDuration())
        .build()

    private val receiver = StreamReceiver.create(connectionFactory, options)

    override fun receive(event: SubscribeTransactionEvent): Flux<Transaction> {
        return createGroupIfNotExists(event)
            .flatMap {
                receiver.receiveAutoAck(
                    Consumer.from(streamGroup, nodeName),
                    StreamOffset.create(event.transactionId, ReadOffset.from(">"))
                ).publishOn(Schedulers.parallel())
                    .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
                    .flatMap {
                        when (it.state) {
                            TransactionState.TRANSACTION_STATE_ROLLBACK ->
                                findOwnTransaction(it)

                            else -> Mono.just(it)
                        }
                    }
            }
    }

    private fun createGroupIfNotExists(event: SubscribeTransactionEvent): Flux<String> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .createGroup(event.transactionId, ReadOffset.from("0"), streamGroup)
            .flatMapMany { Flux.just(it) }
    }

    override fun findOwnTransaction(transaction: Transaction): Mono<Transaction> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .read(StreamOffset.create(transaction.id, ReadOffset.from("0")))
            .map { Transaction.parseFrom(it.value["data"]!!) }
            .filter { it.group == streamGroup }
            .filter { hasUndo(it) }
            .next()
    }

    private fun hasUndo(transaction: Transaction): Boolean =
        transaction.state == TransactionState.TRANSACTION_STATE_JOIN
                || transaction.state == TransactionState.TRANSACTION_STATE_START
}
