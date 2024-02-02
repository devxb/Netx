package org.rooftop.netx.redis

import org.rooftop.netx.api.TransactionCommitEvent
import org.rooftop.netx.api.TransactionJoinEvent
import org.rooftop.netx.api.TransactionRollbackEvent
import org.rooftop.netx.api.TransactionStartEvent
import org.rooftop.netx.engine.SubscribeTransactionEvent
import org.rooftop.netx.idl.Transaction
import org.rooftop.netx.idl.TransactionState
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.stream.StreamReceiver
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class RedisStreamTransactionDispatcher(
    private val eventPublisher: ApplicationEventPublisher,
    private val streamGroup: String,
    private val nodeName: String,
    connectionFactory: ReactiveRedisConnectionFactory,
) {

    private val options = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(java.time.Duration.ofMillis(100))
        .build()

    private val receiver = StreamReceiver.create(connectionFactory, options)

    @EventListener(SubscribeTransactionEvent::class)
    fun subscribeStream(subscribeTransactionEvent: SubscribeTransactionEvent): Flux<Transaction> {
        return receiver.receiveAutoAck(
            Consumer.from(streamGroup, nodeName),
            StreamOffset.fromStart(subscribeTransactionEvent.transactionId)
        ).publishOn(Schedulers.parallel())
            .map { Transaction.parseFrom(it.value["data"]?.toByteArray()) }
            .dispatch()
    }

    private fun Flux<Transaction>.dispatch(): Flux<Transaction> {
        return this.flatMap {
            when (it.state) {
                TransactionState.TRANSACTION_STATE_JOIN -> publishJoin(it)
                TransactionState.TRANSACTION_STATE_COMMIT -> publishCommit(it)
                TransactionState.TRANSACTION_STATE_ROLLBACK -> publishRollback(it)
                TransactionState.TRANSACTION_STATE_START -> publishStart(it)
                else -> error("Cannot find matched transaction state \"${it.state}\"")
            }
        }
    }

    private fun publishJoin(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext {
                eventPublisher.publishEvent(
                    TransactionJoinEvent(
                        it.id,
                        it.replay,
                        it.serverId
                    )
                )
            }
    }

    private fun publishCommit(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext { eventPublisher.publishEvent(TransactionCommitEvent(it.id, it.serverId)) }
    }

    private fun publishRollback(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext {
                eventPublisher.publishEvent(
                    TransactionRollbackEvent(
                        it.id,
                        it.replay,
                        it.serverId,
                        it.cause,
                    )
                )
            }
    }

    private fun publishStart(it: Transaction): Mono<Transaction> {
        return Mono.just(it)
            .doOnNext {
                eventPublisher.publishEvent(TransactionStartEvent(it.id, it.replay, it.serverId))
            }
    }
}
