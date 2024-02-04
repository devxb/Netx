package org.rooftop.netx.redis

import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.SubscribeTransactionEvent
import org.rooftop.netx.idl.Transaction
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import reactor.core.publisher.Flux
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
            }
    }

    private fun createGroupIfNotExists(event: SubscribeTransactionEvent): Flux<String> {
        return reactiveRedisTemplate.opsForStream<String, ByteArray>()
            .createGroup(event.transactionId, ReadOffset.from("0"), streamGroup)
            .flatMapMany { Flux.just(it) }
    }
}
