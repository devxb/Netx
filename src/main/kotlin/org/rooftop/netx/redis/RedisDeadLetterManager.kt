package org.rooftop.netx.redis

import org.rooftop.netx.api.DeadLetterException
import org.rooftop.netx.api.DeadLetterTimeoutException
import org.rooftop.netx.api.SagaEvent
import org.rooftop.netx.core.Codec
import org.rooftop.netx.engine.AbstractSagaDispatcher.Companion.DEAD_LETTER
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.core.SagaState
import org.rooftop.netx.engine.deadletter.AbstractDeadLetterManager
import org.rooftop.netx.engine.logging.error
import org.rooftop.netx.engine.logging.info
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.Limit
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

internal class RedisDeadLetterManager(
    private val codec: Codec,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Saga>,
) : AbstractDeadLetterManager() {

    override fun relaySync(): SagaEvent {
        return relay().block() ?: throw DeadLetterTimeoutException("Cannot get dead letter")
    }

    override fun relay(): Mono<SagaEvent> {
        return reactiveRedisTemplate.opsForStream<String, String>()
            .reverseRange(DEAD_LETTER_KEY, Range.unbounded(), Limit.limit().count(1))
            .single()
            .doOnNext {
                info("Success to read dead letter $it")
            }
            .dispatch()
    }


    override fun relay(deadLetterId: String): Mono<SagaEvent> {
        return reactiveRedisTemplate.opsForStream<String, String>()
            .range(DEAD_LETTER_KEY, Range.just(deadLetterId))
            .single()
            .doOnNext {
                info("Success to read dead letter $it")
            }
            .dispatch()
    }

    override fun relaySync(deadLetterId: String): SagaEvent {
        return relay(deadLetterId).block()
            ?: throw DeadLetterTimeoutException("Cannot get dead letter by deadLetterId: \"$deadLetterId\"")
    }

    private fun Mono<MapRecord<String, String, String>>.dispatch(): Mono<SagaEvent> {
        return this.map {
            it to codec.decode(
                it.value[DATA]
                    ?: throw DeadLetterException("Cannot find any data from record $it"),
                Saga::class,
            )
        }.flatMap {
            val record = it.first
            val saga = it.second
            dispatcher.dispatch(saga, DEAD_LETTER)
                .map { record to saga.toEvent(codec) }
                .info { "Success to dispatch dead letter. saga: \"$saga\"" }
        }.flatMap {
            val record = it.first
            val saga = it.second
            reactiveRedisTemplate.opsForStream<String, String>()
                .delete(DEAD_LETTER_KEY, record.id)
                .map { saga }
                .info {
                    "Success to delete dead letter. record: \"$record\""
                }
        }.doOnError {
            error("Fail to relay dead letter", it)
        }
    }

    override fun add(sagaEvent: SagaEvent): Mono<String> {
        return reactiveRedisTemplate.opsForStream<String, Saga>()
            .add(
                Record.of<String, String, String>(
                    mapOf(DATA to codec.encode(Saga.of(SagaState.ROLLBACK, sagaEvent)))
                ).withStreamKey(DEAD_LETTER_KEY)
            )
            .info("Success to add dead letter to \"$DEAD_LETTER_KEY\". event: \"$sagaEvent\"")
            .doOnError {
                error("Fail to add dead letter to \"$DEAD_LETTER_KEY\". event: \"$sagaEvent\"")
            }.map { it.value }
    }

    companion object {
        private const val DATA = "data"
        private const val DEAD_LETTER_KEY = "NETX_DEAD_LETTER"
    }
}
