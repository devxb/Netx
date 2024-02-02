package org.rooftop.netx.redis

import org.rooftop.netx.engine.AbstractTransactionDispatcher
import org.rooftop.netx.engine.SubscribeTransactionEvent
import org.rooftop.netx.idl.Transaction
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.stream.StreamReceiver
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class RedisStreamTransactionDispatcher(
    eventPublisher: ApplicationEventPublisher,
    private val streamGroup: String,
    private val nodeName: String,
    connectionFactory: ReactiveRedisConnectionFactory,
) : AbstractTransactionDispatcher(SpringEventPublisher(eventPublisher)) {

    private val options = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(100.milliseconds.toJavaDuration())
        .build()

    private val receiver = StreamReceiver.create(connectionFactory, options)

    override fun receive(event: SubscribeTransactionEvent): Flux<Transaction> {
        return receiver.receiveAutoAck(
            Consumer.from(streamGroup, nodeName),
            StreamOffset.fromStart(event.transactionId)
        ).publishOn(Schedulers.parallel())
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
    }
}
