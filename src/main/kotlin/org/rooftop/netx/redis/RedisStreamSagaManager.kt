package org.rooftop.netx.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.rooftop.netx.core.Codec
import org.rooftop.netx.api.SagaException
import org.rooftop.netx.engine.AbstractSagaManager
import org.rooftop.netx.engine.SagaIdGenerator
import org.rooftop.netx.engine.core.Saga
import org.rooftop.netx.engine.core.SagaState
import org.springframework.data.redis.connection.stream.Record
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

internal class RedisStreamSagaManager(
    codec: Codec,
    nodeName: String,
    sagaIdGenerator: SagaIdGenerator,
    nodeGroup: String,
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Saga>,
    private val objectMapper: ObjectMapper,
) : AbstractSagaManager(
    nodeName = nodeName,
    nodeGroup = nodeGroup,
    codec = codec,
    sagaIdGenerator = sagaIdGenerator
) {

    override fun getAnySaga(id: String): Mono<SagaState> {
        return reactiveRedisTemplate
            .opsForHash<String, String>()[id, STATE_KEY]
            .switchIfEmpty(
                Mono.error {
                    throw SagaException("Cannot find exists saga by id \"$id\"")
                }
            )
            .map { SagaState.valueOf(it) }
    }

    override fun publishSaga(id: String, saga: Saga): Mono<String> {
        return reactiveRedisTemplate.opsForHash<String, String>()
            .putAll(
                id, mapOf(
                    STATE_KEY to saga.state.name,
                )
            )
            .map { objectMapper.writeValueAsString(saga) }
            .flatMap {
                reactiveRedisTemplate.opsForStream<String, Saga>()
                    .add(
                        Record.of<String, String, String>(mapOf(DATA to it))
                            .withStreamKey(STREAM_KEY)
                    )
            }
            .map { id }
    }

    private companion object {
        private const val DATA = "data"
        private const val STREAM_KEY = "NETX_STREAM"
        private const val STATE_KEY = "TX_STATE"
    }
}
